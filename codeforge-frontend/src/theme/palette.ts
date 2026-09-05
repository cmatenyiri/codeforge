import { type PaletteOptions } from '@mui/material/styles';
import { dark, light } from './tokens';

type Scheme = typeof dark | typeof light;

/**
 * Builds a full MUI palette from one of the raw token sets, so the dark and
 * light schemes stay structurally identical and can never drift apart.
 */
const buildPalette = (t: Scheme, mode: 'dark' | 'light'): PaletteOptions => ({
  mode,

  primary: {
    light: t.brand.violetLight,
    main: t.brand.violet,
    dark: t.brand.violetDark,
    contrastText: t.accent.primaryContrast,
  },
  secondary: {
    light: t.accent.secondaryLight,
    main: t.accent.secondary,
    dark: t.accent.secondaryDark,
    contrastText: t.accent.secondaryContrast,
  },
  success: {
    light: t.semantic.successLight,
    main: t.semantic.success,
    dark: t.semantic.successDark,
    contrastText: t.surface.canvas,
  },
  warning: {
    light: t.semantic.warningLight,
    main: t.semantic.warning,
    dark: t.semantic.warningDark,
    contrastText: mode === 'dark' ? t.surface.canvas : '#FFFFFF',
  },
  error: {
    light: t.semantic.errorLight,
    main: t.semantic.error,
    dark: t.semantic.errorDark,
    contrastText: mode === 'dark' ? t.surface.canvas : '#FFFFFF',
  },
  info: {
    light: t.semantic.infoLight,
    main: t.semantic.info,
    dark: t.semantic.infoDark,
    contrastText: mode === 'dark' ? t.surface.canvas : '#FFFFFF',
  },

  background: {
    default: t.surface.canvas,
    paper: t.surface.paper,
  },
  text: {
    primary: t.text.primary,
    secondary: t.text.secondary,
    disabled: t.text.disabled,
  },
  divider: t.border.default,

  action: {
    active: t.text.secondary,
    hover: t.surface.hover,
    hoverOpacity: 0.06,
    selected: t.surface.selected,
    selectedOpacity: 0.14,
    disabled: t.text.disabled,
    disabledBackground: t.surface.hover,
    disabledOpacity: 0.4,
    focus: t.brand.ring,
    focusOpacity: 0.2,
  },

  // ── CodeForge-specific tokens ──────────────────────────────────────────
  brand: { ...t.brand },
  surface: { ...t.surface },
  border: { ...t.border },
  difficulty: { ...t.difficulty },
  verdict: { ...t.verdict },
  code: { ...t.code },
});

export const darkPalette = buildPalette(dark, 'dark');
export const lightPalette = buildPalette(light, 'light');
