package org.berrycrush.plugin.adapter

import org.berrycrush.context.resolveParam
import org.berrycrush.context.resolveParams
import org.berrycrush.plugin.ExecutionContext
import org.berrycrush.webhook.MockWebhookServer
import org.berrycrush.context.ExecutionContext as CoreExecutionContext

class ExecutionContextAdapter(
    val context: CoreExecutionContext,
) : ExecutionContext {
    override val shareVariablesAcrossScenarios: Boolean = context.shareVariablesAcrossScenarios

    override fun contains(name: String): Boolean = context.contains(name)

    override fun allVariables(): Map<String, Any> = context.allVariables()

    override fun interpolate(template: String): String = context.interpolate(template)

    override fun <T> get(name: String): T? = context[name]

    override fun set(
        name: String,
        value: Any,
    ) {
        context[name] = value
    }

    override fun <T : Any> resolveParam(value: T): T = context.resolveParam(value)

    override fun <T : Any> resolveParams(value: Map<String, T>): Map<String, T> = context.resolveParams(value)

    fun registerWebhookServer(
        name: String,
        server: MockWebhookServer,
    ) = context.registerWebhookServer(name, server)
}
