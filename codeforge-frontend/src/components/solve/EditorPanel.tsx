import { Alert, Snackbar, Stack } from '@mui/material';
import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toApiError } from '../../api/api-error';
import { executionApi } from '../../api/execution-api';
import { type Language, type ProblemDetail } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { CodeEditor } from './CodeEditor';
import { EditorToolbar } from './EditorToolbar';
import { RunConsole, type ConsoleOutcome, type ConsoleTab } from './RunConsole';
import { DEFAULT_EDITOR_SETTINGS, type EditorSettings } from './editor-settings';
import { useCodeDraft } from './use-code-draft';

/** Preferred default, falling back to whatever the problem does offer. */
const DEFAULT_LANGUAGE: Language = 'JAVA';

type EditorWorkspaceProps = {
  problem: ProblemDetail;
  languages: Language[];
  initialLanguage: Language;
  /**
   * Called once per recorded submission — accepted or not. The page reloads the
   * problem from it, which is what updates the solved badge, the acceptance rate
   * and the submissions tab.
   */
  onSubmitted: () => void;
};

const EditorWorkspace = ({ problem, languages, initialLanguage, onSubmitted }: EditorWorkspaceProps) => {
  const { t } = useTranslation();
  const message = useMessages();
  const [language, setLanguage] = useState<Language>(initialLanguage);

  const starterCode = problem.starterCode[language] ?? '';
  const { code, setCode, reset } = useCodeDraft(problem.slug, language, starterCode);

  const [settings, setSettings] = useState<EditorSettings>(DEFAULT_EDITOR_SETTINGS);
  const [outcome, setOutcome] = useState<ConsoleOutcome | null>(null);
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<ConsoleTab>('testcase');
  const [selectedCase, setSelectedCase] = useState(0);
  const [celebration, setCelebration] = useState<string | null>(null);

  /** Land on the first failure: that is the case the solver needs to read. */
  const focusFirstFailure = useCallback((results: { status: string }[]) => {
    const firstFailure = results.findIndex((result) => result.status !== 'ACCEPTED');
    setSelectedCase(firstFailure === -1 ? 0 : firstFailure);
  }, []);

  const handleFailure = useCallback(
    (caught: unknown) => {
      const apiError = toApiError(caught);
      setOutcome(null);
      setError(message(apiError.code, apiError.message));
    },
    [message],
  );

  const handleRun = useCallback(() => {
    setRunning(true);
    setError(null);
    setTab('result');

    executionApi
      .run(problem.slug, { language, sourceCode: code })
      .then((result) => {
        setOutcome({ kind: 'run', result });
        focusFirstFailure(result.results);
      })
      .catch(handleFailure)
      .finally(() => {
        setRunning(false);
      });
  }, [problem.slug, language, code, focusFirstFailure, handleFailure]);

  const handleSubmit = useCallback(() => {
    setSubmitting(true);
    setError(null);
    setTab('result');

    executionApi
      .submit(problem.slug, { language, sourceCode: code })
      .then((result) => {
        setOutcome({ kind: 'submit', result });
        focusFirstFailure(result.results);
        onSubmitted();

        if (result.status === 'ACCEPTED') {
          setCelebration(result.firstAccepted ? t('solve.firstAccepted') : t('solve.acceptedAgain'));
        }
      })
      .catch(handleFailure)
      .finally(() => {
        setSubmitting(false);
      });
  }, [problem.slug, language, code, focusFirstFailure, handleFailure, onSubmitted, t]);

  const handleLanguageChange = useCallback((next: Language) => {
    setLanguage(next);
    // The previous verdict described code in another language, so keeping it on
    // screen would attribute it to what is now in the editor.
    setOutcome(null);
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
        onSubmit={handleSubmit}
        running={running}
        submitting={submitting}
        canRun={code.trim().length > 0}
        submissionsClosed={problem.archived || !problem.published}
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
        outcome={outcome}
        busy={running || submitting}
        busyLabel={submitting ? t('solve.submitting') : t('solve.running')}
        error={error}
        tab={tab}
        onTabChange={setTab}
        selectedCase={selectedCase}
        onSelectCase={setSelectedCase}
      />

      <Snackbar
        open={celebration !== null}
        autoHideDuration={5000}
        onClose={() => {
          setCelebration(null);
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled">
          {celebration}
        </Alert>
      </Snackbar>
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
export const EditorPanel = ({ problem, onSubmitted }: { problem: ProblemDetail; onSubmitted: () => void }) => {
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

  return (
    <EditorWorkspace
      problem={problem}
      languages={languages}
      initialLanguage={initialLanguage}
      onSubmitted={onSubmitted}
    />
  );
};
