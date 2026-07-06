package org.berrycrush.junit

import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("berrycrush")
@BerryCrushScenarios("integration/*.scenario")
@BerryCrushConfiguration(
    stepPackages = ["org.berrycrush.junit.glue"],
)
@BerryCrushSpec("test-api.yaml")
class ParametersTest
