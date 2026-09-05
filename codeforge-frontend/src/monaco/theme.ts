// A whole-statement `import type`, and pointed at the editor API rather than the
// package root. Under `verbatimModuleSyntax` an inline `{ type editor }` still
// emits `import {} from 'monaco-editor'`, whose side effect is registering every
// language grammar Monaco ships — the exact thing `setup.ts` avoids.
import type { editor } from 'monaco-editor/editor/editor.api';
import { dark, light } from '../theme/tokens';

/**
 * Monaco themes built from the CodeForge design tokens.
 *
 * <p>This is the one place allowed to read `theme/tokens` directly rather than
 * going through `theme.palette.*`. The theme is emitted as CSS variables, so
 * `palette.code.keyword` resolves to the string `var(--mui-palette-code-keyword)`
 * — and Monaco parses colors itself, in JavaScript, where a CSS variable means
 * nothing. Reading the primitives is what keeps the editor's syntax colors the
 * same ones the rest of the app uses.
 */

type Scheme = typeof dark | typeof light;

/** Monaco wants token colors bare and workbench colors prefixed, so both are normalised here. */
const hex = (color: string): string => {
  const rgba = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+)\s*)?\)$/.exec(color);
  if (!rgba) {
    return color.replace('#', '');
  }

  const [, r, g, b, a] = rgba;
  const channel = (value: string) => Number(value).toString(16).padStart(2, '0');
  const alpha = a === undefined ? '' : Math.round(Number(a) * 255).toString(16).padStart(2, '0');

  return `${channel(r)}${channel(g)}${channel(b)}${alpha}`;
};

const withHash = (color: string): string => `#${hex(color)}`;

const buildTheme = (t: Scheme, base: editor.BuiltinTheme): editor.IStandaloneThemeData => ({
  base,
  // Our rules cover the token types the four supported languages emit; anything
  // else should still be coloured rather than fall back to Monaco's defaults.
  inherit: true,
  rules: [
    { token: '', foreground: hex(t.code.variable) },
    { token: 'comment', foreground: hex(t.code.comment), fontStyle: 'italic' },
    { token: 'keyword', foreground: hex(t.code.keyword) },
    { token: 'operator', foreground: hex(t.code.operator) },
    { token: 'delimiter', foreground: hex(t.code.operator) },
    { token: 'string', foreground: hex(t.code.string) },
    { token: 'number', foreground: hex(t.code.number) },
    { token: 'regexp', foreground: hex(t.code.string) },
    { token: 'type', foreground: hex(t.code.type) },
    { token: 'type.identifier', foreground: hex(t.code.type) },
    { token: 'identifier', foreground: hex(t.code.variable) },
    // Monaco's Java and JS tokenizers both use `constructor` for class-like names.
    { token: 'constructor', foreground: hex(t.code.type) },
    { token: 'annotation', foreground: hex(t.code.func) },
    { token: 'tag', foreground: hex(t.code.keyword) },
    { token: 'attribute.name', foreground: hex(t.code.func) },
    { token: 'attribute.value', foreground: hex(t.code.string) },
  ],
  colors: {
    'editor.background': withHash(t.code.bg),
    'editor.foreground': withHash(t.code.variable),
    'editor.lineHighlightBackground': withHash(t.code.lineHighlight),
    'editor.selectionBackground': withHash(t.surface.selected),
    'editor.inactiveSelectionBackground': withHash(t.surface.hover),
    'editorCursor.foreground': withHash(t.brand.violet),
    'editorLineNumber.foreground': withHash(t.code.gutter),
    'editorLineNumber.activeForeground': withHash(t.text.secondary),
    'editorIndentGuide.background1': withHash(t.border.subtle),
    'editorIndentGuide.activeBackground1': withHash(t.border.default),
    'editorWhitespace.foreground': withHash(t.border.default),
    'editorBracketMatch.background': withHash(t.surface.selected),
    'editorBracketMatch.border': withHash(t.brand.violet),
    // The completion popup is the reason for most of what follows: left to the
    // built-in themes it renders as a slab of unrelated grey over the editor.
    'editorWidget.background': withHash(t.surface.overlay),
    'editorWidget.border': withHash(t.border.default),
    'editorSuggestWidget.background': withHash(t.surface.overlay),
    'editorSuggestWidget.border': withHash(t.border.default),
    'editorSuggestWidget.foreground': withHash(t.text.primary),
    'editorSuggestWidget.selectedBackground': withHash(t.surface.selected),
    'editorSuggestWidget.highlightForeground': withHash(t.brand.violet),
    'editorHoverWidget.background': withHash(t.surface.overlay),
    'editorHoverWidget.border': withHash(t.border.default),
    'editorError.foreground': withHash(t.semantic.error),
    'editorWarning.foreground': withHash(t.semantic.warning),
    'scrollbarSlider.background': withHash(t.border.default),
    'scrollbarSlider.hoverBackground': withHash(t.border.strong),
    'scrollbarSlider.activeBackground': withHash(t.border.strong),
  },
});

export const MONACO_THEME = {
  dark: 'codeforge-dark',
  light: 'codeforge-light',
} as const;

export const monacoThemes: Record<string, editor.IStandaloneThemeData> = {
  [MONACO_THEME.dark]: buildTheme(dark, 'vs-dark'),
  [MONACO_THEME.light]: buildTheme(light, 'vs'),
};
