package org.berrycrush.context

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for parallel execution safety of ExecutionContext.
 *
 * These tests verify that:
 * - ExecutionContext is thread-safe for concurrent access
 * - Isolated copies are truly independent
 * - No data races occur under concurrent modification
 */
class ExecutionContextParallelTest {
    @Test
    fun `concurrent variable writes should be thread-safe`() {
        val context = ExecutionContext()
        val threadCount = 10
        val iterationsPerThread = 1000
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            repeat(threadCount) { threadIndex ->
                executor.submit {
                    try {
                        repeat(iterationsPerThread) { iteration ->
                            val key = "thread${threadIndex}_var$iteration"
                            context[key] = "value_$iteration"
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "Threads should complete in time")

            // Verify all variables were written
            val expectedCount = threadCount * iterationsPerThread
            assertEquals(expectedCount, context.variableNames().size)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `concurrent reads and writes should be thread-safe`() {
        val context = ExecutionContext()
        context["sharedKey"] = "initial"

        val readerCount = 5
        val writerCount = 5
        val iterations = 500
        val readErrors = AtomicInteger(0)
        val latch = CountDownLatch(readerCount + writerCount)
        val executor = Executors.newFixedThreadPool(readerCount + writerCount)

        try {
            // Writers
            repeat(writerCount) { writerIndex ->
                executor.submit {
                    try {
                        repeat(iterations) { i ->
                            context["sharedKey"] = "writer${writerIndex}_$i"
                            context["writer${writerIndex}_key$i"] = "value"
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // Readers
            repeat(readerCount) {
                executor.submit {
                    try {
                        repeat(iterations) {
                            try {
                                // These should never throw, just return null or a value
                                context.get<String>("sharedKey")
                                context.contains("sharedKey")
                                context.variableNames()
                            } catch (
                                @Suppress("SwallowedException")
                                e: Exception,
                            ) {
                                // Track errors - exception details not needed for counting
                                readErrors.incrementAndGet()
                            }
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "Threads should complete in time")
            assertEquals(0, readErrors.get(), "No read errors should occur")
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `isolated copies should not affect each other under concurrent modification`() {
        val original = ExecutionContext()
        original["base"] = "original"

        val copyCount = 10
        val copies = (1..copyCount).map { original.createIsolatedCopy() }
        val results = ConcurrentHashMap<Int, String>()
        val latch = CountDownLatch(copyCount)
        val executor = Executors.newFixedThreadPool(copyCount)

        try {
            copies.forEachIndexed { index, copy ->
                executor.submit {
                    try {
                        // Each copy modifies its own state
                        copy["base"] = "modified_$index"
                        copy["unique_$index"] = "unique_value_$index"

                        // Simulate some work
                        Thread.sleep(10)

                        // Store final value for verification
                        results[index] = copy.get<String>("base") ?: "null"
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "Threads should complete in time")

            // Verify each copy maintained its own state
            results.forEach { (index, value) ->
                assertEquals("modified_$index", value, "Copy $index should have its own value")
            }

            // Verify original was not affected
            assertEquals("original", original.get<String>("base"))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `volatile fields should be visible across threads`() {
        val context = ExecutionContext()
        val updateCount = 100
        val latch = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)
        val lastSeenValues = ConcurrentHashMap<String, Long>()

        try {
            // Writer thread
            executor.submit {
                try {
                    repeat(updateCount) { i ->
                        context.updateLastResponseTime(i.toLong())
                        Thread.sleep(1)
                    }
                } finally {
                    latch.countDown()
                }
            }

            // Reader thread
            executor.submit {
                try {
                    repeat(updateCount * 2) {
                        val value = context.lastResponseTimeMs ?: 0L
                        lastSeenValues["last"] = value
                        Thread.sleep(1)
                    }
                } finally {
                    latch.countDown()
                }
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "Threads should complete in time")

            // Reader should have seen some values > 0 (proving visibility)
            val lastValue = lastSeenValues["last"] ?: 0L
            assertTrue(lastValue > 0, "Reader should have seen updated values")
        } finally {
            executor.shutdownNow()
        }
    }

    @Suppress("LongMethod") // Comprehensive parallel simulation test
    @Test
    fun `parallel scenario simulation should maintain isolation`() {
        // Simulate what happens in parallel JUnit test execution:
        // - Multiple scenarios start with isolated contexts
        // - Each scenario modifies its own context
        // - No cross-contamination should occur

        val scenarioCount = 20
        val variablesPerScenario = 50
        val sharedConfig = ExecutionContext() // Simulates shared "base" context
        sharedConfig["configValue"] = "shared"

        val scenarioResults = ConcurrentHashMap<Int, Map<String, Any?>>()
        val latch = CountDownLatch(scenarioCount)
        val executor = Executors.newFixedThreadPool(scenarioCount)

        try {
            repeat(scenarioCount) { scenarioIndex ->
                executor.submit {
                    try {
                        // Each scenario creates its own isolated context
                        val scenarioContext = sharedConfig.createIsolatedCopy()

                        // Simulate scenario execution
                        repeat(variablesPerScenario) { varIndex ->
                            scenarioContext["scenario${scenarioIndex}_var$varIndex"] =
                                "value_${scenarioIndex}_$varIndex"
                        }

                        // Simulate response extraction
                        scenarioContext["extractedId"] = "id_$scenarioIndex"
                        scenarioContext.updateLastResponseTime((100 + scenarioIndex).toLong())

                        // Store results for verification
                        scenarioResults[scenarioIndex] =
                            mapOf(
                                "extractedId" to scenarioContext.get<String>("extractedId"),
                                "configValue" to scenarioContext.get<String>("configValue"),
                                "variableCount" to scenarioContext.variableNames().size,
                                "responseTime" to scenarioContext.lastResponseTimeMs,
                            )
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertTrue(latch.await(60, TimeUnit.SECONDS), "Scenarios should complete in time")

            // Verify each scenario maintained proper isolation
            repeat(scenarioCount) { scenarioIndex ->
                val result = scenarioResults[scenarioIndex]!!
                assertEquals(
                    "id_$scenarioIndex",
                    result["extractedId"],
                    "Scenario $scenarioIndex should have its own extractedId",
                )
                assertEquals(
                    "shared",
                    result["configValue"],
                    "Scenario $scenarioIndex should have inherited config",
                )
                assertEquals(
                    variablesPerScenario + 2,
                    result["variableCount"],
                    "Scenario $scenarioIndex should have correct variable count",
                )
                assertEquals(
                    (100 + scenarioIndex).toLong(),
                    result["responseTime"],
                    "Scenario $scenarioIndex should have its own response time",
                )
            }

            // Verify shared config was not modified
            assertEquals("shared", sharedConfig.get<String>("configValue"))
            assertEquals(1, sharedConfig.variableNames().size)
        } finally {
            executor.shutdownNow()
        }
    }
}
