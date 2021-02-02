/*
 * Copyright (C) 2009-2011 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.parboiled.transform;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.loading.ClassInjector;
import org.objectweb.asm.ClassWriter;
import org.parboiled.common.ImmutableList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.parboiled.common.Preconditions.checkArgNotNull;
import static org.parboiled.transform.AsmUtils.*;

public class ParserTransformer {

    // Only to keep the oldest API
    @Deprecated
    private static final Map<String, Class<?>> groupClasses = new ConcurrentHashMap<>();

    private ParserTransformer() {
    }

    @SuppressWarnings({"unchecked"})
    @Deprecated
    public static synchronized <T> Class<? extends T> transformParser(Class<T> parserClass) throws Exception {
        checkArgNotNull(parserClass, "parserClass");
        // first check whether we did not already create and load the extension of the given parser class
        final String      extendedParserClassName = getExtendedParserClassName(parserClass.getName());
        final ClassLoader classLoader             = parserClass.getClassLoader();
        Class<?>          extendedClass           = findLoadedClass(extendedParserClassName, classLoader);
        return (Class<? extends T>)
                (extendedClass != null ? extendedClass : extendParserClass(parserClass).getExtendedClass());
    }

    @Deprecated
    static ParserClassNode extendParserClass(Class<?> parserClass) throws Exception {

        ClassInjector classInjector = (ClassInjector.UsingLookup.isAvailable()
                ? Optional.of(parserClass).<ClassInjector>map(
                ClassInjector.UsingLookup.of(MethodHandles.lookup())::in)
                : Optional.<ClassInjector>empty()).orElseGet(() -> {
            ClassLoader      classLoader      = parserClass.getClassLoader();
            ProtectionDomain protectionDomain = parserClass.getProtectionDomain();
            return ClassInjector.UsingUnsafe.isAvailable()
                    ? new ClassInjector.UsingUnsafe(classLoader, protectionDomain)
                    : new ClassInjector.UsingReflection(classLoader, protectionDomain);
        });
        final BiFunction<String, Supplier<byte[]>, Class<?>> defaultGroupClassGeneratorWithBytebuddy = (className, groupClassGenerator) -> {
            return groupClasses.computeIfAbsent(className, key -> {
                byte[] groupClassCode = groupClassGenerator.get();
                Map<String, Class<?>> groupClass = classInjector.injectRaw(
                        Collections.singletonMap(className, groupClassCode));
                return groupClass.get(className);
            });
        };
        final IntSupplier javaVersionDetection = () -> {
            try {
                return ClassFileVersion.of(parserClass).getMinorMajorVersion();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return extendParserClass(parserClass, defaultGroupClassGeneratorWithBytebuddy,
                                 javaVersionDetection);
    }

    @Deprecated
    private static ParserClassNode extendParserClass(Class<?> parserClass,
                                                     BiFunction<String,
                                                             Supplier<byte[]>, Class<?>> classInjector,
                                                     IntSupplier classFileVersion) throws
                                                                                   Exception {
        ParserClassNode classNode = new ParserClassNode(parserClass);
        new ClassNodeInitializer(classFileVersion).process(classNode);
        runMethodTransformers(classNode, classInjector::apply, classFileVersion);
        new ConstructorGenerator().process(classNode);
        defineExtendedParserClass(classNode, classInjector);
        return classNode;
    }

    @SuppressWarnings({"unchecked"})
    private static void runMethodTransformers(ParserClassNode classNode, BiConsumer<String, Supplier<byte[]>> classInjector,
                                              IntSupplier classFileVersion) throws
                                                                            Exception {
        List<RuleMethodProcessor> methodProcessors = createRuleMethodProcessors(classInjector, classFileVersion);

        // iterate through all rule methods
        // since the ruleMethods map on the classnode is a treemap we get the methods sorted by name which puts
        // all super methods first (since they are prefixed with one or more '$')
        for (RuleMethod ruleMethod : classNode.getRuleMethods().values()) {
            if (!ruleMethod.hasDontExtend()) {
                for (RuleMethodProcessor methodProcessor : methodProcessors) {
                    if (methodProcessor.appliesTo(classNode, ruleMethod)) {
                        methodProcessor.process(classNode, ruleMethod);
                    }
                }
            }
        }

        for (RuleMethod ruleMethod : classNode.getRuleMethods().values()) {
            if (!ruleMethod.isGenerationSkipped()) {
                classNode.methods.add(ruleMethod);
            }
        }
    }

    private static void defineExtendedParserClass(final ParserClassNode classNode,
                                                  BiFunction<String, Supplier<byte[]>, Class<?>> classInjector) {
        ClassWriter classWriter = new ClassWriter(ASMSettings.FRAMES) {
            @Override
            protected ClassLoader getClassLoader() {
                return classNode.getParentClass().getClassLoader();
            }
        };
        classNode.accept(classWriter);
        classNode.setClassCode(classWriter.toByteArray());
        classNode.setExtendedClass(classInjector == null ? loadClass(
                classNode.name.replace('/', '.'),
                classNode.getClassCode(),
                classNode.getParentClass().getClassLoader()) :
                classInjector.apply(
                        classNode.name.replace('/', '.'),
                        () -> classNode.getClassCode()
                ));
    }

    private static List<RuleMethodProcessor> createRuleMethodProcessors(BiConsumer<String, Supplier<byte[]>> classInjector,
                                                                        IntSupplier classFileVersion) {
        return ImmutableList.of(
                new UnusedLabelsRemover(),
                new ReturnInstructionUnifier(),
                new InstructionGraphCreator(),
                new ImplicitActionsConverter(),
                new InstructionGroupCreator(),
                new InstructionGroupPreparer(),
                new ActionClassGenerator(false, classInjector, classFileVersion),
                new VarInitClassGenerator(false, classInjector, classFileVersion),

                new RuleMethodRewriter(),
                new SuperCallRewriter(),
                new BodyWithSuperCallReplacer(),
                new VarFramingGenerator(),
                new LabellingGenerator(),
                new FlagMarkingGenerator(),
                new CachingGenerator()
        );
    }

    public static synchronized <T> Class<? extends T> transformParser(Class<T> parserClass, BiFunction<String, Supplier<byte[]>,
            Class<?>> classInjector, IntSupplier classFileVersion) throws
                                                                   Exception {
        checkArgNotNull(parserClass, "parserClass");
        checkArgNotNull(classInjector, "classInjector");
        checkArgNotNull(classFileVersion, "classFileVersion");
        return extendParserClass(parserClass, classInjector, classFileVersion).getExtendedClass().asSubclass(parserClass);
    }

}
