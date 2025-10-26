tasks.jar {
    enabled = true
}

tasks.bootJar {
    enabled = false
}

tasks.test {
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation(projects.modules.domain)
    implementation(projects.modules.application)
    implementation(libs.spring.boot.starter.jpa)
    implementation(libs.flyway.core)
    runtimeOnly(libs.database.h2)
    runtimeOnly(libs.database.mariadb)
    runtimeOnly(libs.flyway.mysql)

    testImplementation(projects.modules.external.pgClient)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(module = "mockito-core")
    }
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.junit)
}
