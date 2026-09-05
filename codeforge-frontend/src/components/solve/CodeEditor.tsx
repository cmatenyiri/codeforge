import Editor, { type OnMount } from '@monaco-editor/react';
import { Box, CircularProgress } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import { useCallback, useRef } from 'react';
import { type Language } from '../../api/types';
import { MONACO_THEME } from '../../monaco/theme';
import { fontFamilyMono } from '../../theme';
import { type EditorSettings } from './editor-settings';
// Side-effecting: wires up the workers, themes and completions before mount.
import '../../monaco/setup';

/** Our language enum to Monaco's language ids. */
const MONACO_LANGUAGE = {
  JAVA: 'java',
  PYTHON: 'python',
  JAVASCRIPT: 'javascript',
  TYPESCRIPT: 'typescript',
} as const satisfies Record<Language, string>;

type CodeEditorProps = {
  value: string;
  language: Language;
  settings: EditorSettings;
  onChange: (value: string) => void;
  /** Invoked on Cmd/Ctrl+Enter, so the keyboard can run without leaving the editor. */
  onRun: () => void;
  /**
   * Model identity. One model per problem, not per language: switching language
   * then changes the language of the same model rather than leaving a second one
   * behind, which is what stops the TypeScript service from seeing two files
   * declaring the same function and reporting both as duplicates.
   */
  path: string;
};

export const CodeEditor = ({ value, language, settings, onChange, onRun, path }: CodeEditorProps) => {
  const { mode, systemMode } = useColorScheme();

  // Held in a ref so the keybinding registered on mount always calls the current
  // handler; capturing `onRun` directly would freeze the first render's closure.
  const runRef = useRef(onRun);
  runRef.current = onRun;

  const resolvedMode = mode === 'system' ? systemMode : mode;
  const theme = resolvedMode === 'light' ? MONACO_THEME.light : MONACO_THEME.dark;

  const handleMount: OnMount = useCallback((editor, monaco) => {
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      runRef.current();
    });
  }, []);

  return (
    <Box sx={{ flex: 1, minHeight: 0, backgroundColor: 'code.bg' }}>
      <Editor
        path={path}
        language={MONACO_LANGUAGE[language]}
        value={value}
        theme={theme}
        onMount={handleMount}
        onChange={(next) => {
          onChange(next ?? '');
        }}
        loading={<CircularProgress size={24} />}
        options={{
          fontFamily: fontFamilyMono,
          fontSize: settings.fontSize,
          tabSize: settings.tabSize,
          minimap: { enabled: settings.minimap },
          wordWrap: settings.wordWrap ? 'on' : 'off',
          // The panel supplies its own padding and the console sits directly
          // below, so the editor should fill whatever space it is given.
          automaticLayout: true,
          scrollBeyondLastLine: false,
          padding: { top: 12, bottom: 12 },
          renderLineHighlight: 'line',
          smoothScrolling: true,
          cursorBlinking: 'smooth',
          bracketPairColorization: { enabled: true },
          guides: { indentation: true, bracketPairs: false },
          scrollbar: { verticalScrollbarSize: 10, horizontalScrollbarSize: 10, useShadows: false },
          // Completion should feel like an editor, not a form: suggest as you
          // type, but never commit a suggestion on a character the code needs.
          quickSuggestions: { other: true, comments: false, strings: false },
          suggestOnTriggerCharacters: true,
          acceptSuggestionOnEnter: 'on',
          acceptSuggestionOnCommitCharacter: false,
          tabCompletion: 'on',
          suggestSelection: 'first',
          fixedOverflowWidgets: true,
        }}
      />
    </Box>
  );
};
