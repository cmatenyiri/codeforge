import AddRounded from '@mui/icons-material/AddRounded';
import { Button, Stack, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { FormSection } from './FormSection';
import { RepeatableRow } from './RepeatableRow';
import { moveRow, newKey, type ExampleRow, type ProblemFormState } from './problem-form';

type ProblemExamplesSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
};

/**
 * The worked examples printed in the statement.
 *
 * <p>Prose, not data: these are what a reader is shown, and they are deliberately
 * not the same thing as the sample test cases the judge runs — an example may be
 * written as `nums = [2,7,11,15], target = 9` while the case behind it is two
 * bare lines. Keeping them apart is what lets a statement read well without
 * constraining the harness's input format.
 */
export const ProblemExamplesSection = ({ form, onChange, errorOf }: ProblemExamplesSectionProps) => {
  const { t } = useTranslation();

  const update = (index: number, patch: Partial<ExampleRow>) => {
    onChange({
      examples: form.examples.map((example, position) => (position === index ? { ...example, ...patch } : example)),
    });
  };

  return (
    <FormSection
      title={t('admin.form.examples')}
      description={t('admin.form.examplesHelp')}
      action={
        <Button
          variant="soft"
          startIcon={<AddRounded />}
          onClick={() => {
            onChange({
              examples: [...form.examples, { key: newKey(), id: null, input: '', output: '', explanation: '' }],
            });
          }}
        >
          {t('admin.form.addExample')}
        </Button>
      }
    >
      {form.examples.length === 0 ? (
        <Typography variant="body2" sx={{ color: 'text.disabled' }}>
          {t('admin.form.noExamples')}
        </Typography>
      ) : null}

      {form.examples.map((example, index) => (
        <RepeatableRow
          key={example.key}
          label={t('solve.example', { index: index + 1 })}
          index={index}
          count={form.examples.length}
          onMove={(delta) => {
            onChange({ examples: moveRow(form.examples, index, delta) });
          }}
          onRemove={() => {
            onChange({ examples: form.examples.filter((_, position) => position !== index) });
          }}
        >
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
              <TextField
                label={t('solve.input')}
                value={example.input}
                onChange={(event) => {
                  update(index, { input: event.target.value });
                }}
                error={Boolean(errorOf(`examples[${index}].input`))}
                helperText={errorOf(`examples[${index}].input`) ?? ' '}
                multiline
                minRows={2}
                fullWidth
                slotProps={{ input: { sx: { typography: 'code' } } }}
              />
              <TextField
                label={t('solve.output')}
                value={example.output}
                onChange={(event) => {
                  update(index, { output: event.target.value });
                }}
                error={Boolean(errorOf(`examples[${index}].output`))}
                helperText={errorOf(`examples[${index}].output`) ?? ' '}
                multiline
                minRows={2}
                fullWidth
                slotProps={{ input: { sx: { typography: 'code' } } }}
              />
            </Stack>

            <TextField
              label={t('solve.explanation')}
              value={example.explanation}
              onChange={(event) => {
                update(index, { explanation: event.target.value });
              }}
              helperText={t('admin.form.explanationHelp')}
              multiline
              minRows={2}
              fullWidth
            />
          </Stack>
        </RepeatableRow>
      ))}
    </FormSection>
  );
};
