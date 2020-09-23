plugins {
    base
}

configurations.create("ktlint")

dependencies {
    "ktlint"("com.pinterest:ktlint:0.39.0")
}

val ktlint = tasks.register<JavaExec>("ktlint") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = configurations.getByName("ktlint")
    main = "com.pinterest.ktlint.Main"
    args("src/**/*.kt")
}

tasks.named("check").configure {
    dependsOn(ktlint)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = configurations.getByName("ktlint")
    main = "com.pinterest.ktlint.Main"
    args("-F", "src/**/*.kt")
}
