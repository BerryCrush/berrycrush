"""
Pygments lexer for BerryCrush scenario/fragment files.

This lexer provides syntax highlighting for BerryCrush BDD test files
in Sphinx documentation.
"""

from pygments.lexer import RegexLexer, bygroups, include, words
from pygments.token import (
    Comment,
    Keyword,
    Name,
    Number,
    Operator,
    Punctuation,
    String,
    Whitespace,
)


class BerryCrushLexer(RegexLexer):
    """Pygments lexer for BerryCrush scenario/fragment files."""

    name = 'BerryCrush'
    aliases = ['berrycrush', 'scenario', 'fragment']
    filenames = ['*.scenario', '*.fragment']
    mimetypes = ['text/x-berrycrush']

    tokens = {
        'root': [
            # Comments
            (r'#.*$', Comment.Single),

            # Tags (@smoke, @api, @critical, etc.)
            (r'@[\w\-]+', Name.Decorator),

            # Block keywords (scenario:, feature:, fragment:, etc.)
            (r'\b(scenario|feature|fragment|parameters|background|examples|outline)(:)',
             bygroups(Keyword, Punctuation)),

            # Step keywords
            (r'\b(given|when|then|and|but)\b', Keyword),

            # Directives with operation reference
            (r'(call)(\s+)(using)(\s+)(\w+)(\s+)(\^)([\w\-]+)',
             bygroups(Keyword.Declaration, Whitespace, Keyword.Namespace,
                      Whitespace, Name.Namespace, Whitespace, Operator, Name.Function)),
            (r'(call)(\s+)(\^)([\w\-]+)',
             bygroups(Keyword.Declaration, Whitespace, Operator, Name.Function)),

            # Include directive
            (r'(include)(\s+)([\w\-]+)',
             bygroups(Keyword.Declaration, Whitespace, Name.Label)),

            # Fail directive
            (r'(fail)(\s+)(.*)',
             bygroups(Keyword.Declaration, Whitespace, String)),

            # Auto-test directives
            (r'(auto)(:)(\s*)(\[)',
             bygroups(Keyword, Punctuation, Whitespace, Punctuation), 'auto-types'),
            (r'(excludes)(:)(\s*)(\[)',
             bygroups(Keyword, Punctuation, Whitespace, Punctuation), 'excludes-list'),

            # Multi-test parameters
            (r'(count|mode)(:)(\s*)(\d+|sequential|concurrent)',
             bygroups(Name.Attribute, Punctuation, Whitespace, Number)),

            # Assert directives
            (r'(assert)(\s+)(statusCode?|status)(\s+)(\d+(?:xx|-\d+)?)',
             bygroups(Keyword.Declaration, Whitespace, Name.Builtin, Whitespace, Number)),
            (r'(assert)(\s+)(header)(\s+)([\w\-]+)',
             bygroups(Keyword.Declaration, Whitespace, Name.Builtin,
                      Whitespace, Name.Variable)),
            (r'(assert)(\s+)(schema|responseTime|contains)',
             bygroups(Keyword.Declaration, Whitespace, Name.Builtin)),
            (r'(assert)(\s+)(\$[\w\.\[\]\*]+)(\s+)',
             bygroups(Keyword.Declaration, Whitespace, String.Symbol, Whitespace),
             'assertion-operator'),

            # Extract directive
            (r'(extract)(\s+)(\$[\w\.\[\]\*]+)(\s*)(=>)(\s*)([\w]+)',
             bygroups(Keyword.Declaration, Whitespace, String.Symbol, Whitespace,
                      Operator, Whitespace, Name.Variable)),

            # Body directive
            (r'(body|bodyfile)(:)', bygroups(Keyword.Declaration, Punctuation)),

            # Conditionals
            (r'\b(if|else\s+if|else)\b', Keyword),

            # Parameter entries (name: value)
            (r'^(\s+)([\w\.]+)(:)(\s*)',
             bygroups(Whitespace, Name.Attribute, Punctuation, Whitespace)),

            # Test context variables
            (r'\btest\.(type|field|description|value|location)\b', Name.Variable.Magic),

            # Variable interpolation ({{variable}})
            (r'\{\{[\w\.]+\}\}', Name.Variable),

            # Status codes and numbers
            (r'\b[1-5][0-9]{2}\b', Number),
            (r'\b\d+\b', Number),

            # Strings
            (r'"[^"]*"', String.Double),
            (r"'[^']*'", String.Single),

            # JSON content (basic)
            include('json'),

            # Whitespace
            (r'\s+', Whitespace),

            # Identifiers
            (r'[\w\-]+', Name),
        ],

        'auto-types': [
            (r'\]', Punctuation, '#pop'),
            (r',', Punctuation),
            (r'\s+', Whitespace),
            (r'invalid|security|multi', Keyword.Type),
            (r'[\w\-]+', Name.Tag),
        ],

        'excludes-list': [
            (r'\]', Punctuation, '#pop'),
            (r',', Punctuation),
            (r'\s+', Whitespace),
            (r'[\w\-]+', String),
        ],

        'assertion-operator': [
            (r'(not\s+)?(equals|exists|notEmpty|greaterThan|lessThan|contains|in|hasSize|size|matches|=)',
             bygroups(Keyword.Operator, Keyword.Operator), '#pop'),
            (r'\s+', Whitespace),
        ],

        'json': [
            (r'[{}\[\]]', Punctuation),
            (r':', Operator),
            (r',', Punctuation),
            (r'"[^"]*"\s*(?=:)', Name.Tag),  # JSON keys
            (r'"[^"]*"', String),
            (r'-?\d+\.?\d*', Number),
            (r'\b(true|false|null)\b', Keyword.Constant),
        ],
    }


def setup(app):
    """Sphinx extension entry point for automatic registration."""
    from sphinx.highlighting import lexers
    lexer = BerryCrushLexer()
    lexers['berrycrush'] = lexer
    lexers['scenario'] = lexer
    lexers['fragment'] = lexer
    return {'version': '1.0', 'parallel_read_safe': True}
