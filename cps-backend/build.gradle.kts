plugins {
    `java-library`
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.webmvc)
    implementation(libs.spring.validation)
    implementation(libs.spring.security)
    implementation(libs.spring.security.resource.server)
    implementation(libs.spring.security.jose)
    implementation(project(":cps-core"))
    implementation(libs.jetbrains.annotations)

    // Spring Data JPA integration and Flyway migrations.
    implementation(libs.spring.data.jpa)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgres)
    implementation(libs.spring.flyway)

    // Database driver for PostgreSQL.
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.webmvc.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.h2database)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
