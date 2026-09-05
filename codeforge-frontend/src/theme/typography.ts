import { type TypographyVariantsOptions } from '@mui/material/styles';
import { fontFamilyMono, fontFamilySans } from './tokens';

/**
 * A dense, technical type scale. 14px base (not MUI's 16px) so tables, chips and
 * side panels stay information-rich, with negative tracking on headings to keep
 * them looking engineered rather than editorial.
 */
export const typography: TypographyVariantsOptions = {
  fontFamily: fontFamilySans,
  fontSize: 14,
  fontWeightLight: 400,
  fontWeightRegular: 450,
  fontWeightMedium: 550,
  fontWeightBold: 650,

  h1: { fontSize: '2.25rem', lineHeight: 1.15, fontWeight: 700, letterSpacing: '-0.03em' },
  h2: { fontSize: '1.75rem', lineHeight: 1.2, fontWeight: 700, letterSpacing: '-0.025em' },
  h3: { fontSize: '1.375rem', lineHeight: 1.25, fontWeight: 650, letterSpacing: '-0.02em' },
  h4: { fontSize: '1.125rem', lineHeight: 1.3, fontWeight: 650, letterSpacing: '-0.015em' },
  h5: { fontSize: '1rem', lineHeight: 1.4, fontWeight: 600, letterSpacing: '-0.01em' },
  h6: { fontSize: '0.875rem', lineHeight: 1.45, fontWeight: 600, letterSpacing: '-0.005em' },

  subtitle1: { fontSize: '0.9375rem', lineHeight: 1.5, fontWeight: 550, letterSpacing: '-0.005em' },
  subtitle2: { fontSize: '0.8125rem', lineHeight: 1.5, fontWeight: 600, letterSpacing: '0.005em' },

  body1: { fontSize: '0.9375rem', lineHeight: 1.65, fontWeight: 450, letterSpacing: '-0.003em' },
  body2: { fontSize: '0.8125rem', lineHeight: 1.6, fontWeight: 450, letterSpacing: 0 },

  button: { fontSize: '0.875rem', lineHeight: 1.4, fontWeight: 600, letterSpacing: '0.005em', textTransform: 'none' },
  caption: { fontSize: '0.75rem', lineHeight: 1.5, fontWeight: 500, letterSpacing: '0.01em' },

  // Section eyebrows: monospaced and widely tracked — reads like a tool's UI label.
  overline: {
    fontFamily: fontFamilyMono,
    fontSize: '0.6875rem',
    lineHeight: 1.5,
    fontWeight: 600,
    letterSpacing: '0.14em',
    textTransform: 'uppercase',
  },

  // ── CodeForge-specific variants ────────────────────────────────────────
  /** Inline or block source code. */
  code: {
    fontFamily: fontFamilyMono,
    fontSize: '0.8125rem',
    lineHeight: 1.7,
    fontWeight: 450,
    letterSpacing: 0,
  },
  /** Monospaced UI text: tags, IDs, runtimes, timers. */
  mono: {
    fontFamily: fontFamilyMono,
    fontSize: '0.8125rem',
    lineHeight: 1.5,
    fontWeight: 500,
    letterSpacing: '0.01em',
    fontVariantNumeric: 'tabular-nums',
  },
  /** Big numbers on stat tiles. */
  metric: {
    fontFamily: fontFamilyMono,
    fontSize: '1.75rem',
    lineHeight: 1.1,
    fontWeight: 600,
    letterSpacing: '-0.02em',
    fontVariantNumeric: 'tabular-nums',
  },
};
