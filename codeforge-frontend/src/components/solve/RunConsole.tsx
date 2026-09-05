import { Alert, Box, Chip, CircularProgress, Stack, Tab, Tabs, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type CaseResult, type RunResult, type TestCase } from '../../api/types';
import { OutputBlock } from './OutputBlock';
import { VerdictChip } from './VerdictChip';
import { VERDICT_TOKEN } from './verdict';

export type ConsoleTab = 'testcase' | 'result';

type RunConsoleProps = {
  sampleTestCases: TestCase[];
  result: RunResult | null;
  running: boolean;
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
        const status = results?.[index]?.status;

        return (
          <Chip
            key={index}
            size="small"
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
                  }
                : undefined
            }
          />
        );
      })}
    </Stack>
  );
};

const RunSummary = ({ result }: { result: RunResult }) => {
  const { t } = useTranslation();

  return (
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
  );
};

const CaseDetail = ({ result }: { result: CaseResult }) => {
  const { t } = useTranslation();
  const failed = result.status !== 'ACCEPTED';

  // stdout is only worth its own block when it holds more than the answer line —
  // otherwise it repeats what "Output" already says.
  const extraOutput = result.stdout.trim() !== result.actualOutput.trim() ? result.stdout : '';

  return (
    <Stack spacing={1.5}>
      <OutputBlock label={t('solve.input')} value={result.input} />
      <OutputBlock
        label={t('solve.output')}
        value={result.actualOutput}
        tone={failed ? 'error' : 'default'}
        placeholder={t('solve.noOutput')}
      />
      <OutputBlock label={t('solve.expected')} value={result.expectedOutput} />
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
  result,
  running,
  error,
  tab,
  onTabChange,
  selectedCase,
  onSelectCase,
}: RunConsoleProps) => {
  const { t } = useTranslation();
  const testCase = sampleTestCases[selectedCase];
  const caseResult = result?.results[selectedCase];

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
        <Tab value="result" label={t('solve.result')} sx={{ minHeight: 36 }} disabled={!result && !running} />
      </Tabs>

      <Box sx={{ p: 1.75, overflow: 'auto', flex: 1 }}>
        {error ? (
          <Alert severity="error" sx={{ mb: 1.5 }}>
            {error}
          </Alert>
        ) : null}

        {running ? (
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', color: 'text.secondary' }}>
            <CircularProgress size={18} />
            <Typography variant="body2">{t('solve.running')}</Typography>
          </Stack>
        ) : sampleTestCases.length === 0 ? (
          <Typography variant="body2" sx={{ color: 'text.disabled' }}>
            {t('solve.noSampleCases')}
          </Typography>
        ) : tab === 'result' && result ? (
          <Stack spacing={1.75}>
            <RunSummary result={result} />
            {result.compileOutput ? (
              // A compile failure means nothing ran, so the per-case blocks below
              // would all be empty; the compiler's message is the whole story.
              <OutputBlock label={t('solve.compileError')} value={result.compileOutput} tone="error" />
            ) : (
              <>
                <CaseChips
                  count={result.results.length}
                  selected={selectedCase}
                  onSelect={onSelectCase}
                  results={result.results}
                />
                {caseResult ? <CaseDetail result={caseResult} /> : null}
              </>
            )}
          </Stack>
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
