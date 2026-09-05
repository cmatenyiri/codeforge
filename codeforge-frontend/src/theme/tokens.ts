/**
 * CodeForge raw design tokens.
 *
 * These are the primitive values the theme is built from — never reference them
 * directly from components. Consume them through the MUI theme (`theme.palette.*`,
 * `theme.typography.*`) so that color-scheme switching keeps working.
 *
 * Direction: a cool graphite canvas (code-editor grade), an electric violet brand
 * accent, and a molten "forge" ember reserved for identity moments.
 */

/* ------------------------------------------------------------------------- */
/* Typefaces                                                                  */
/* ------------------------------------------------------------------------- */

export const fontFamilySans = [
  "'Inter Variable'",
  '-apple-system',
  'BlinkMacSystemFont',
  "'Segoe UI'",
  'Roboto',
  "'Helvetica Neue'",
  'Arial',
  'sans-serif',
].join(', ');

export const fontFamilyMono = [
  "'JetBrains Mono Variable'",
  'ui-monospace',
  'SFMono-Regular',
  "'SF Mono'",
  'Menlo',
  'Consolas',
  "'Liberation Mono'",
  'monospace',
].join(', ');

/* ------------------------------------------------------------------------- */
/* Geometry                                                                   */
/* ------------------------------------------------------------------------- */

export const radius = {
  xs: 4,
  sm: 6,
  md: 8,
  lg: 12,
  xl: 16,
  pill: 999,
} as const;

/** Base spacing unit, in px. Deliberately tight — this is a dense working tool. */
export const spacingUnit = 8;

/* ------------------------------------------------------------------------- */
/* Dark scheme (the primary, hand-tuned scheme)                               */
/* ------------------------------------------------------------------------- */

export const dark = {
  brand: {
    violet: '#7C6BFF',
    violetLight: '#A99CFF',
    violetDark: '#5B49E5',
    ember: '#FF7A2F',
    emberBg: 'rgba(255, 122, 47, 0.14)',
    ring: 'rgba(124, 107, 255, 0.55)',
    gradient: 'linear-gradient(135deg, #7C6BFF 0%, #FF7A2F 100%)',
  },
  surface: {
    canvas: '#0A0D13',
    paper: '#0E121A',
    raised: '#141922',
    sunken: '#070A10',
    overlay: '#171D27',
    hover: 'rgba(255, 255, 255, 0.045)',
    selected: 'rgba(124, 107, 255, 0.14)',
  },
  border: {
    subtle: '#1A2029',
    default: '#232B38',
    strong: '#334055',
  },
  text: {
    primary: '#E3E8F0',
    secondary: '#9BA6B8',
    disabled: '#5A6577',
  },
  accent: {
    primaryContrast: '#0A0D13',
    secondary: '#2DD4E6',
    secondaryLight: '#7FE6F2',
    secondaryDark: '#12A0B2',
    secondaryContrast: '#04141A',
  },
  semantic: {
    success: '#3DD68C',
    successLight: '#7BE8B0',
    successDark: '#1FA968',
    warning: '#F5A623',
    warningLight: '#FFD68F',
    warningDark: '#C57D08',
    error: '#FF5C6C',
    errorLight: '#FFA0A8',
    errorDark: '#DB3B4C',
    info: '#4AB3FF',
    infoLight: '#9BD6FF',
    infoDark: '#1B84D6',
  },
  difficulty: {
    easy: '#3DD68C',
    easyBg: 'rgba(61, 214, 140, 0.14)',
    medium: '#F5A623',
    mediumBg: 'rgba(245, 166, 35, 0.14)',
    hard: '#FF5C6C',
    hardBg: 'rgba(255, 92, 108, 0.14)',
  },
  verdict: {
    accepted: '#3DD68C',
    acceptedBg: 'rgba(61, 214, 140, 0.14)',
    wrongAnswer: '#FF5C6C',
    wrongAnswerBg: 'rgba(255, 92, 108, 0.14)',
    timeLimit: '#F5A623',
    timeLimitBg: 'rgba(245, 166, 35, 0.14)',
    runtimeError: '#FF7A2F',
    runtimeErrorBg: 'rgba(255, 122, 47, 0.14)',
    compileError: '#C792EA',
    compileErrorBg: 'rgba(199, 146, 234, 0.14)',
    pending: '#9BA6B8',
    pendingBg: 'rgba(155, 166, 184, 0.12)',
  },
  code: {
    bg: '#070A10',
    gutter: '#4A5567',
    lineHighlight: 'rgba(124, 107, 255, 0.08)',
    keyword: '#C792EA',
    string: '#A5E075',
    number: '#F5A623',
    func: '#61AFEF',
    comment: '#566073',
    variable: '#E3E8F0',
    operator: '#56B6C2',
    type: '#E5C07B',
  },
} as const;

/* ------------------------------------------------------------------------- */
/* Light scheme (a faithful daylight translation, not an afterthought)        */
/* ------------------------------------------------------------------------- */

export const light = {
  brand: {
    violet: '#5B45E0',
    violetLight: '#9A8CFF',
    violetDark: '#4432B8',
    ember: '#E85D0C',
    emberBg: 'rgba(232, 93, 12, 0.10)',
    ring: 'rgba(91, 69, 224, 0.45)',
    gradient: 'linear-gradient(135deg, #5B45E0 0%, #E85D0C 100%)',
  },
  surface: {
    canvas: '#F5F6F8',
    paper: '#FFFFFF',
    raised: '#FFFFFF',
    sunken: '#F1F3F6',
    overlay: '#FFFFFF',
    hover: 'rgba(13, 17, 23, 0.04)',
    selected: 'rgba(91, 69, 224, 0.10)',
  },
  border: {
    subtle: '#EAEDF2',
    default: '#DCE1E9',
    strong: '#B7C0CE',
  },
  text: {
    primary: '#0D1117',
    secondary: '#56606F',
    disabled: '#97A0AD',
  },
  accent: {
    primaryContrast: '#FFFFFF',
    secondary: '#0E9CB0',
    secondaryLight: '#6FDCEC',
    secondaryDark: '#087A8B',
    secondaryContrast: '#FFFFFF',
  },
  semantic: {
    success: '#12A05F',
    successLight: '#5FD3A0',
    successDark: '#0B7645',
    warning: '#B87503',
    warningLight: '#F0BC5C',
    warningDark: '#8A5702',
    error: '#D93848',
    errorLight: '#F58692',
    errorDark: '#A82634',
    info: '#1177D1',
    infoLight: '#6FB8F0',
    infoDark: '#0B5AA0',
  },
  difficulty: {
    easy: '#12A05F',
    easyBg: 'rgba(18, 160, 95, 0.10)',
    medium: '#B87503',
    mediumBg: 'rgba(184, 117, 3, 0.10)',
    hard: '#D93848',
    hardBg: 'rgba(217, 56, 72, 0.10)',
  },
  verdict: {
    accepted: '#12A05F',
    acceptedBg: 'rgba(18, 160, 95, 0.10)',
    wrongAnswer: '#D93848',
    wrongAnswerBg: 'rgba(217, 56, 72, 0.10)',
    timeLimit: '#B87503',
    timeLimitBg: 'rgba(184, 117, 3, 0.10)',
    runtimeError: '#E85D0C',
    runtimeErrorBg: 'rgba(232, 93, 12, 0.10)',
    compileError: '#8B3FD6',
    compileErrorBg: 'rgba(139, 63, 214, 0.10)',
    pending: '#56606F',
    pendingBg: 'rgba(86, 96, 111, 0.10)',
  },
  code: {
    bg: '#F7F8FA',
    gutter: '#A6AFBC',
    lineHighlight: 'rgba(91, 69, 224, 0.06)',
    keyword: '#8B3FD6',
    string: '#2A8C3C',
    number: '#B87503',
    func: '#1F6FD6',
    comment: '#8A94A3',
    variable: '#0D1117',
    operator: '#0E7C8B',
    type: '#9A6B00',
  },
} as const;
