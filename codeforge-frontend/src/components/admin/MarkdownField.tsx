import { Box, Tab, Tabs, TextField } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MarkdownBody } from '../solve/MarkdownBody';

type MarkdownFieldProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  helperText?: string;
  rows?: number;
  placeholder?: string;
};

/**
 * A Markdown field with a preview.
 *
 * <p>The preview uses the very component the solving page renders with, so what
 * an author checks here is what a solver will see — a second, approximate
 * renderer would be worse than none.
 */
export const MarkdownField = ({
  label,
  value,
  onChange,
  error,
  helperText,
  rows = 10,
  placeholder,
}: MarkdownFieldProps) => {
  const { t } = useTranslation();
  const [preview, setPreview] = useState(false);

  return (
    <Box>
      <Tabs
        value={preview ? 1 : 0}
        onChange={(_, next: number) => {
          setPreview(next === 1);
        }}
        sx={{ mb: 1, minHeight: 36 }}
      >
        <Tab label={label} sx={{ minHeight: 36 }} />
        <Tab label={t('admin.form.preview')} sx={{ minHeight: 36 }} />
      </Tabs>

      {preview ? (
        <Box
          sx={{
            border: 1,
            borderColor: 'border.subtle',
            borderRadius: 1.5,
            p: 2,
            minHeight: rows * 23,
            backgroundColor: 'surface.sunken',
          }}
        >
          {value.trim() === '' ? null : <MarkdownBody>{value}</MarkdownBody>}
        </Box>
      ) : (
        <TextField
          multiline
          minRows={rows}
          fullWidth
          value={value}
          placeholder={placeholder}
          onChange={(event) => {
            onChange(event.target.value);
          }}
          error={Boolean(error)}
          helperText={error ?? helperText ?? ' '}
          // The field's name is on the tab above it rather than on a <label>, so
          // it is spelled out here — otherwise a screen reader meets an unnamed
          // text box, and so does anything else driving the page by name.
          slotProps={{
            input: { sx: { typography: 'code', alignItems: 'flex-start' } },
            htmlInput: { 'aria-label': label },
          }}
        />
      )}
    </Box>
  );
};
