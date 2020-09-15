package never.ending.splendor.app

import never.ending.splendor.app.AppInitializer
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

val debugModule = DI.Module("Debug Module") {
    bind<AppInitializer>() with singleton { DebugAppInitializer(instance()) }
}

val buildSpecificModules = listOf(debugModule)
