import AddRounded from '@mui/icons-material/AddRounded';
import { Button, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { FormSection } from './FormSection';
import { RepeatableRow } from './RepeatableRow';
import { moveRow, newKey, type ProblemFormState } from './problem-form';

type ProblemHintsSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
};

/**
 * Hints, in the order they are revealed.
 *
 * <p>Order is the whole design of a hint list: they are revealed one at a time
 * and each one costs the reader something — a mock interview counts them against
 * the round — so the first should nudge and the last should give the approach
 * away. That is why these rows can be reordered rather than only added.
 */
export const ProblemHintsSection = ({ form, onChange, errorOf }: ProblemHintsSectionProps) => {
  const { t } = useTranslation();

  return (
    <FormSection
      title={t('admin.form.hints')}
      description={t('admin.form.hintsHelp')}
      action={
        <Button
          variant="soft"
          startIcon={<AddRounded />}
          onClick={() => {
            onChange({ hints: [...form.hints, { key: newKey(), content: '' }] });
          }}
        >
          {t('admin.form.addHint')}
        </Button>
      }
    >
      {form.hints.length === 0 ? (
        <Typography variant="body2" sx={{ color: 'text.disabled' }}>
          {t('admin.form.noHints')}
        </Typography>
      ) : null}

      {form.hints.map((hint, index) => (
        <RepeatableRow
          key={hint.key}
          label={t('solve.hint', { index: index + 1 })}
          index={index}
          count={form.hints.length}
          onMove={(delta) => {
            onChange({ hints: moveRow(form.hints, index, delta) });
          }}
          onRemove={() => {
            onChange({ hints: form.hints.filter((_, position) => position !== index) });
          }}
        >
          <TextField
            value={hint.content}
            onChange={(event) => {
              onChange({
                hints: form.hints.map((row, position) =>
                  position === index ? { ...row, content: event.target.value } : row,
                ),
              });
            }}
            error={Boolean(errorOf(`hints[${index}]`))}
            helperText={errorOf(`hints[${index}]`) ?? ' '}
            multiline
            minRows={2}
            fullWidth
          />
        </RepeatableRow>
      ))}
    </FormSection>
  );
};
