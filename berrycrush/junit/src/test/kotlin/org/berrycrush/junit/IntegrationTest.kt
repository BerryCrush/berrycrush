package org.berrycrush.junit

import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("berrycrush")
@BerryCrushScenarios("integration/*.scenario", fragments = ["integration/*.fragment"])
@BerryCrushConfiguration(
    stepPackages = ["org.berrycrush.junit.glue"],
    plugins = ["report:console:high-contrast"],
)
@BerryCrushSpec("test-api.yaml")
class IntegrationTest
