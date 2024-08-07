import nes.gradle.KotlinConfigWriterExtension
import nes.gradle.KotlinConfigWriterTask

plugins {
    kotlin("jvm")
}

val extension = extensions.create<KotlinConfigWriterExtension>("kotlinConfigWriter")

val outputDirectory: Provider<RegularFile> = layout.buildDirectory.file("generated-configs")

kotlin {
    sourceSets["main"].kotlin.srcDir(outputDirectory)
}

val generateConfigTask = tasks.create<KotlinConfigWriterTask>("generateKotlinConfigFile") {
    className = extension.className
    packageName = extension.packageName
    keyValuePairs = extension.keys
    kotlinConfigFile = outputDirectory
}

afterEvaluate {
    tasks.named("compileKotlin").configure {
        dependsOn(generateConfigTask)
    }
    tasks.named("kspKotlin").configure {
        dependsOn(generateConfigTask)
    }
}

plugins.findPlugin(IdeaPlugin::class)?.apply {
    model.module.sourceDirs.add(outputDirectory.get().asFile)
}
