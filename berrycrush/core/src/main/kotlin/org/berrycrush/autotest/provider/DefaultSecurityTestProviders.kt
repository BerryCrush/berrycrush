package org.berrycrush.autotest.provider

import org.berrycrush.autotest.AutoTestCase
import org.berrycrush.autotest.AutoTestType
import org.berrycrush.autotest.ParameterLocation

/**
 * Collection of default security test providers.
 */
object DefaultSecurityTestProviders {
    /**
     * All built-in security test providers.
     */
    val all: List<SecurityTestProvider> =
        listOf(
            SqlInjectionProvider(),
            XssProvider(),
            PathTraversalProvider(),
            CommandInjectionProvider(),
            LdapInjectionProvider(),
            XxeProvider(),
            HeaderInjectionProvider(),
            NoSqlInjectionProvider(),
            SstiProvider(),
            JwtAttackProvider(),
            AuthorizationBypassProvider(),
        )
}

/**
 * SQL injection attack payloads.
 */
class SqlInjectionProvider : SecurityTestProvider {
    override val testType: String = "SQLInjection"
    override val displayName: String = "SQL Injection"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Single quote", "' OR '1'='1"),
                SecurityPayload("Union select", "' UNION SELECT * FROM users--"),
                SecurityPayload("Comment bypass", "admin'--"),
                SecurityPayload("Boolean-based", "1' AND '1'='1"),
                SecurityPayload("Stacked queries", "'; DROP TABLE users;--"),
            ),
        )
}

/**
 * Cross-site scripting (XSS) attack payloads.
 */
class XssProvider : SecurityTestProvider {
    override val testType: String = "XSS"
    override val displayName: String = "XSS"

    override fun applicableLocations(): Set<ParameterLocation> =
        setOf(ParameterLocation.BODY, ParameterLocation.QUERY, ParameterLocation.HEADER)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Script tag", "<script>alert('XSS')</script>"),
                SecurityPayload("Event handler", "<img src=x onerror=alert('XSS')>"),
                SecurityPayload("SVG onload", "<svg onload=alert('XSS')>"),
                SecurityPayload("JavaScript URL", "javascript:alert('XSS')"),
                SecurityPayload("HTML injection", "<h1>Injected</h1>"),
            ),
        )
}

/**
 * Path traversal attack payloads.
 */
class PathTraversalProvider : SecurityTestProvider {
    override val testType: String = "PathTraversal"
    override val displayName: String = "Path Traversal"

    override fun applicableLocations(): Set<ParameterLocation> =
        setOf(ParameterLocation.PATH, ParameterLocation.QUERY, ParameterLocation.BODY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Unix relative", "../../../etc/passwd"),
                SecurityPayload("Windows relative", "..%5C..%5C..%5Cwindows%5Csystem32%5Cconfig%5Csam"),
                SecurityPayload("URL encoded", "..%2F..%2F..%2Fetc%2Fpasswd"),
                SecurityPayload("Double encoded", "..%252F..%252F..%252Fetc%252Fpasswd"),
            ),
        )
}

/**
 * Command injection attack payloads.
 */
class CommandInjectionProvider : SecurityTestProvider {
    override val testType: String = "CommandInjection"
    override val displayName: String = "Command Injection"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Unix semicolon", "; ls -la"),
                SecurityPayload("Unix pipe", "| cat /etc/passwd"),
                SecurityPayload("Unix backtick", "`id`"),
                SecurityPayload("Unix subshell", "$(whoami)"),
                SecurityPayload("Windows ampersand", "& dir"),
            ),
        )
}

/**
 * LDAP injection attack payloads.
 */
class LdapInjectionProvider : SecurityTestProvider {
    override val testType: String = "LDAPInjection"
    override val displayName: String = "LDAP Injection"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Wildcard", "*"),
                SecurityPayload("Filter bypass", "admin)(&)"),
            ),
        )
}

/**
 * XML External Entity (XXE) attack payloads.
 */
class XxeProvider : SecurityTestProvider {
    override val testType: String = "XXE"
    override val displayName: String = "XXE"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload(
                    "External entity",
                    "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>",
                ),
                SecurityPayload("CDATA", "<![CDATA[<script>alert('XSS')</script>]]>"),
            ),
        )
}

/**
 * HTTP header injection attack payloads.
 */
class HeaderInjectionProvider : SecurityTestProvider {
    override val testType: String = "HeaderInjection"
    override val displayName: String = "Header Injection"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.HEADER)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("CRLF injection", "value\r\nX-Injected: header"),
                SecurityPayload("Null byte", "value\u0000injection"),
            ),
        )
}

/**
 * NoSQL injection attack payloads (MongoDB, etc.).
 */
class NoSqlInjectionProvider : SecurityTestProvider {
    override val testType: String = "NoSQLInjection"
    override val displayName: String = "NoSQL Injection"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("MongoDB \$ne", "{\"\$ne\": null}"),
                SecurityPayload("MongoDB \$where", "{\"\$where\": \"1==1\"}"),
                SecurityPayload("MongoDB \$regex", "{\"\$regex\": \".*\"}"),
                SecurityPayload("MongoDB \$gt", "{\"\$gt\": \"\"}"),
                SecurityPayload("MongoDB JS injection", "{\"\$where\": \"function(){return true}\"}"),
            ),
        )
}

/**
 * Server-Side Template Injection (SSTI) attack payloads.
 */
class SstiProvider : SecurityTestProvider {
    override val testType: String = "SSTI"
    override val displayName: String = "Template Injection"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.BODY, ParameterLocation.QUERY)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Jinja2/Twig", "{{7*7}}"),
                SecurityPayload("Freemarker", "\${7*7}"),
                SecurityPayload("Velocity", "#set(\$x=7*7)\$x"),
                SecurityPayload("Smarty", "{php}echo 'test';{/php}"),
                SecurityPayload("ERB", "<%= 7*7 %>"),
            ),
        )
}

/**
 * JWT attack payloads for Authorization header.
 */
class JwtAttackProvider : SecurityTestProvider {
    override val testType: String = "JWT"
    override val displayName: String = "JWT Attacks"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.HEADER)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Algorithm none", "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiIxMjM0NTY3ODkwIn0."),
                SecurityPayload("Malformed JWT", "Bearer not.a.jwt"),
                SecurityPayload("Empty signature", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0."),
                SecurityPayload("Invalid base64", "Bearer !!!invalid!!!.!!!base64!!!.!!!jwt!!!"),
                SecurityPayload("Missing Bearer", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.xxx"),
            ),
        )
}

/**
 * Authorization bypass attack payloads.
 */
class AuthorizationBypassProvider : SecurityTestProvider {
    override val testType: String = "AuthorizationBypass"
    override val displayName: String = "Authorization Bypass"

    override fun applicableLocations(): Set<ParameterLocation> = setOf(ParameterLocation.HEADER)

    override fun generateTestCases(request: SecurityTestRequest): List<AutoTestCase> =
        request.toSecurityCases(
            testType,
            displayName,
            listOf(
                SecurityPayload("Empty auth", ""),
                SecurityPayload("Invalid scheme", "Basic !!!invalid!!!"),
                SecurityPayload("Empty bearer", "Bearer "),
                SecurityPayload("Null bearer", "Bearer null"),
                SecurityPayload("Undefined bearer", "Bearer undefined"),
            ),
        )
}

private fun SecurityTestRequest.toSecurityCases(
    testType: String,
    displayName: String,
    payloads: List<SecurityPayload>,
): List<AutoTestCase> =
    payloads.map { payload ->
        when (location) {
            ParameterLocation.BODY -> {
                val body = baseBody.toMutableMap()
                body[fieldName] = payload.payload
                toSecurityCase(testType, displayName, payload, body = body)
            }

            ParameterLocation.PATH -> {
                val pathParams = basePathParams.toMutableMap()
                pathParams[fieldName] = payload.payload
                toSecurityCase(testType, displayName, payload, pathParams = pathParams)
            }

            ParameterLocation.HEADER -> {
                val headers = baseHeaders.toMutableMap()
                headers[fieldName] = payload.payload
                toSecurityCase(testType, displayName, payload, headers = headers)
            }

            ParameterLocation.QUERY -> toSecurityCase(testType, displayName, payload)
        }
    }

private fun SecurityTestRequest.toSecurityCase(
    testType: String,
    displayName: String,
    payload: SecurityPayload,
    body: Map<String, Any?> = baseBody,
    pathParams: Map<String, Any?> = basePathParams,
    headers: Map<String, String> = baseHeaders,
): AutoTestCase =
    AutoTestCase(
        type = AutoTestType.SECURITY,
        testType = testType,
        fieldName = fieldName,
        invalidValue = payload.payload,
        description = "$displayName: ${payload.name}",
        location = location,
        body = body,
        pathParams = pathParams,
        headers = headers,
        tag = "security - $displayName",
    )
