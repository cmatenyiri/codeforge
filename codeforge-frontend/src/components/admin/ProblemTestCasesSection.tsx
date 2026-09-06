import AddRounded from '@mui/icons-material/AddRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControlLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi } from '../../api/admin-api';
import { toApiError } from '../../api/api-error';
import { type CaseResult, type DataType, type Language, type RunResult } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { CodeEditor } from '../solve/CodeEditor';
import { OutputBlock } from '../solve/OutputBlock';
import { VerdictChip } from '../solve/VerdictChip';
import { DEFAULT_EDITOR_SETTINGS } from '../solve/editor-settings';
import { LANGUAGE_LABEL } from '../solve/verdict';
import { FormSection } from './FormSection';
import { RepeatableRow } from './RepeatableRow';
import { LANGUAGES, hasSignature, moveRow, newKey, type ProblemFormState, type TestCaseRow } from './problem-form';

type ProblemTestCasesSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
  /** Null while the problem has never been saved — there is nothing stored to judge against. */
  problemId: number | null;
  /** True when the form holds unsaved edits, which the stored cases do not have. */
  dirty: boolean;
};

/**
 * The cases a submission is judged against, and the check that they are right.
 *
 * <p>Two kinds of case, and the difference is the whole point of having both:
 * samples are shown to the solver and are what "Run" judges, hidden ones are the
 * bar a submission actually has to clear. A solution that special-cases the two
 * examples fails here and nowhere else.
 *
 * <p>The verification panel is what makes hand-written expected outputs
 * trustworthy. It runs a reference solution against every stored case and shows
 * what each one actually printed — so a mismatch is settled before a solver ever
 * meets it, and a correct answer that the author mistyped can be adopted with one
 * click rather than retyped.
 */
export const ProblemTestCasesSection = ({
  form,
  onChange,
  errorOf,
  problemId,
  dirty,
}: ProblemTestCasesSectionProps) => {
  const { t } = useTranslation();
  const message = useMessages();

  const [language, setLanguage] = useState<Language>('PYTHON');
  const [sourceCode, setSourceCode] = useState('');
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<RunResult | null>(null);
  const [runError, setRunError] = useState<string | null>(null);

  const signed = hasSignature(form);
  const samples = form.testCases.filter((testCase) => !testCase.hidden).length;

  const update = (index: number, patch: Partial<TestCaseRow>) => {
    onChange({
      testCases: form.testCases.map((testCase, position) =>
        position === index ? { ...testCase, ...patch } : testCase,
      ),
    });
  };

  const loadStub = async () => {
    setRunError(null);
    try {
      const stubs = await adminApi.starterCode({
        functionName: form.functionName.trim(),
        returnType: form.returnType as DataType,
        parameters: form.parameters.map((parameter) => ({ name: parameter.name.trim(), type: parameter.type })),
      });
      setSourceCode(stubs[language] ?? '');
    } catch (caught) {
      const apiError = toApiError(caught);
      setRunError(message(apiError.code, apiError.message));
    }
  };

  const run = async () => {
    if (problemId === null) {
      return;
    }

    setRunning(true);
    setRunError(null);
    try {
      setResult(await adminApi.validate(problemId, { language, sourceCode }));
    } catch (caught) {
      const apiError = toApiError(caught);
      setResult(null);
      setRunError(message(apiError.code, apiError.message));
    } finally {
      setRunning(false);
    }
  };

  /** Results carry the stored case's id, which is what pairs them with a row here. */
  const resultFor = (testCase: TestCaseRow): CaseResult | undefined =>
    testCase.id === null ? undefined : result?.results.find((judged) => judged.testCaseId === testCase.id);

  const adopt = (index: number, actual: string) => {
    update(index, { expectedOutput: actual });
  };

  const mismatches = result
    ? form.testCases.filter((testCase) => {
        const judged = resultFor(testCase);
        return judged !== undefined && judged.status === 'WRONG_ANSWER' && judged.actualOutput !== undefined;
      })
    : [];

  const adoptAll = () => {
    onChange({
      testCases: form.testCases.map((testCase) => {
        const judged = resultFor(testCase);
        return judged?.status === 'WRONG_ANSWER' && judged.actualOutput !== undefined
          ? { ...testCase, expectedOutput: judged.actualOutput }
          : testCase;
      }),
    });
  };

  return (
    <FormSection
      title={t('admin.form.testCases')}
      description={t('admin.form.testCasesHelp')}
      action={
        <Button
          variant="soft"
          startIcon={<AddRounded />}
          onClick={() => {
            onChange({
              testCases: [
                ...form.testCases,
                // New cases default to hidden: the samples are chosen
                // deliberately, and everything else is the bar.
                { key: newKey(), id: null, input: '', expectedOutput: '', hidden: form.testCases.length > 0 },
              ],
            });
          }}
        >
          {t('admin.form.addTestCase')}
        </Button>
      }
    >
      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
        <Chip label={t('admin.form.sampleCount', { count: samples })} />
        <Chip label={t('admin.form.hiddenCount', { count: form.testCases.length - samples })} />
      </Stack>

      {signed ? (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('admin.form.inputShape', {
            count: form.parameters.length,
            names: form.parameters.map((parameter) => parameter.name || '?').join(', '),
          })}
        </Typography>
      ) : (
        <Alert severity="info">{t('admin.form.testCasesNoSignature')}</Alert>
      )}

      {form.testCases.map((testCase, index) => {
        const judged = resultFor(testCase);

        return (
          <RepeatableRow
            key={testCase.key}
            label={t('solve.case', { index: index + 1 })}
            badge={
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                {judged ? <VerdictChip status={judged.status} size="small" /> : null}
                <FormControlLabel
                  control={
                    <Switch
                      size="small"
                      checked={testCase.hidden}
                      onChange={(event) => {
                        update(index, { hidden: event.target.checked });
                      }}
                    />
                  }
                  label={<Typography variant="caption">{t('admin.form.hidden')}</Typography>}
                  sx={{ ml: 0.5, mr: 0 }}
                />
              </Stack>
            }
            index={index}
            count={form.testCases.length}
            onMove={(delta) => {
              onChange({ testCases: moveRow(form.testCases, index, delta) });
            }}
            onRemove={() => {
              onChange({ testCases: form.testCases.filter((_, position) => position !== index) });
            }}
          >
            <Stack spacing={2}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  label={t('admin.form.stdin')}
                  value={testCase.input}
                  onChange={(event) => {
                    update(index, { input: event.target.value });
                  }}
                  error={Boolean(errorOf(`testCases[${index}].input`))}
                  helperText={errorOf(`testCases[${index}].input`) ?? ' '}
                  multiline
                  minRows={3}
                  fullWidth
                  slotProps={{ input: { sx: { typography: 'code' } } }}
                />
                <TextField
                  label={t('solve.expected')}
                  value={testCase.expectedOutput}
                  onChange={(event) => {
                    update(index, { expectedOutput: event.target.value });
                  }}
                  error={Boolean(errorOf(`testCases[${index}].expectedOutput`))}
                  helperText={errorOf(`testCases[${index}].expectedOutput`) ?? ' '}
                  multiline
                  minRows={3}
                  fullWidth
                  slotProps={{ input: { sx: { typography: 'code' } } }}
                />
              </Stack>

              {judged && judged.status !== 'ACCEPTED' ? (
                <Stack spacing={1.5}>
                  <OutputBlock
                    label={t('admin.form.actualOutput')}
                    value={judged.actualOutput ?? ''}
                    tone="error"
                    placeholder={t('solve.noOutput')}
                  />
                  {judged.stderr ? <OutputBlock label={t('solve.stderr')} value={judged.stderr} tone="error" /> : null}
                  {judged.actualOutput !== undefined && judged.status === 'WRONG_ANSWER' ? (
                    <Box>
                      <Button
                        variant="soft"
                        size="small"
                        onClick={() => {
                          adopt(index, judged.actualOutput ?? '');
                        }}
                      >
                        {t('admin.form.useActual')}
                      </Button>
                    </Box>
                  ) : null}
                </Stack>
              ) : null}
            </Stack>
          </RepeatableRow>
        );
      })}

      <Box sx={{ borderTop: 1, borderColor: 'border.subtle', pt: 2.5 }}>
        <Typography variant="h5">{t('admin.form.verify')}</Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5, mb: 2 }}>
          {t('admin.form.verifyHelp')}
        </Typography>

        {problemId === null ? <Alert severity="info">{t('admin.form.verifyUnsaved')}</Alert> : null}
        {problemId !== null && dirty ? <Alert severity="warning">{t('admin.form.verifyDirty')}</Alert> : null}

        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', my: 2, flexWrap: 'wrap' }} useFlexGap>
          <TextField
            select
            size="small"
            label={t('solve.programmingLanguage')}
            value={language}
            onChange={(event) => {
              setLanguage(event.target.value as Language);
            }}
            sx={{ minWidth: 170 }}
          >
            {LANGUAGES.map((candidate) => (
              <MenuItem key={candidate} value={candidate}>
                {LANGUAGE_LABEL[candidate]}
              </MenuItem>
            ))}
          </TextField>

          <Button
            variant="text"
            disabled={!signed}
            onClick={() => {
              void loadStub();
            }}
          >
            {t('admin.form.loadStub')}
          </Button>

          <Box sx={{ flex: 1 }} />

          <Button
            startIcon={running ? <CircularProgress size={16} color="inherit" /> : <PlayArrowRounded />}
            disabled={problemId === null || running || sourceCode.trim() === ''}
            onClick={() => {
              void run();
            }}
          >
            {running ? t('admin.form.verifying') : t('admin.form.runReference')}
          </Button>
        </Stack>

        <Box
          sx={{
            height: 320,
            display: 'flex',
            border: 1,
            borderColor: 'border.subtle',
            borderRadius: 1.5,
            overflow: 'hidden',
          }}
        >
          <CodeEditor
            value={sourceCode}
            language={language}
            settings={DEFAULT_EDITOR_SETTINGS}
            onChange={setSourceCode}
            onRun={() => {
              void run();
            }}
            path={`admin-reference-${problemId ?? 'new'}`}
          />
        </Box>

        {runError ? (
          <Alert severity="error" sx={{ mt: 2 }}>
            {runError}
          </Alert>
        ) : null}

        {result ? (
          <Stack spacing={1.5} sx={{ mt: 2 }}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap' }} useFlexGap>
              <VerdictChip status={result.status} />
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {t('solve.casesPassed', { passed: result.passed, total: result.total })}
              </Typography>
              {mismatches.length > 0 ? (
                <Button variant="soft" size="small" onClick={adoptAll}>
                  {t('admin.form.useAllActual', { count: mismatches.length })}
                </Button>
              ) : null}
            </Stack>

            {result.compileOutput ? (
              <OutputBlock label={t('solve.compileError')} value={result.compileOutput} tone="error" />
            ) : null}

            {result.status === 'ACCEPTED' ? <Alert severity="success">{t('admin.form.verifyPassed')}</Alert> : null}
          </Stack>
        ) : null}
      </Box>
    </FormSection>
  );
};
