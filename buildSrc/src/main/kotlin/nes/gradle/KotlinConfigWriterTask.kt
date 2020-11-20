package nes.gradle

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
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
    abstract var kotlinConfigFile: File

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

        file.writeTo(kotlinConfigFile)
    }
}

