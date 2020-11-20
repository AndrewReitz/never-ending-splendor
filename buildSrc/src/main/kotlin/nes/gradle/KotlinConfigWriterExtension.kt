package nes.gradle

open class KotlinConfigWriterExtension {
    internal val keys = mutableMapOf<String, String>()

    var className = "Config"

    var packageName = ""

    fun put(key: String, value: String) {
        keys[key] = value
    }

    fun put(pair: Pair<String, String>) {
        keys[pair.first] = pair.second
    }
}
