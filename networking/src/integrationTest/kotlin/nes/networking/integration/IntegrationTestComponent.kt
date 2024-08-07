package nes.networking.integration

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [IntegrationTestModule::class])
interface IntegrationTestComponent {
    fun inject(test: PhishNetServiceIntegrationTest)
    fun inject(test: PhishInServiceIntegrationTest)
}
