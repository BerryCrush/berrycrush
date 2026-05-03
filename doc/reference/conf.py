# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup for custom extensions ----------------------------------------
import os
import sys
sys.path.insert(0, os.path.abspath('../sphinx/_extensions'))

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'BerryCrush'
copyright = '2026, Takashi Kato'
author = 'Takashi Kato'

# -- Dynamic version configuration -------------------------------------------
# Read version from gradle.properties or environment variable
_default_version = '0.1.0'
_default_release = '0.1.0-SNAPSHOT'

# Try to read from gradle.properties
_gradle_props_path = os.path.join(os.path.dirname(__file__), '../../gradle.properties')
try:
    with open(_gradle_props_path, 'r') as f:
        for line in f:
            if line.startswith('version='):
                _full_version = line.split('=')[1].strip()
                _default_release = _full_version
                # Extract base version (remove -SNAPSHOT etc.)
                _default_version = _full_version.split('-')[0]
                break
except FileNotFoundError:
    pass

# Environment variables override gradle.properties (for CI builds)
version = os.environ.get('DOC_VERSION', _default_version)
release = os.environ.get('DOC_RELEASE', _default_release)

# -- Dynamic API documentation URL -------------------------------------------
# Configure API doc URL based on build context
_api_doc_base = 'https://doc.berrycrush.org'
_doc_tag = os.environ.get('DOC_TAG', 'current')
api_doc_url = f"{_api_doc_base}/{_doc_tag}/kdoc"

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.viewcode',
    'sphinx.ext.napoleon',
    'sphinx.ext.intersphinx',
    'berrycrush_lexer',
]

# Optional: sphinx_tabs for tabbed content (build system options)
try:
    import sphinx_tabs  # noqa: F401
    extensions.append('sphinx_tabs.tabs')
except ImportError:
    pass

# Optional: sphinx_copybutton for code copy button
try:
    import sphinx_copybutton  # noqa: F401
    extensions.append('sphinx_copybutton')
except ImportError:
    pass

# -- RST prolog for substitutions --------------------------------------------
# Define substitutions available in all RST files
rst_prolog = f"""
.. |version| replace:: {version}
.. |release| replace:: {release}
.. |api_doc_url| replace:: {api_doc_url}
"""

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
