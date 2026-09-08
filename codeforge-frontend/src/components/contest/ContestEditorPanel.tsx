import LockRounded from '@mui/icons-material/LockRounded';
import { Alert, Stack } from '@mui/material';
import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toApiError } from '../../api/api-error';
import { contestsApi } from '../../api/contests-api';
import { type ContestProblemDetail, type Language } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { CodeEditor } from '../solve/CodeEditor';
import { EditorToolbar } from '../solve/EditorToolbar';
import { RunConsole, type ConsoleOutcome, type ConsoleTab } from '../solve/RunConsole';
import { DEFAULT_EDITOR_SETTINGS, type EditorSettings } from '../solve/editor-settings';
import { useCodeDraft } from '../solve/use-code-draft';

/** Preferred default, falling back to whatever the problem does offer. */
const DEFAULT_LANGUAGE: Language = 'JAVA';

type WorkspaceProps = {
  contestSlug: string;
  problem: ContestProblemDetail;
  languages: Language[];
  initialLanguage: Language;
  /** Called after every recorded submission, so the header's score can catch up. */
  onSubmitted: () => void;
};

const ContestWorkspace = ({
  contestSlug,
  problem,
  languages,
  initialLanguage,
  onSubmitted,
}: WorkspaceProps) => {
  const { t } = useTranslation();
  const message = useMessages();
  const [language, setLanguage] = useState<Language>(initialLanguage);

  const starterCode = problem.starterCode[language] ?? '';

  // Keyed by the contest and the position rather than the slug. Before a contest
  // starts nobody is told the slug, and an author renaming the problem afterwards
  // would silently point the key at a different entry — handing a competitor an
  // empty editor mid-contest and orphaning what they had written. A question's
  // place in the contest is the one identifier nothing can move.
  const draftKey = `contest-${contestSlug}-${problem.position}`;
  const { code, setCode, reset } = useCodeDraft(draftKey, language, starterCode);

  const [settings, setSettings] = useState<EditorSettings>(DEFAULT_EDITOR_SETTINGS);
  const [outcome, setOutcome] = useState<ConsoleOutcome | null>(null);
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<ConsoleTab>('testcase');
  const [selectedCase, setSelectedCase] = useState(0);

  /** Land on the first failure: that is the case worth reading. */
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

    contestsApi
      .run(contestSlug, problem.position, { language, sourceCode: code })
      .then((result) => {
        setOutcome({ kind: 'run', result });
        focusFirstFailure(result.results);
      })
      .catch(handleFailure)
      .finally(() => {
        setRunning(false);
      });
  }, [contestSlug, problem.position, language, code, focusFirstFailure, handleFailure]);

  const handleSubmit = useCallback(() => {
    setSubmitting(true);
    setError(null);
    setTab('result');

    contestsApi
      .submit(contestSlug, problem.position, { language, sourceCode: code })
      .then((result) => {
        setOutcome({ kind: 'submit', result });
        focusFirstFailure(result.results);
        onSubmitted();
      })
      .catch(handleFailure)
      .finally(() => {
        setSubmitting(false);
      });
  }, [contestSlug, problem.position, language, code, focusFirstFailure, handleFailure, onSubmitted]);

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
        readOnly={false}
      />

      {/* The editor stays live after the buzzer — practising a finished contest
          is the most useful thing it leaves behind — so this says plainly that
          the attempt is real but will not move the standings. */}
      {problem.counted ? null : (
        <Alert severity="info" square icon={<LockRounded fontSize="small" />} sx={{ borderRadius: 0 }}>
          {t('contest.practiceBody')}
        </Alert>
      )}

      <CodeEditor
        path={draftKey}
        value={code}
        language={language}
        settings={settings}
        onChange={setCode}
        onRun={handleRun}
        readOnly={false}
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
    </Stack>
  );
};

/**
 * The right-hand half of the contest workspace.
 *
 * <p>The same editor, toolbar and console as the practice page. A contest that
 * felt like a different tool would be testing the tool; only where the code is
 * sent differs — the contest endpoints judge against the frozen copy, re-check
 * the clock, and attribute the verdict to the scoreboard.
 */
export const ContestEditorPanel = ({
  contestSlug,
  problem,
  onSubmitted,
}: {
  contestSlug: string;
  problem: ContestProblemDetail;
  onSubmitted: () => void;
}) => {
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
    <ContestWorkspace
      key={problem.position}
      contestSlug={contestSlug}
      problem={problem}
      languages={languages}
      initialLanguage={initialLanguage}
      onSubmitted={onSubmitted}
    />
  );
};
