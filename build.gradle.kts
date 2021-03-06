apply(plugin = "com.github.ben-manes.versions")

buildscript {
    val kotlinVersion by extra("1.4.10")
    repositories {
        google()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
        gradlePluginPortal()
    }

    dependencies {
        // Reading gradle versions
        classpath("com.github.ben-manes:gradle-versions-plugin:0.28.0")
        // Kotlin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.4.10")
        // Android tools
        classpath("com.android.tools.build:gradle:4.2.0-alpha15")
        // Navigation safe args
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.1")
        // Sqldelight
        classpath("com.squareup.sqldelight:gradle-plugin:1.4.3")

    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}