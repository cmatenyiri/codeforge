import { Alert, Stack } from '@mui/material';
import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toApiError } from '../../api/api-error';
import { executionApi } from '../../api/execution-api';
import { type Language, type ProblemDetail, type RunResult } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { CodeEditor } from './CodeEditor';
import { EditorToolbar } from './EditorToolbar';
import { RunConsole, type ConsoleTab } from './RunConsole';
import { DEFAULT_EDITOR_SETTINGS, type EditorSettings } from './editor-settings';
import { useCodeDraft } from './use-code-draft';

/** Preferred default, falling back to whatever the problem does offer. */
const DEFAULT_LANGUAGE: Language = 'JAVA';

type EditorWorkspaceProps = {
  problem: ProblemDetail;
  languages: Language[];
  initialLanguage: Language;
};

const EditorWorkspace = ({ problem, languages, initialLanguage }: EditorWorkspaceProps) => {
  const message = useMessages();
  const [language, setLanguage] = useState<Language>(initialLanguage);

  const starterCode = problem.starterCode[language] ?? '';
  const { code, setCode, reset } = useCodeDraft(problem.slug, language, starterCode);

  const [settings, setSettings] = useState<EditorSettings>(DEFAULT_EDITOR_SETTINGS);
  const [result, setResult] = useState<RunResult | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<ConsoleTab>('testcase');
  const [selectedCase, setSelectedCase] = useState(0);

  const handleRun = useCallback(() => {
    setRunning(true);
    setError(null);
    setTab('result');

    executionApi
      .run(problem.slug, { language, sourceCode: code })
      .then((runResult) => {
        setResult(runResult);
        // Land on the first failure: that is the one the solver needs to read,
        // and on a passing run every case says the same thing anyway.
        const firstFailure = runResult.results.findIndex((caseResult) => caseResult.status !== 'ACCEPTED');
        setSelectedCase(firstFailure === -1 ? 0 : firstFailure);
      })
      .catch((caught: unknown) => {
        const apiError = toApiError(caught);
        setResult(null);
        setError(message(apiError.code, apiError.message));
      })
      .finally(() => {
        setRunning(false);
      });
  }, [problem.slug, language, code, message]);

  const handleLanguageChange = useCallback((next: Language) => {
    setLanguage(next);
    // The previous run described code in another language, so keeping it on
    // screen would attribute those verdicts to what is now in the editor.
    setResult(null);
    setError(null);
    setTab('testcase');
    setSelectedCase(0);
  }, []);

  return (
    <Stack sx={{ height: '100%' }}>
      <EditorToolbar
        language={language}
        languages={languages}
        onLanguageChange={handleLanguageChange}
        settings={settings}
        onSettingsChange={setSettings}
        onReset={reset}
        onRun={handleRun}
        running={running}
        canRun={code.trim().length > 0}
      />

      <CodeEditor
        path={problem.slug}
        value={code}
        language={language}
        settings={settings}
        onChange={setCode}
        onRun={handleRun}
      />

      <RunConsole
        sampleTestCases={problem.sampleTestCases}
        result={result}
        running={running}
        error={error}
        tab={tab}
        onTabChange={setTab}
        selectedCase={selectedCase}
        onSelectCase={setSelectedCase}
      />
    </Stack>
  );
};

/**
 * The right-hand half of the solving page: toolbar, editor and run console.
 *
 * <p>The set of languages comes from the problem rather than being hard-coded —
 * `starterCode` is keyed by the languages the backend can generate a stub and a
 * harness for, so a problem without a signature correctly offers none.
 *
 * <p>Splitting the workspace out is what lets that empty case be handled without
 * lying about types: the workspace takes a language it is guaranteed to have,
 * rather than initialising state from `languages[0]` on a list that may be
 * empty and calling the `undefined` a `Language`.
 */
export const EditorPanel = ({ problem }: { problem: ProblemDetail }) => {
  const { t } = useTranslation();
  const languages = useMemo(() => Object.keys(problem.starterCode) as Language[], [problem.starterCode]);
  const initialLanguage = languages.includes(DEFAULT_LANGUAGE) ? DEFAULT_LANGUAGE : languages[0];

  if (initialLanguage === undefined) {
    return (
      <Stack sx={{ height: '100%', p: 3 }}>
        <Alert severity="info">{t('solve.notSolvable')}</Alert>
      </Stack>
    );
  }

  return <EditorWorkspace problem={problem} languages={languages} initialLanguage={initialLanguage} />;
};
