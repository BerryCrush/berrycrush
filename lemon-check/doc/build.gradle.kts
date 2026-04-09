plugins {
    alias(libs.plugins.kotlin.jvm)
}

description = "lemon-check documentation module (Sphinx + Dokka)"

// Sphinx documentation build task
tasks.register<Exec>("buildSphinx") {
    group = "documentation"
    description = "Build Sphinx documentation"

    workingDir = file("src/sphinx")

    // Check if sphinx-build is available
    commandLine(
        "sphinx-build",
        "-b",
        "html",
        ".",
        "${layout.buildDirectory.get()}/sphinx/html",
    )

    doFirst {
        layout.buildDirectory
            .dir("sphinx/html")
            .get()
            .asFile
            .mkdirs()
    }
}

// Alternative task for environments without Sphinx
tasks.register<Copy>("copyDocs") {
    group = "documentation"
    description = "Copy RST files for documentation (fallback without Sphinx)"

    from("src/sphinx")
    into(layout.buildDirectory.dir("docs"))
    include("**/*.rst")
}
