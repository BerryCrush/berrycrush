package org.berrycrush.step

import org.berrycrush.scanner.PackageScanner
import org.berrycrush.util.StepDefinition

/**
 * Scans packages for classes containing @Step annotated methods.
 *
 * Uses classpath reflection to discover step definition classes in specified packages.
 */
class PackageStepScanner : PackageScanner<StepDefinition>(AnnotationStepScanner(), Step::class.java)
