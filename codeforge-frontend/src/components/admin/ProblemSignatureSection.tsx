import AddRounded from '@mui/icons-material/AddRounded';
import CodeRounded from '@mui/icons-material/CodeRounded';
import { Alert, Box, Button, MenuItem, Stack, Tab, Tabs, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi } from '../../api/admin-api';
import { toApiError } from '../../api/api-error';
import { type DataType, type Language } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { EditorialCode } from '../solve/EditorialCode';
import { FormSection } from './FormSection';
import { RepeatableRow } from './RepeatableRow';
import { DATA_TYPES, hasSignature, moveRow, newKey, type ProblemFormState } from './problem-form';

type ProblemSignatureSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
};

/**
 * The function a solver implements.
 *
 * <p>One declaration drives three things that would otherwise be written by hand
 * for every (problem, language) pair: the starter code, the harness that reads
 * stdin into the function's arguments, and the canonical printing of its return
 * value. It is also what the test cases are checked against — one input line per
 * argument, in this order — so changing it invalidates every case that was
 * written for the old shape.
 *
 * <p>The preview is the point of the section: an argument order or a return type
 * is far easier to judge from the four stubs it produces than from the form.
 */
export const ProblemSignatureSection = ({ form, onChange, errorOf }: ProblemSignatureSectionProps) => {
  const { t } = useTranslation();
  const message = useMessages();

  const [preview, setPreview] = useState<Partial<Record<Language, string>> | null>(null);
  const [previewLanguage, setPreviewLanguage] = useState<Language | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [previewing, setPreviewing] = useState(false);

  const signed = hasSignature(form);

  const showPreview = async () => {
    setPreviewing(true);
    setPreviewError(null);
    try {
      const stubs = await adminApi.starterCode({
        functionName: form.functionName.trim(),
        returnType: form.returnType as DataType,
        parameters: form.parameters.map((parameter) => ({ name: parameter.name.trim(), type: parameter.type })),
      });
      setPreview(stubs);
      setPreviewLanguage(Object.keys(stubs)[0] as Language | undefined ?? null);
    } catch (caught) {
      const apiError = toApiError(caught);
      setPreviewError(message(apiError.code, apiError.message));
    } finally {
      setPreviewing(false);
    }
  };

  const languages = preview ? (Object.keys(preview) as Language[]) : [];

  return (
    <FormSection
      title={t('admin.form.signature')}
      description={t('admin.form.signatureHelp')}
      action={
        <Button
          variant="soft"
          startIcon={<CodeRounded />}
          disabled={!signed || previewing}
          onClick={() => {
            void showPreview();
          }}
        >
          {t('admin.form.previewStarter')}
        </Button>
      }
    >
      {!signed ? <Alert severity="info">{t('admin.form.noSignature')}</Alert> : null}

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          label={t('admin.form.functionName')}
          value={form.functionName}
          onChange={(event) => {
            onChange({ functionName: event.target.value });
          }}
          error={Boolean(errorOf('functionName'))}
          helperText={errorOf('functionName') ?? t('admin.form.functionNameHelp')}
          sx={{ flex: 1 }}
          slotProps={{ input: { sx: { typography: 'mono' } } }}
        />

        <TextField
          select
          label={t('admin.form.returnType')}
          value={form.returnType}
          onChange={(event) => {
            onChange({ returnType: event.target.value as DataType | '' });
          }}
          error={Boolean(errorOf('returnType'))}
          helperText={errorOf('returnType') ?? ' '}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">{t('admin.form.noReturnType')}</MenuItem>
          {DATA_TYPES.map((type) => (
            <MenuItem key={type} value={type} sx={{ typography: 'mono' }}>
              {type}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      <Box>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mb: 1.5 }}>
          <Typography variant="subtitle2">{t('admin.form.parameters')}</Typography>
          <Box sx={{ flex: 1 }} />
          <Button
            variant="text"
            startIcon={<AddRounded />}
            onClick={() => {
              onChange({ parameters: [...form.parameters, { key: newKey(), name: '', type: 'INT' }] });
            }}
          >
            {t('admin.form.addParameter')}
          </Button>
        </Stack>

        {form.parameters.length === 0 ? (
          <Typography variant="body2" sx={{ color: 'text.disabled' }}>
            {t('admin.form.noParameters')}
          </Typography>
        ) : null}

        <Stack spacing={1.5}>
          {form.parameters.map((parameter, index) => (
            <RepeatableRow
              key={parameter.key}
              label={t('admin.form.argument', { index: index + 1 })}
              index={index}
              count={form.parameters.length}
              onMove={(delta) => {
                onChange({ parameters: moveRow(form.parameters, index, delta) });
              }}
              onRemove={() => {
                onChange({ parameters: form.parameters.filter((_, position) => position !== index) });
              }}
            >
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  label={t('admin.form.argumentName')}
                  value={parameter.name}
                  onChange={(event) => {
                    onChange({
                      parameters: form.parameters.map((row, position) =>
                        position === index ? { ...row, name: event.target.value } : row,
                      ),
                    });
                  }}
                  error={Boolean(errorOf(`parameters[${index}].name`))}
                  helperText={errorOf(`parameters[${index}].name`) ?? ' '}
                  sx={{ flex: 1 }}
                  slotProps={{ input: { sx: { typography: 'mono' } } }}
                />
                <TextField
                  select
                  label={t('admin.form.argumentType')}
                  value={parameter.type}
                  onChange={(event) => {
                    onChange({
                      parameters: form.parameters.map((row, position) =>
                        position === index ? { ...row, type: event.target.value as DataType } : row,
                      ),
                    });
                  }}
                  error={Boolean(errorOf(`parameters[${index}].type`))}
                  helperText={errorOf(`parameters[${index}].type`) ?? ' '}
                  sx={{ minWidth: 200 }}
                >
                  {DATA_TYPES.map((type) => (
                    <MenuItem key={type} value={type} sx={{ typography: 'mono' }}>
                      {type}
                    </MenuItem>
                  ))}
                </TextField>
              </Stack>
            </RepeatableRow>
          ))}
        </Stack>
      </Box>

      {previewError ? <Alert severity="error">{previewError}</Alert> : null}

      {preview && previewLanguage ? (
        <Box>
          <Tabs
            value={previewLanguage}
            onChange={(_, next: Language) => {
              setPreviewLanguage(next);
            }}
            sx={{ mb: 1.5 }}
          >
            {languages.map((language) => (
              <Tab key={language} value={language} label={language} />
            ))}
          </Tabs>
          <EditorialCode code={preview[previewLanguage] ?? ''} language={previewLanguage} />
        </Box>
      ) : null}
    </FormSection>
  );
};
