import { Box, FormControlLabel, Stack, Switch, Tab, Tabs, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type Language } from '../../api/types';
import { CodeEditor } from '../solve/CodeEditor';
import { DEFAULT_EDITOR_SETTINGS } from '../solve/editor-settings';
import { LANGUAGE_LABEL } from '../solve/verdict';
import { FormSection } from './FormSection';
import { MarkdownField } from './MarkdownField';
import { LANGUAGES, emptyEditorial, type ProblemFormState } from './problem-form';

type ProblemEditorialSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
};

/**
 * The written solution.
 *
 * <p>Optional, and off by default: a problem with no editorial is a normal
 * problem, and the reader's tab is disabled rather than opening onto an apology.
 *
 * <p>All four languages get a tab, but only the ones actually written are sent —
 * an empty tab is not an authored solution, and the reader's language picker
 * offers exactly what exists.
 */
export const ProblemEditorialSection = ({ form, onChange, errorOf }: ProblemEditorialSectionProps) => {
  const { t } = useTranslation();
  const [language, setLanguage] = useState<Language>('JAVA');

  const written = LANGUAGES.filter((candidate) => (form.editorial.solutions[candidate] ?? '').trim() !== '');

  return (
    <FormSection
      title={t('admin.form.editorial')}
      description={t('admin.form.editorialHelp')}
      action={
        <FormControlLabel
          control={
            <Switch
              checked={form.hasEditorial}
              onChange={(event) => {
                onChange({
                  hasEditorial: event.target.checked,
                  // Cleared on the way out rather than kept in limbo: switching
                  // it off means the editorial is deleted on save, and leaving
                  // the old text behind the switch would make that surprising.
                  editorial: event.target.checked ? form.editorial : emptyEditorial(),
                });
              }}
            />
          }
          label={t('admin.form.hasEditorial')}
        />
      }
    >
      {!form.hasEditorial ? (
        <Typography variant="body2" sx={{ color: 'text.disabled' }}>
          {t('admin.form.noEditorial')}
        </Typography>
      ) : (
        <>
          <MarkdownField
            label={t('admin.form.walkthrough')}
            value={form.editorial.contentMarkdown}
            onChange={(contentMarkdown) => {
              onChange({ editorial: { ...form.editorial, contentMarkdown } });
            }}
            error={errorOf('editorial.contentMarkdown')}
            placeholder={t('admin.form.walkthroughPlaceholder')}
            rows={10}
          />

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label={t('solve.time')}
              value={form.editorial.timeComplexity}
              onChange={(event) => {
                onChange({ editorial: { ...form.editorial, timeComplexity: event.target.value } });
              }}
              error={Boolean(errorOf('editorial.timeComplexity'))}
              helperText={errorOf('editorial.timeComplexity') ?? t('admin.form.complexityHelp')}
              sx={{ flex: 1 }}
              slotProps={{ input: { sx: { typography: 'mono' } } }}
            />
            <TextField
              label={t('solve.space')}
              value={form.editorial.spaceComplexity}
              onChange={(event) => {
                onChange({ editorial: { ...form.editorial, spaceComplexity: event.target.value } });
              }}
              error={Boolean(errorOf('editorial.spaceComplexity'))}
              helperText={errorOf('editorial.spaceComplexity') ?? ' '}
              sx={{ flex: 1 }}
              slotProps={{ input: { sx: { typography: 'mono' } } }}
            />
          </Stack>

          <Box>
            <Tabs
              value={language}
              onChange={(_, next: Language) => {
                setLanguage(next);
              }}
              sx={{ mb: 1.5 }}
            >
              {LANGUAGES.map((candidate) => (
                <Tab
                  key={candidate}
                  value={candidate}
                  label={written.includes(candidate) ? `${LANGUAGE_LABEL[candidate]} ●` : LANGUAGE_LABEL[candidate]}
                />
              ))}
            </Tabs>

            <Box
              sx={{
                height: 340,
                display: 'flex',
                border: 1,
                borderColor: 'border.subtle',
                borderRadius: 1.5,
                overflow: 'hidden',
              }}
            >
              <CodeEditor
                value={form.editorial.solutions[language] ?? ''}
                language={language}
                settings={DEFAULT_EDITOR_SETTINGS}
                onChange={(source) => {
                  onChange({
                    editorial: {
                      ...form.editorial,
                      solutions: { ...form.editorial.solutions, [language]: source },
                    },
                  });
                }}
                onRun={() => {
                  // Nothing to run: this buffer is reference prose, not a submission.
                }}
                path="admin-editorial"
              />
            </Box>

            {errorOf(`editorial.solutions.${language}`) ? (
              <Typography variant="caption" sx={{ color: 'error.main', mt: 0.5, display: 'block' }}>
                {errorOf(`editorial.solutions.${language}`)}
              </Typography>
            ) : null}
          </Box>
        </>
      )}
    </FormSection>
  );
};
