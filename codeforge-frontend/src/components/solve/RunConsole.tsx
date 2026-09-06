import LockRounded from '@mui/icons-material/LockRounded';
import { Alert, Box, Chip, CircularProgress, Divider, Stack, Tab, Tabs, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type CaseResult, type RunResult, type SubmissionResult, type TestCase } from '../../api/types';
import { OutputBlock } from './OutputBlock';
import { VerdictChip } from './VerdictChip';
import { VERDICT_TOKEN } from './verdict';

export type ConsoleTab = 'testcase' | 'result';

/**
 * What the console is showing.
 *
 * <p>A run and a submission are different enough to be worth distinguishing at
 * the type level: only one of them has hidden cases, a recorded verdict, or the
 * authority to call a problem solved.
 */
export type ConsoleOutcome =
  | { kind: 'run'; result: RunResult }
  | { kind: 'submit'; result: SubmissionResult };

type RunConsoleProps = {
  sampleTestCases: TestCase[];
  outcome: ConsoleOutcome | null;
  busy: boolean;
  busyLabel: string;
  error: string | null;
  tab: ConsoleTab;
  onTabChange: (tab: ConsoleTab) => void;
  selectedCase: number;
  onSelectCase: (index: number) => void;
};

/** Case 1 / Case 2 / … — the selector shared by both tabs. */
const CaseChips = ({
  count,
  selected,
  onSelect,
  results,
}: {
  count: number;
  selected: number;
  onSelect: (index: number) => void;
  results?: CaseResult[];
}) => {
  const { t } = useTranslation();

  return (
    <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
      {Array.from({ length: count }, (_, index) => {
        const result = results?.[index];
        const status = result?.status;

        return (
          <Chip
            key={index}
            size="small"
            icon={result?.hidden ? <LockRounded sx={{ fontSize: 13 }} /> : undefined}
            label={t('solve.case', { index: index + 1 })}
            onClick={() => {
              onSelect(index);
            }}
            variant={selected === index ? 'filled' : 'outlined'}
            sx={
              status
                ? {
                    // Once there are results the chip carries the verdict, so the
                    // failing case is findable without opening each one.
                    color: `verdict.${VERDICT_TOKEN[status]}`,
                    borderColor: `verdict.${VERDICT_TOKEN[status]}`,
                    backgroundColor: selected === index ? `verdict.${VERDICT_TOKEN[status]}Bg` : 'transparent',
                    '& .MuiChip-icon': { color: 'inherit' },
                  }
                : undefined
            }
          />
        );
      })}
    </Stack>
  );
};

const Summary = ({ outcome }: { outcome: ConsoleOutcome }) => {
  const { t } = useTranslation();
  const { result } = outcome;

  return (
    <Stack spacing={1}>
      <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <VerdictChip status={result.status} />
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('solve.casesPassed', { passed: result.passed, total: result.total })}
        </Typography>
        {result.runtimeMs === undefined ? null : (
          <Typography variant="mono" sx={{ color: 'text.disabled' }}>
            {t('solve.runtime', { ms: result.runtimeMs })}
          </Typography>
        )}
        {result.memoryKb === undefined ? null : (
          <Typography variant="mono" sx={{ color: 'text.disabled' }}>
            {t('solve.memory', { mb: (result.memoryKb / 1024).toFixed(1) })}
          </Typography>
        )}
      </Stack>

      {outcome.kind === 'submit' && outcome.result.hiddenTotal > 0 ? (
        <Typography variant="caption" sx={{ color: 'text.disabled' }}>
          {t('solve.hiddenJudged', { hidden: outcome.result.hiddenTotal, total: outcome.result.total })}
        </Typography>
      ) : null}
    </Stack>
  );
};

/**
 * One case's detail. A hidden case has a verdict and nothing else — that is not
 * an omission in the UI but the whole point of judging against cases the solver
 * cannot read.
 */
const CaseDetail = ({ result }: { result: CaseResult }) => {
  const { t } = useTranslation();
  const failed = result.status !== 'ACCEPTED';

  if (result.hidden) {
    return (
      <Stack spacing={1}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <LockRounded sx={{ fontSize: 15, color: 'text.disabled' }} />
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('solve.hiddenCase')}
          </Typography>
        </Stack>
        <Typography variant="caption" sx={{ color: 'text.disabled' }}>
          {failed ? t('solve.hiddenCaseFailed') : t('solve.hiddenCasePassed')}
        </Typography>
      </Stack>
    );
  }

  // stdout is only worth its own block when it holds more than the answer line —
  // otherwise it repeats what "Output" already says.
  const stdout = result.stdout ?? '';
  const actual = result.actualOutput ?? '';
  const extraOutput = stdout.trim() === actual.trim() ? '' : stdout;

  return (
    <Stack spacing={1.5}>
      <OutputBlock label={t('solve.input')} value={result.input ?? ''} />
      <OutputBlock
        label={t('solve.output')}
        value={actual}
        tone={failed ? 'error' : 'default'}
        placeholder={t('solve.noOutput')}
      />
      <OutputBlock label={t('solve.expected')} value={result.expectedOutput ?? ''} />
      {extraOutput ? <OutputBlock label={t('solve.stdout')} value={extraOutput} /> : null}
      {result.stderr ? <OutputBlock label={t('solve.stderr')} value={result.stderr} tone="error" /> : null}
    </Stack>
  );
};

/**
 * The panel below the editor: the sample cases before a run, and their verdicts
 * after one.
 */
export const RunConsole = ({
  sampleTestCases,
  outcome,
  busy,
  busyLabel,
  error,
  tab,
  onTabChange,
  selectedCase,
  onSelectCase,
}: RunConsoleProps) => {
  const { t } = useTranslation();
  const testCase = sampleTestCases[selectedCase];
  const caseResult = outcome?.result.results[selectedCase];

  return (
    <Stack sx={{ borderTop: 1, borderColor: 'border.subtle', height: '38%', minHeight: 180 }}>
      <Tabs
        value={tab}
        onChange={(_, next: ConsoleTab) => {
          onTabChange(next);
        }}
        sx={{ px: 1, minHeight: 36, borderBottom: 1, borderColor: 'border.subtle', flexShrink: 0 }}
      >
        <Tab value="testcase" label={t('solve.testcase')} sx={{ minHeight: 36 }} />
        <Tab value="result" label={t('solve.result')} sx={{ minHeight: 36 }} disabled={!outcome && !busy} />
      </Tabs>

      <Box sx={{ p: 1.75, overflow: 'auto', flex: 1 }}>
        {error ? (
          <Alert severity="error" sx={{ mb: 1.5 }}>
            {error}
          </Alert>
        ) : null}

        {busy ? (
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', color: 'text.secondary' }}>
            <CircularProgress size={18} />
            <Typography variant="body2">{busyLabel}</Typography>
          </Stack>
        ) : tab === 'result' && outcome ? (
          <Stack spacing={1.75}>
            <Summary outcome={outcome} />
            {outcome.result.compileOutput ? (
              // A compile failure means nothing ran, so the per-case blocks below
              // would all be empty; the compiler's message is the whole story.
              <OutputBlock label={t('solve.compileError')} value={outcome.result.compileOutput} tone="error" />
            ) : outcome.result.results.length > 0 ? (
              <>
                <Divider />
                <CaseChips
                  count={outcome.result.results.length}
                  selected={selectedCase}
                  onSelect={onSelectCase}
                  results={outcome.result.results}
                />
                {caseResult ? <CaseDetail result={caseResult} /> : null}
              </>
            ) : null}
          </Stack>
        ) : sampleTestCases.length === 0 ? (
          <Typography variant="body2" sx={{ color: 'text.disabled' }}>
            {t('solve.noSampleCases')}
          </Typography>
        ) : (
          <Stack spacing={1.75}>
            <CaseChips count={sampleTestCases.length} selected={selectedCase} onSelect={onSelectCase} />
            {testCase ? (
              <Stack spacing={1.5}>
                <OutputBlock label={t('solve.input')} value={testCase.input} />
                <OutputBlock label={t('solve.expected')} value={testCase.expectedOutput} />
              </Stack>
            ) : null}
          </Stack>
        )}
      </Box>
    </Stack>
  );
};
