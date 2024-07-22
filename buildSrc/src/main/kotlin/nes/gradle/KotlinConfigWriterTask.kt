package nes.gradle

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class KotlinConfigWriterTask : DefaultTask() {

    @get:Input
    abstract var keyValuePairs: Map<String, String>

    @get:Input
    abstract var className: String

    @get:Input
    abstract var packageName: String

    @get:OutputDirectory
    abstract var kotlinConfigFile: Provider<RegularFile>

    @TaskAction
    fun generate() {
        val file = FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder(className)
                    .apply {
                        keyValuePairs.forEach { (key, value) ->
                            addProperty(
                                PropertySpec.builder(key, String::class, KModifier.CONST)
                                    .initializer("%S", value).build()
                            )
                        }
                    }
                    .build()
            ).build()

        val outputFile = kotlinConfigFile.get().asFile
        require(outputFile.mkdirs() || outputFile.exists()) { "could not create config output directory" }
        file.writeTo(outputFile)
    }
}

