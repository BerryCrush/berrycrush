package org.berrycrush.junit.scenario

import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushScenarios
import org.berrycrush.junit.BerryCrushSpec

@BerryCrushConfiguration
@BerryCrushScenarios(locations = ["scenarios/*.scenario"])
@BerryCrushSpec("test-api.yaml")
class ScenarioTest
