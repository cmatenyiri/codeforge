import { Box, Divider, Stack, Typography } from '@mui/material';
import { type ReactNode } from 'react';

type SectionProps = {
  id: string;
  eyebrow: string;
  title: string;
  description?: string;
  children: ReactNode;
};

/** One labelled block of the design system. */
export const Section = ({ id, eyebrow, title, description, children }: SectionProps) => (
  <Box component="section" id={id} sx={{ scrollMarginTop: 80 }}>
    <Stack spacing={0.5} sx={{ mb: 2.5 }}>
      <Typography variant="overline" sx={{ color: 'primary.main' }}>
        {eyebrow}
      </Typography>
      <Typography variant="h2">{title}</Typography>
      {description ? (
        <Typography variant="body1" sx={{ color: 'text.secondary', maxWidth: 720 }}>
          {description}
        </Typography>
      ) : null}
    </Stack>
    <Stack spacing={3}>{children}</Stack>
    <Divider sx={{ mt: 6 }} />
  </Box>
);

type BlockProps = { label: string; hint?: string; children: ReactNode };

/** A titled sub-group inside a section. */
export const Block = ({ label, hint, children }: BlockProps) => (
  <Stack spacing={1.5}>
    <Stack direction="row" spacing={1} sx={{ alignItems: 'baseline', flexWrap: 'wrap' }}>
      <Typography variant="overline" sx={{ color: 'text.disabled' }}>
        {label}
      </Typography>
      {hint ? (
        <Typography variant="caption" sx={{ color: 'text.disabled' }}>
          {hint}
        </Typography>
      ) : null}
    </Stack>
    {children}
  </Stack>
);

/** Horizontal, wrapping arrangement of specimens. */
export const Row = ({ children }: { children: ReactNode }) => (
  <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
    {children}
  </Stack>
);
