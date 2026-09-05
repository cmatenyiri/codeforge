import { createTheme } from '@mui/material/styles';
import { components } from './components';
import { darkPalette, lightPalette } from './palette';
import { shadows } from './shadows';
import { radius, spacingUnit } from './tokens';
import { typography } from './typography';

/**
 * The CodeForge theme.
 *
 * Dark-first: the dark scheme is the designed one and is the default; light is a
 * faithful translation for people who work in a bright room. Both are emitted as
 * CSS variables so switching is a class swap, with no React re-render cascade.
 */
export const codeForgeTheme = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  defaultColorScheme: 'dark',
  colorSchemes: {
    dark: { palette: darkPalette },
    light: { palette: lightPalette },
  },

  spacing: spacingUnit,
  shape: { borderRadius: radius.md },
  typography,
  shadows,
  components,

  motion: { reducedMotion: 'system' },
});

export { radius, fontFamilyMono, fontFamilySans } from './tokens';
