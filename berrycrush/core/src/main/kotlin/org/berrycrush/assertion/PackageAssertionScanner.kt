package org.berrycrush.assertion

import org.berrycrush.scanner.PackageScanner

/**
 * Scans packages for classes containing [@Assertion] annotated methods.
 *
 * Uses classpath reflection to discover annotation definition classes in specified packges.
 */
class PackageAssertionScanner : PackageScanner<AssertionDefinition>(AnnotationAssertionScanner(), Assertion::class.java)
