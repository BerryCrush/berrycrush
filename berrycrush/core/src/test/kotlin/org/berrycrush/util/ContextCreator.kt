package org.berrycrush.util

import org.berrycrush.context.ExecutionContext
import org.berrycrush.plugin.HttpRequest
import org.berrycrush.plugin.HttpResponse
import org.berrycrush.plugin.ScenarioContext
import org.berrycrush.plugin.StepContext
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

fun createStepContext(context: ExecutionContext = ExecutionContext()) =
    object : StepContext {
        private val dummyScenarioContext =
            object : ScenarioContext {
                override val scenarioName: String
                    get() = TODO("Not yet implemented")
                override val scenarioFile: Path
                    get() = TODO("Not yet implemented")
                override val variables: MutableMap<String, Any>
                    get() = TODO("Not yet implemented")
                override val metadata: Map<String, String>
                    get() = TODO("Not yet implemented")
                override val startTime: Instant
                    get() = TODO("Not yet implemented")
                override val tags: Set<String>
                    get() = TODO("Not yet implemented")
                override val audits: List<ScenarioContext.HttpAudit>
                    get() = TODO("Not yet implemented")
                override val executionContext: ExecutionContext
                    get() = context

                override fun addAudit(
                    request: HttpRequest,
                    response: HttpResponse,
                ) {
                    TODO("Not yet implemented")
                }
            }
        override val stepDescription: String
            get() = TODO("Not yet implemented")
        override val stepType: org.berrycrush.plugin.StepType
            get() = TODO("Not yet implemented")
        override val stepIndex: Int
            get() = TODO("Not yet implemented")
        override val scenarioContext: ScenarioContext
            get() = dummyScenarioContext
        override val request: HttpRequest
            get() = TODO("Not yet implemented")
        override val response: HttpResponse
            get() = TODO("Not yet implemented")
        override val operationId: String?
            get() = TODO("Not yet implemented")
        override val responseTime: Duration?
            get() = TODO("Not yet implemented")

        override fun updateResponseTime(responseTime: Duration) {
            TODO("Not yet implemented")
        }
    }
