import { Box, Paper, Stack, Typography } from '@mui/material';
import { type ReactNode } from 'react';

type FormSectionProps = {
  title: string;
  description?: string;
  /** Rendered at the top right — a count, a switch, an "add" button. */
  action?: ReactNode;
  children: ReactNode;
};

/**
 * One panel of the authoring form.
 *
 * <p>The form is a long document rather than a wizard: an author fixing a typo
 * in a hint should not have to walk through five steps to reach it, and the
 * sections are all one save.
 */
export const FormSection = ({ title, description, action, children }: FormSectionProps) => (
  <Paper variant="outlined" sx={{ p: 3 }}>
    <Stack spacing={2.5}>
      <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="h4">{title}</Typography>
          {description ? (
            <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
              {description}
            </Typography>
          ) : null}
        </Box>
        {action}
      </Stack>
      {children}
    </Stack>
  </Paper>
);
