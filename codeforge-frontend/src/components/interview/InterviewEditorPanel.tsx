import LockRounded from '@mui/icons-material/LockRounded';
import { Alert, Stack } from '@mui/material';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toApiError } from '../../api/api-error';
import { interviewsApi } from '../../api/interviews-api';
import { type InterviewProblemDetail, type Language } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { CodeEditor } from '../solve/CodeEditor';
import { EditorToolbar } from '../solve/EditorToolbar';
import { RunConsole, type ConsoleOutcome, type ConsoleTab } from '../solve/RunConsole';
import { DEFAULT_EDITOR_SETTINGS, type EditorSettings } from '../solve/editor-settings';
import { useCodeDraft } from '../solve/use-code-draft';

/** Preferred default, falling back to whatever the problem does offer. */
const DEFAULT_LANGUAGE: Language = 'JAVA';

type WorkspaceProps = {
  interviewId: number;
  problem: InterviewProblemDetail;
  languages: Language[];
  initialLanguage: Language;
  /** Called after every recorded submission, so the header's score can catch up. */
  onSubmitted: () => void;
  /**
   * Raised while the judge has the code.
   *
   * <p>The page holds the buzzer's navigation to the report until it drops, so a
   * submission made in the last seconds is not abandoned half-judged.
   */
  onJudgingChange: (judging: boolean) => void;
  /**
   * False for a problem the round has moved past, and once the clock has run.
   * The server refuses either way; locking the buffer is so the screen does not
   * invite work that cannot be submitted.
   */
  editable: boolean;
};

const InterviewWorkspace = ({
  interviewId,
  problem,
  languages,
  initialLanguage,
  onSubmitted,
  onJudgingChange,
  editable,
}: WorkspaceProps) => {
  const { t } = useTranslation();
  const message = useMessages();
  const [language, setLanguage] = useState<Language>(initialLanguage);

  const starterCode = problem.starterCode[language] ?? '';
  // Keyed by the interview as well as the slot, so a round starts from the stub
  // rather than from whatever was left in the practice editor last week — while
  // a refresh mid-round still gets the candidate's work back.
  //
  // The position rather than the slug: an author renaming the problem changes
  // its slug, and a key built from that would silently point at a different
  // entry, handing the candidate an empty editor and orphaning what they had
  // written. The slot's place in the round is the one identifier nothing outside
  // the round can move.
  const draftKey = `interview-${interviewId}-${problem.position}`;
  const { code: draft, setCode, reset } = useCodeDraft(draftKey, language, starterCode);

  /**
   * A closed problem shows the code the judge saw, not the local draft.
   *
   * <p>The draft keeps taking keystrokes for as long as a submission is being
   * judged — a submit can take minutes — so an acceptance can land and lock the
   * problem over text that was never submitted. The server's copy is the one the
   * verdict was about, and it survives a reload and a change of browser, which a
   * draft in `localStorage` does not.
   */
  const judged = problem.submittedSourceCode;
  const showingJudged = !editable && judged !== undefined;
  const code = showingJudged ? judged : draft;
  const shownLanguage = showingJudged ? (problem.submittedLanguage ?? language) : language;

  const [settings, setSettings] = useState<EditorSettings>(DEFAULT_EDITOR_SETTINGS);
  const [outcome, setOutcome] = useState<ConsoleOutcome | null>(null);
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<ConsoleTab>('testcase');
  const [selectedCase, setSelectedCase] = useState(0);

  useEffect(() => {
    onJudgingChange(running || submitting);
  }, [running, submitting, onJudgingChange]);

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

    interviewsApi
      .run(interviewId, problem.position, { language, sourceCode: code })
      .then((result) => {
        setOutcome({ kind: 'run', result });
        focusFirstFailure(result.results);
      })
      .catch(handleFailure)
      .finally(() => {
        setRunning(false);
      });
  }, [interviewId, problem.position, language, code, focusFirstFailure, handleFailure]);

  const handleSubmit = useCallback(() => {
    setSubmitting(true);
    setError(null);
    setTab('result');

    interviewsApi
      .submit(interviewId, problem.position, { language, sourceCode: code })
      .then((result) => {
        setOutcome({ kind: 'submit', result });
        focusFirstFailure(result.results);
        onSubmitted();
      })
      .catch(handleFailure)
      .finally(() => {
        setSubmitting(false);
      });
  }, [interviewId, problem.position, language, code, focusFirstFailure, handleFailure, onSubmitted]);

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
        language={shownLanguage}
        // Only the language it was submitted in, once that is what is on screen:
        // a picker offering three others implies they would show something.
        languages={showingJudged ? [shownLanguage] : languages}
        onLanguageChange={handleLanguageChange}
        settings={settings}
        onSettingsChange={setSettings}
        onReset={reset}
        onRun={handleRun}
        onSubmit={handleSubmit}
        running={running}
        submitting={submitting}
        canRun={code.trim().length > 0 && editable}
        readOnly={!editable}
      />

      {editable ? null : (
        <Alert severity="info" square icon={<LockRounded fontSize="small" />} sx={{ borderRadius: 0 }}>
          {showingJudged ? t('interview.showingSubmitted') : t('interview.problemClosed')}
        </Alert>
      )}

      <CodeEditor
        // A distinct model for the recorded submission, so it never shares a
        // buffer with the draft it replaced.
        path={showingJudged ? `${draftKey}-submitted` : draftKey}
        value={code}
        language={shownLanguage}
        settings={settings}
        onChange={setCode}
        onRun={handleRun}
        readOnly={!editable}
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
 * The right-hand half of the interview workspace.
 *
 * <p>The same editor, toolbar and console as the practice page — a mock that
 * felt like a different tool would be testing the tool. Only where the code is
 * sent differs: the interview endpoints re-check the clock before judging and
 * attribute the verdict to the slot.
 *
 * <p>No "Accepted — problem solved!" toast either. The verdict belongs in the
 * console, and a round is not over because one problem went in.
 */
export const InterviewEditorPanel = ({
  interviewId,
  problem,
  onSubmitted,
  onJudgingChange,
  editable,
}: {
  interviewId: number;
  problem: InterviewProblemDetail;
  onSubmitted: () => void;
  onJudgingChange: (judging: boolean) => void;
  editable: boolean;
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
    <InterviewWorkspace
      key={problem.slug}
      interviewId={interviewId}
      problem={problem}
      languages={languages}
      initialLanguage={initialLanguage}
      onSubmitted={onSubmitted}
      onJudgingChange={onJudgingChange}
      editable={editable}
    />
  );
};
