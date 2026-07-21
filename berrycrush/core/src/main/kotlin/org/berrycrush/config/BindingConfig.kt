package org.berrycrush.config

import org.berrycrush.openapi.LoadedSpec
import org.berrycrush.openapi.LoadedSpecProvider
import org.berrycrush.openapi.SpecRegistry

data class BindingConfig(
    val name: String,
    val baseUrl: String? = null,
    val location: String? = null,
    val operationAliases: Map<String, String> = emptyMap(),
) : LoadedSpecProvider {
    override val spec: LoadedSpec? by lazy {
        location?.let {
            SpecRegistry.load(name, it) {
                this.baseUrl = baseUrl
            }
        }
    }

    companion object {
        const val DEFAULT_BINDING_NAME = "default"
    }
}
