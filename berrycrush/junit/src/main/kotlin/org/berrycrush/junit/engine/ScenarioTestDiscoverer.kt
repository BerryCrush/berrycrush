package org.berrycrush.junit.engine

import org.berrycrush.junit.BerryCrushScenarios
import org.berrycrush.junit.discovery.DiscoveredScenario
import org.berrycrush.junit.discovery.ScenarioDiscovery
import org.berrycrush.model.Scenario
import org.berrycrush.scenario.FeatureGroup
import org.berrycrush.scenario.ScenarioEntry
import org.berrycrush.scenario.ScenarioFileContent
import org.berrycrush.scenario.ScenarioLoader
import org.berrycrush.scenario.Story
import org.junit.jupiter.api.Disabled
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.jvmName

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
        testClass: KClass<*>,
        filters: ScenarioFilters = ScenarioFilters.EMPTY,
    ) {
        if (testClass.hasAnnotation<Disabled>()) return
        val annotation = testClass.findAnnotation<BerryCrushScenarios>() ?: return

        if (engineDescriptor.alreadyDiscovered(testClass)) return
        if (annotation.locations.isEmpty()) return

        val discoveredFiles =
            discoverScenarioFiles(testClass.java.classLoader, annotation.locations)
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
        testClass: KClass<*>,
        files: List<DiscoveredScenario>,
        filters: ScenarioFilters,
    ): ClassTestDescriptor {
        val classUniqueId = parentId.append("class", testClass.jvmName)
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
                logger.log(Level.SEVERE, e) { "Failed to parse ${file.path} during discovery: ${e.message}" }
            }

        return fileDescriptor
    }

    private fun populateFileDescriptor(
        fileDescriptor: ScenarioFileDescriptor,
        content: ScenarioFileContent,
        filters: ScenarioFilters,
        scenarioFile: File?,
    ) {
        content.stories.forEach { entry: Story ->
            when (entry) {
                is ScenarioEntry ->
                    listOf(entry.scenario)
                        .addToDescriptor(filters, fileDescriptor.uniqueId, scenarioFile, fileDescriptor)

                is FeatureGroup -> {
                    if (filters.matchesFeatureName(entry.name)) {
                        fileDescriptor.addChild(
                            createFeatureDescriptor(fileDescriptor.uniqueId, entry, filters, scenarioFile),
                        )
                    }
                }
            }
        }
    }

    private fun List<Scenario>.addToDescriptor(
        filters: ScenarioFilters,
        featureId: UniqueId,
        scenarioFile: File?,
        descriptor: AbstractTestDescriptor,
        featureName: String? = null,
    ) = this
        .filter { scenario -> filters.matchesScenarioName(scenario.name, featureName) }
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
                name = "Example ${index + 1} - $row",
                examples = listOf(row), // Keep only this row's data
            )
        }
    }

    private fun createScenarioDescriptor(
        parentId: UniqueId,
        scenario: Scenario,
        scenarioFile: File? = null,
        example: Boolean = false,
    ): TestDescriptor {
        val testSource = IndividualScenarioDescriptor.createTestSource(scenarioFile, scenario.sourceLocation)

        return if (!example && !scenario.examples.isNullOrEmpty()) {
            val containerId = parentId.append("container", scenario.name)
            expandScenarioIfOutline(scenario)
                .fold(ContainerDescriptor(containerId, scenario.name, testSource)) { acc, scenario ->
                    val child = createScenarioDescriptor(acc.uniqueId, scenario, scenarioFile, true)
                    acc.addChild(child)
                    acc
                }
        } else {
            val hasAutoTests = scenario.steps.any { it.autoTestConfig != null }
            if (hasAutoTests) {
                val containerId = parentId.append("container", scenario.name)
                ContainerDescriptor(containerId, scenario.name, testSource).apply {
                    val scenarioId = containerId.append("scenario", scenario.name)
                    addChild(
                        IndividualScenarioDescriptor(
                            uniqueId = scenarioId,
                            displayName = "** Auto test **",
                            scenario = scenario,
                            testSource = testSource,
                        ),
                    )
                }
            } else {
                val scenarioId = parentId.append("scenario", scenario.name)
                IndividualScenarioDescriptor(
                    uniqueId = scenarioId,
                    displayName = scenario.name,
                    scenario = scenario,
                    testSource = testSource,
                )
            }
        }
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

        feature.scenarios.addToDescriptor(filters, featureId, scenarioFile, featureDescriptor, feature.name)
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

    private val logger = Logger.getLogger(ScenarioTestDiscoverer::class.java.name)
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

private fun EngineDescriptor.alreadyDiscovered(testClass: KClass<*>): Boolean =
    children.any { it is ClassTestDescriptor && it.testClass == testClass.java }
