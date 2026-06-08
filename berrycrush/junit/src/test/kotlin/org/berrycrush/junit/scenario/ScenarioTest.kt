package org.berrycrush.junit.scenario

import org.berrycrush.junit.BerryCrushConfiguration
import org.berrycrush.junit.BerryCrushScenarios

@BerryCrushConfiguration
@BerryCrushScenarios(locations = ["scenarios/*.scenario"])
class ScenarioTest
