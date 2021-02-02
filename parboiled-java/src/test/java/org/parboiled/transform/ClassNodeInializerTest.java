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

import org.objectweb.asm.tree.MethodNode;
import org.parboiled.common.Predicate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

import static org.testng.Assert.assertEquals;

public class ClassNodeInializerTest extends TransformationTest {

    @BeforeClass
    public void setup() throws IOException {
        setup(TestParser.class);
    }

    @Test()
    public void testClassNodeSetup() throws Exception {
        assertEquals(classNode.name, "org/parboiled/transform/TestParser$$parboiled");
        assertEquals(classNode.superName, "org/parboiled/transform/TestParser");

        assertEquals(join(classNode.getConstructors(), null), "<init>");

        assertEquals(join(classNode.getRuleMethods().values(), new Predicate<RuleMethod>() {
            public boolean apply(RuleMethod method) {
                return method.containsExplicitActions();
            }
        }), "RuleWith2Returns,RuleWithComplexActionSetup,RuleWithDirectExplicitAction,RuleWithIndirectExplicitAction");

        assertEquals(join(classNode.getRuleMethods().values(), new Predicate<RuleMethod>() {
            public boolean apply(RuleMethod method) {
                return method.containsImplicitActions();
            }
        }), "NumberRule,RuleWithCachedAnd2Params,RuleWithComplexActionSetup,RuleWithDirectImplicitAction,RuleWithFakeImplicitAction,RuleWithIndirectImplicitAction,RuleWithIndirectImplicitParamAction,RuleWithSwitchAndAction");

        assertEquals(join(classNode.getRuleMethods().values(), new Predicate<RuleMethod>() {
            public boolean apply(RuleMethod method) {
                return method.hasCachedAnnotation();
            }
        }), "AnyOf,BugIn0990,Ch,CharRange,FirstOf,IgnoreCase,IgnoreCase,NTimes,NTimes,NumberRule,OneOrMore,Optional,RuleWithCachedAnd2Params,RuleWithDirectExplicitAction,RuleWithDirectImplicitAction,RuleWithIndirectExplicitAction,RuleWithIndirectImplicitAction,RuleWithLeaf,RuleWithNamedLabel,RuleWithoutAction,Sequence,String,Test,TestNot,ZeroOrMore");

        assertEquals(join(classNode.getRuleMethods().values(), new Predicate<RuleMethod>() {
            public boolean apply(RuleMethod method) {
                return method.hasDontLabelAnnotation();
            }
        }), "AnyOf,AnyOf,AnyOf,Ch,CharRange,FirstOf,FirstOf,IgnoreCase,IgnoreCase,IgnoreCase,NTimes,NTimes,NoneOf,NoneOf,OneOrMore,OneOrMore,Optional,Optional,RuleWith2Returns,RuleWithSwitchAndAction,Sequence,Sequence,String,String,Test,Test,TestNot,TestNot,ZeroOrMore,ZeroOrMore");

        assertEquals(join(classNode.getRuleMethods().values(), new Predicate<RuleMethod>() {
            public boolean apply(RuleMethod method) {
                return method.hasSuppressNodeAnnotation();
            }
        }), "RuleWithLeaf,Test,TestNot");
    }

    private <T extends MethodNode> String join(Collection<T> methods, Predicate<T> predicate) {
        StringBuilder sb = new StringBuilder();
        for (T method : methods) {
            if (predicate == null || predicate.apply(method)) {
                if (sb.length() > 0) sb.append(',');
                sb.append(method.name);
            }
        }
        return sb.toString();
    }

}
