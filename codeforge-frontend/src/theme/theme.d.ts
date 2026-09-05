/* eslint-disable @typescript-eslint/consistent-type-definitions --
   MUI theme customization is done through declaration merging, which only
   works with `interface`. */
import { type dark } from './tokens';

/** Every value in a raw token group is a CSS color string once it reaches the theme. */
type TokenGroup<T> = { -readonly [K in keyof T]: string };

type BrandTokens = TokenGroup<typeof dark.brand>;
type SurfaceTokens = TokenGroup<typeof dark.surface>;
type BorderTokens = TokenGroup<typeof dark.border>;
type DifficultyTokens = TokenGroup<typeof dark.difficulty>;
type VerdictTokens = TokenGroup<typeof dark.verdict>;
type CodeTokens = TokenGroup<typeof dark.code>;

declare module '@mui/material/styles' {
  /** Opt in to CSS theme variables, so `theme.vars` is typed as always present. */
  interface CssThemeVariables {
    enabled: true;
  }

  interface Palette {
    /** Identity colors: the violet accent and the molten "forge" ember. */
    brand: BrandTokens;
    /** Layered background surfaces, from the app canvas down to sunken insets. */
    surface: SurfaceTokens;
    /** Three-step border ramp — most CodeForge chrome is drawn with borders, not shadows. */
    border: BorderTokens;
    /** Problem difficulty, foreground + tinted background pairs. */
    difficulty: DifficultyTokens;
    /** Judge verdicts, foreground + tinted background pairs. */
    verdict: VerdictTokens;
    /** Syntax highlighting tokens for embedded source code. */
    code: CodeTokens;
  }

  interface PaletteOptions {
    brand?: BrandTokens;
    surface?: SurfaceTokens;
    border?: BorderTokens;
    difficulty?: DifficultyTokens;
    verdict?: VerdictTokens;
    code?: CodeTokens;
  }

  interface TypographyVariants {
    /** Inline or block source code. */
    code: React.CSSProperties;
    /** Monospaced UI text: tags, IDs, runtimes, timers. */
    mono: React.CSSProperties;
    /** Big tabular numbers on stat tiles. */
    metric: React.CSSProperties;
  }

  interface TypographyVariantsOptions {
    code?: React.CSSProperties;
    mono?: React.CSSProperties;
    metric?: React.CSSProperties;
  }
}

declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    code: true;
    mono: true;
    metric: true;
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsVariantOverrides {
    /** Tinted, borderless button — the workhorse for secondary toolbar actions. */
    soft: true;
  }
}

declare module '@mui/material/Paper' {
  interface PaperPropsVariantOverrides {
    /** Recessed panel used for code, console output and inputs. */
    sunken: true;
  }
}
