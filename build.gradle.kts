plugins {
    `java-library`
    `maven`
    `eclipse`
}

repositories {
    jcenter()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("com.fifesoft:autocomplete:3.1.0")

}

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
}
