package org.berrycrush.junit.engine

import org.berrycrush.junit.BerryCrushScenarios
import org.berrycrush.junit.discovery.DiscoveredScenario
import org.berrycrush.junit.discovery.ScenarioDiscovery
import org.berrycrush.model.Scenario
import org.berrycrush.scenario.FeatureGroup
import org.berrycrush.scenario.ScenarioFileContent
import org.berrycrush.scenario.ScenarioLoader
import org.junit.jupiter.api.Disabled
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.io.File
import java.io.InputStreamReader
import java.net.URL

/**
 * Responsible for discovering scenario tests from annotated test classes.
 *
 * This class handles:
 * - Finding classes annotated with @BerryCrushScenarios
 * - Loading .scenario files from specified locations
 * - Building the test descriptor hierarchy (Class -> File -> Feature -> Scenario)
 * - Applying filters from system properties
 */
object ScenarioTestDiscoverer {
    /**
     * Discovers scenarios for a test class and adds them to the engine descriptor.
     *
     * @param engineDescriptor The parent engine descriptor to add class descriptors to
     * @param testClass The test class annotated with @BerryCrushScenarios
     * @param filters Optional filters to apply during discovery
     */
    fun discoverScenariosForClass(
        engineDescriptor: EngineDescriptor,
        testClass: Class<*>,
        filters: ScenarioFilters = ScenarioFilters.EMPTY,
    ) {
        val annotation = testClass.getAnnotation(BerryCrushScenarios::class.java) ?: return

        if (testClass.isAnnotationPresent(Disabled::class.java)) return
        if (engineDescriptor.alreadyDiscovered(testClass)) return
        if (annotation.locations.isEmpty()) return

        val discoveredFiles =
            discoverScenarioFiles(testClass.classLoader, annotation.locations)
                .filter { filters.matchesFile(it.path, it.name) }
        val classDescriptor = createClassDescriptor(engineDescriptor.uniqueId, testClass, discoveredFiles, filters)

        if (classDescriptor.children.isNotEmpty() || discoveredFiles.isEmpty()) {
            engineDescriptor.addChild(classDescriptor)
        }
    }

    private fun discoverScenarioFiles(
        classLoader: ClassLoader,
        locations: Array<out String>,
    ): List<DiscoveredScenario> =
        ScenarioDiscovery
            .discoverScenarios(classLoader, locations)
            .sortedBy { it.name }

    private fun createClassDescriptor(
        parentId: UniqueId,
        testClass: Class<*>,
        files: List<DiscoveredScenario>,
        filters: ScenarioFilters,
    ): ClassTestDescriptor {
        val classUniqueId = parentId.append("class", testClass.name)
        val classDescriptor = ClassTestDescriptor(classUniqueId, testClass)
        val scenarioLoader = ScenarioLoader()

        files
            .map { file -> createFileDescriptor(classUniqueId, file, scenarioLoader, filters) }
            .forEach { classDescriptor.addChild(it) }

        return classDescriptor
    }

    private fun createFileDescriptor(
        parentId: UniqueId,
        file: DiscoveredScenario,
        loader: ScenarioLoader,
        filters: ScenarioFilters,
    ): ScenarioFileDescriptor {
        val fileId = parentId.append("file", file.name.removeSuffix(".scenario"))
        val fileDescriptor =
            ScenarioFileDescriptor(
                uniqueId = fileId,
                displayName = file.name,
                scenarioPath = file.path,
                scenarioSource = file.url,
            )

        // Try to get the source file for IDE navigation
        // Build output files are mapped back to source files
        val scenarioFile = file.url.toFileOrNull()

        runCatching { loadScenarioFromUrl(loader, file.url) }
            .onSuccess { populateFileDescriptor(fileDescriptor, it, filters, scenarioFile) }
            .onFailure { e ->
                System.err.println("Warning: Failed to parse ${file.path} during discovery: ${e.message}")
            }

        return fileDescriptor
    }

    private fun populateFileDescriptor(
        fileDescriptor: ScenarioFileDescriptor,
        content: ScenarioFileContent,
        filters: ScenarioFilters,
        scenarioFile: File?,
    ) {
        // Add standalone scenarios (expanding outlines), filtered by scenario name
        content.standaloneScenarios
            .addToDescriptor(filters, fileDescriptor.uniqueId, scenarioFile, fileDescriptor)

        // Add feature groups, filtered by feature name
        content.features
            .filter { feature -> filters.matchesFeatureName(feature.name) }
            .map { feature -> createFeatureDescriptor(fileDescriptor.uniqueId, feature, filters, scenarioFile) }
            .forEach { fileDescriptor.addChild(it) }
    }

    private fun List<Scenario>.addToDescriptor(
        filters: ScenarioFilters,
        featureId: UniqueId,
        scenarioFile: File?,
        descriptor: AbstractTestDescriptor,
    ) = this
        .flatMap { scenario -> expandScenarioIfOutline(scenario) }
        .filter { scenario -> filters.matchesScenarioName(scenario.name) }
        .map { scenario -> createScenarioDescriptor(featureId, scenario, scenarioFile) }
        .forEach { descriptor.addChild(it) }

    /**
     * Expand a scenario outline into individual scenarios per example row.
     * Non-outline scenarios are returned as-is.
     */
    private fun expandScenarioIfOutline(scenario: Scenario): List<Scenario> {
        val examples = scenario.examples
        if (examples.isNullOrEmpty()) {
            return listOf(scenario)
        }

        // Expand into one scenario per example row
        return examples.mapIndexed { index, row ->
            scenario.copy(
                name = "${scenario.name} (Example ${index + 1})",
                examples = listOf(row), // Keep only this row's data
            )
        }
    }

    private fun createScenarioDescriptor(
        parentId: UniqueId,
        scenario: Scenario,
        scenarioFile: File? = null,
    ): IndividualScenarioDescriptor {
        val scenarioId = parentId.append("scenario", scenario.name)
        val hasAutoTests = scenario.steps.any { it.autoTestConfig != null }
        val testSource = IndividualScenarioDescriptor.createTestSource(scenarioFile, scenario.sourceLocation)

        return IndividualScenarioDescriptor(
            uniqueId = scenarioId,
            displayName = scenario.name,
            scenario = scenario,
            hasAutoTests = hasAutoTests,
            testSource = testSource,
        )
        // Note: No placeholder children are added during discovery
        // Auto-tests are added dynamically during execution
    }

    private fun createFeatureDescriptor(
        parentId: UniqueId,
        feature: FeatureGroup,
        filters: ScenarioFilters,
        scenarioFile: File? = null,
    ): FeatureDescriptor {
        val featureId = parentId.append("feature", feature.name)
        val testSource = FeatureDescriptor.createTestSource(scenarioFile, feature.sourceLocation)
        val featureDescriptor =
            FeatureDescriptor(
                uniqueId = featureId,
                displayName = feature.name,
                featureName = feature.name,
                parameters = feature.parameters,
                testSource = testSource,
            )

        feature.scenarios.addToDescriptor(filters, featureId, scenarioFile, featureDescriptor)
        return featureDescriptor
    }

    fun loadScenarioFromUrl(
        loader: ScenarioLoader,
        url: URL,
    ): ScenarioFileContent =
        url.openStream().use { input ->
            val content = InputStreamReader(input).readText()
            val fileName = url.path.substringAfterLast("/")
            loader.loadFileContentFromString(content, fileName)
        }
}

/**
 * Convert a URL to a File if the URL represents a file system resource.
 *
 * @return File if the URL is a file:// URL and the file exists, null otherwise
 */
private fun URL.toFileOrNull(): File? =
    if (protocol == "file") {
        runCatching { File(toURI()) }
            .getOrNull()
            ?.takeIf { it.exists() }
    } else {
        null
    }

private fun EngineDescriptor.alreadyDiscovered(testClass: Class<*>): Boolean =
    children.any { it is ClassTestDescriptor && it.testClass == testClass }
