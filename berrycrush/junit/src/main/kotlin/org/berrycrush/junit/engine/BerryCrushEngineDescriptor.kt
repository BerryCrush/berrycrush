package org.berrycrush.junit.engine

import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

/**
 * Root test descriptor for the BerryCrush TestEngine.
 *
 * This descriptor represents the engine itself in the JUnit test tree
 * and contains [ClassTestDescriptor] nodes as children.
 */
class BerryCrushEngineDescriptor(
    uniqueId: UniqueId,
) : EngineDescriptor(uniqueId, "BerryCrush") {
    companion object {
        const val ENGINE_ID = "berrycrush"
    }
}
