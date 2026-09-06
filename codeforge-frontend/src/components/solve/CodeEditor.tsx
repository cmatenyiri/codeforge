import Editor, { type OnMount } from '@monaco-editor/react';
import { Box, CircularProgress } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import { useCallback, useEffect, useRef } from 'react';
import { type Language } from '../../api/types';
import { MONACO_FILE_EXTENSION, MONACO_LANGUAGE_ID } from '../../monaco/languages';
import { MONACO_THEME } from '../../monaco/theme';
import { fontFamilyMono } from '../../theme';
import { type EditorSettings } from './editor-settings';
// Side-effecting: wires up the workers, themes and completions before mount.
import '../../monaco/setup';

/** Monaco resolves a model by URI, so the file name has to be one. */
const modelUri = (path: string, language: Language) => `file:///${path}.${MONACO_FILE_EXTENSION[language]}`;

type CodeEditorProps = {
  value: string;
  language: Language;
  settings: EditorSettings;
  onChange: (value: string) => void;
  /** Invoked on Cmd/Ctrl+Enter, so the keyboard can run without leaving the editor. */
  onRun: () => void;
  /** Model identity: combined with the language to name the model's file. */
  path: string;
  /**
   * Locks the buffer. Used by the interview for a problem already moved on
   * from, where the code should stay legible but must not change.
   */
  readOnly?: boolean;
};

export const CodeEditor = ({
  value,
  language,
  settings,
  onChange,
  onRun,
  path,
  readOnly = false,
}: CodeEditorProps) => {
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
   * The last text the editor itself produced.
   *
   * <p>This is what makes the editor safe to type in quickly. `@monaco-editor/react`
   * treats `value` as fully controlled: whenever the prop differs from the model,
   * it replaces the *entire* model range in one edit. React's state update is a
   * round trip, so on a fast burst the prop arrives already stale — and the
   * replace then wipes every character typed since that snapshot and leaves the
   * caret at the end of the file. Measured, a 10–20 ms keystroke interval could
   * lose the whole word.
   *
   * <p>So the prop is only handed to Monaco when the text did *not* come from
   * Monaco — a reset, or a language switch. While React is merely echoing the
   * user's own typing back, `undefined` is passed instead, which the wrapper
   * ignores, leaving the model exactly as the user left it.
   */
  const echoRef = useRef(value);

  const handleChange = useCallback(
    (next: string | undefined) => {
      const text = next ?? '';
      echoRef.current = text;
      onChange(text);
    },
    [onChange],
  );

  const externalValue = value === echoRef.current ? undefined : value;

  /**
   * Records an outside change as though the editor had produced it.
   *
   * <p>The wrapper suppresses `onChange` while applying its own edit, so without
   * this the ref still holds the text from before the push and stays stale. The
   * next genuine outside change would then compare equal to it, be mistaken for
   * an echo, and never reach the model — switching away from a language and back
   * would leave the previous language's code on screen.
   *
   * <p>Runs after the wrapper has applied the edit: effects flush from the child
   * outwards, and the wrapper is this component's child.
   */
  useEffect(() => {
    if (externalValue !== undefined) {
      echoRef.current = externalValue;
    }
  }, [externalValue]);

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
        language={MONACO_LANGUAGE_ID[language]}
        // `defaultValue` seeds a newly created model — one is created per
        // language — while `value` only ever carries an outside change.
        defaultValue={value}
        value={externalValue}
        theme={theme}
        onMount={handleMount}
        onChange={handleChange}
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
          readOnly,
          // A caret in a buffer that cannot be typed into reads as a bug.
          domReadOnly: readOnly,
          renderValidationDecorations: readOnly ? 'off' : 'editable',
        }}
      />
    </Box>
  );
};
