import Editor, { type OnMount } from '@monaco-editor/react';
import { Box, CircularProgress } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import { useCallback, useEffect, useRef } from 'react';
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

/**
 * The extension the model's file name gets.
 *
 * <p>Not cosmetic. The TypeScript worker decides whether a file is TypeScript or
 * JavaScript from its extension alone, not from the editor's language id — so a
 * model at an extension-less path is parsed as JavaScript, and every type
 * annotation is reported as "Type annotations can only be used in TypeScript
 * files".
 */
const FILE_EXTENSION = {
  JAVA: 'java',
  PYTHON: 'py',
  JAVASCRIPT: 'js',
  TYPESCRIPT: 'ts',
} as const satisfies Record<Language, string>;

/** Monaco resolves a model by URI, so the file name has to be one. */
const modelUri = (path: string, language: Language) => `file:///${path}.${FILE_EXTENSION[language]}`;

type CodeEditorProps = {
  value: string;
  language: Language;
  settings: EditorSettings;
  onChange: (value: string) => void;
  /** Invoked on Cmd/Ctrl+Enter, so the keyboard can run without leaving the editor. */
  onRun: () => void;
  /** Model identity: combined with the language to name the model's file. */
  path: string;
};

export const CodeEditor = ({ value, language, settings, onChange, onRun, path }: CodeEditorProps) => {
  const { mode, systemMode } = useColorScheme();

  // Held in a ref so the keybinding registered on mount always calls the current
  // handler; capturing `onRun` directly would freeze the first render's closure.
  const runRef = useRef(onRun);
  runRef.current = onRun;

  const editorRef = useRef<Parameters<OnMount>[0] | null>(null);
  const monacoRef = useRef<Parameters<OnMount>[1] | null>(null);

  const resolvedMode = mode === 'system' ? systemMode : mode;
  const theme = resolvedMode === 'light' ? MONACO_THEME.light : MONACO_THEME.dark;
  const uri = modelUri(path, language);

  const handleMount: OnMount = useCallback((editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      runRef.current();
    });
  }, []);

  /**
   * Throws away the models the other languages left behind.
   *
   * <p>Giving each language its own file name means switching language creates a
   * second model rather than relabelling the first, and the wrapper only
   * disposes models on unmount. Left alone, `solution.js` and `solution.ts` both
   * stay in the TypeScript project declaring the same function, and the service
   * reports a duplicate implementation in each — a red underline on correct code
   * that no amount of editing clears.
   *
   * <p>This runs after the wrapper has already attached the new model, because
   * effects flush from the child outwards, and it compares against the editor's
   * live model rather than a computed name so it can never dispose the one on
   * screen.
   */
  useEffect(() => {
    const monaco = monacoRef.current;
    const current = editorRef.current?.getModel();
    if (!monaco || !current) {
      return;
    }

    const prefix = `/${path}.`;
    for (const model of monaco.editor.getModels()) {
      if (model !== current && model.uri.path.startsWith(prefix)) {
        model.dispose();
      }
    }
  }, [uri, path]);

  return (
    <Box sx={{ flex: 1, minHeight: 0, backgroundColor: 'code.bg' }}>
      <Editor
        path={uri}
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
