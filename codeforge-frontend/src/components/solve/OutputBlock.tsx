import { Box, Paper, Typography } from '@mui/material';

type OutputBlockProps = {
  label: string;
  value: string;
  /** Tints the text, for an output that did not match or a program that crashed. */
  tone?: 'default' | 'error';
  /** Shown in place of the value when there is nothing to show. */
  placeholder?: string;
};

/**
 * One labelled, monospaced block of console text.
 *
 * <p>`pre-wrap` rather than `pre`: judge output is arbitrary and a long line
 * should wrap inside the panel instead of pushing a horizontal scrollbar onto
 * the whole console.
 */
export const OutputBlock = ({ label, value, tone = 'default', placeholder }: OutputBlockProps) => (
  <Box>
    <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 0.5 }}>
      {label}
    </Typography>
    <Paper variant="sunken" sx={{ px: 1.25, py: 1 }}>
      <Typography
        variant="code"
        sx={{
          display: 'block',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          color: value ? (tone === 'error' ? 'verdict.runtimeError' : 'text.primary') : 'text.disabled',
        }}
      >
        {value || placeholder}
      </Typography>
    </Paper>
  </Box>
);
