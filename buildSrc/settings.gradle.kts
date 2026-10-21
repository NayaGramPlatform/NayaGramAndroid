pluginManagement {
    repositories {
        // Kotlin DSL plugin transitive dependencies are published to Maven Central.
        // Keep the plugin portal for plugin marker artifacts, but search Maven repositories too.
        google()
        mavenCentral()
        gradlePluginPortal()
        // Mirror is a fallback; avoid failing dependency resolution on transient 502s.
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
