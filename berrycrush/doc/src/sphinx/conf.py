# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'BerryCrush'
copyright = '2026, Takashi Kato'
author = 'Takashi Kato'
version = '0.1.0'
release = '0.1.0-SNAPSHOT'

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.viewcode',
    'sphinx.ext.napoleon',
    'sphinx.ext.intersphinx',
]

# Optional: sphinx_copybutton for code copy button (install with pip if desired)
try:
    import sphinx_copybutton  # noqa: F401
    extensions.append('sphinx_copybutton')
except ImportError:
    pass

templates_path = ['_templates']
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']

# The master toctree document.
master_doc = 'index'

# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

# Use Read the Docs theme if available, otherwise fallback to alabaster
try:
    import sphinx_rtd_theme  # noqa: F401
    html_theme = 'sphinx_rtd_theme'
    html_theme_options = {
        'navigation_depth': 4,
        'collapse_navigation': False,
        'sticky_navigation': True,
        'includehidden': True,
    }
except ImportError:
    html_theme = 'alabaster'
    html_theme_options = {}

html_static_path = ['_static']

# -- Options for intersphinx extension ---------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#confval-intersphinx_mapping

intersphinx_mapping = {
    'python': ('https://docs.python.org/3', None),
}

# -- Options for copybutton extension ----------------------------------------

copybutton_prompt_text = r'>>> |\.\.\. |\$ |> '
copybutton_prompt_is_regexp = True
