package io.github.ktakashi.lemoncheck.junit.engine

import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

/**
 * Root test descriptor for the LemonCheck TestEngine.
 *
 * This descriptor represents the engine itself in the JUnit test tree
 * and contains [ClassTestDescriptor] nodes as children.
 */
class LemonCheckEngineDescriptor(
    uniqueId: UniqueId,
) : EngineDescriptor(uniqueId, "LemonCheck") {
    companion object {
        const val ENGINE_ID = "lemoncheck"
    }
}
