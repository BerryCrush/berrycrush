package org.berrycrush.config

data class BindingConfig(
    val baseUrl: String?,
) {
    companion object {
        const val DEFAULT_BINDING_NAME = "default"
    }
}
