[ ![Download](https://api.bintray.com/packages/byoskill/byoskill-parboiled/parboiled/images/download.svg) ](https://bintray.com/byoskill/byoskill-parboiled/parboiled/_latestVersion)
[![Build Status](https://travis-ci.org/Byoskill/parboiled.svg?branch=master)](https://travis-ci.org/Byoskill/parboiled)

# Important notes

This project is a fork from the excellent project Parboiled where the original sources can be found there: https://github.com/sirthias/parboiled

## Release notes

* **1.4.1**: Full JDK 21 support with ASM 9.7, updated JVM module access configuration
* **1.3.2**: Maven packaging, bump to ASM 9, minimum compatibility raised to Java 1.8 and ASM 8

## JDK 21 Compatibility

**Parboiled 1.4.1+ fully supports JDK 21!** However, due to Java's bytecode transformation requirements, you may need to configure your project properly.

### Maven Configuration

If you're using JDK 21 (or any newer JDK version), you might need to explicitly declare a newer ASM version in your project's `pom.xml`:

```xml
<dependencies>
    <!-- Parboiled dependency -->
    <dependency>
        <groupId>com.byoskill.parboiled</groupId>
        <artifactId>parboiled-java</artifactId>
        <version>1.4.1</version>
    </dependency>
    
    <!-- Explicitly declare ASM for JDK 21+ support -->
    <dependency>
        <groupId>org.ow2.asm</groupId>
        <artifactId>asm</artifactId>
        <version>9.7.1</version>
    </dependency>
</dependencies>
```

### Required JVM Arguments

When running tests or applications using Parboiled with JDK 9+, you need to open certain internal Java modules. Add these JVM arguments:

```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.invoke=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/jdk.internal.loader=ALL-UNNAMED
--add-opens java.base/java.security=ALL-UNNAMED
```

#### Maven Surefire Plugin Configuration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <argLine>
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.lang.invoke=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
            --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
            --add-opens java.base/java.security=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

### Gradle Configuration

For Gradle users:

```gradle
dependencies {
    implementation 'com.byoskill.parboiled:parboiled-java:1.4.1'
    // Explicitly declare ASM for JDK 21+ support
    implementation 'org.ow2.asm:asm:9.7.1'
}

test {
    jvmArgs(
        '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
        '--add-opens', 'java.base/java.lang.invoke=ALL-UNNAMED',
        '--add-opens', 'java.base/java.lang.reflect=ALL-UNNAMED',
        '--add-opens', 'java.base/jdk.internal.loader=ALL-UNNAMED',
        '--add-opens', 'java.base/java.security=ALL-UNNAMED'
    )
}
```

### Why is ASM needed?

Parboiled uses ASM (a Java bytecode manipulation framework) to transform parser classes at runtime. The ASM version must support the bytecode format of your target JDK:

- **JDK 8-16**: ASM 8.x is sufficient
- **JDK 17-21**: ASM 9.x is required (9.7+ recommended)
- **JDK 22+**: Check for latest ASM version

By explicitly declaring the ASM dependency in your project, you ensure Maven/Gradle uses a version compatible with your JDK, overriding any older transitive dependencies.

## Resources

* <https://repo1.maven.org/maven2/org/parboiled/> for download access to the artifacts
* <https://github.com/sirthias/parboiled/wiki> for all documentation
* <https://sirthias.github.com/parboiled/api/core> for the parboiled-core API javadoc
* <https://sirthias.github.com/parboiled/api/java> for the parboiled-java API javadoc
* <https://sirthias.github.com/parboiled/api/scala> for the parboiled-scala API scaladoc
* <https://github.com/sirthias/parboiled/wiki/Patch-Policy> for the Patch Policy
