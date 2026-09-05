import { type Theme, type ThemeOptions } from '@mui/material/styles';
import { fontFamilyMono, radius } from './tokens';

type Overrides = NonNullable<ThemeOptions['components']>;

const tintedColors = ['primary', 'secondary', 'success', 'error', 'warning', 'info'] as const;

/** `variant="soft"` buttons: a tinted, borderless workhorse for toolbar actions. */
const softButtonVariants = tintedColors.map((color) => ({
  props: { variant: 'soft' as const, color },
  style: ({ theme }: { theme: Theme }) => ({
    backgroundColor: `rgba(${theme.vars.palette[color].mainChannel} / 0.14)`,
    color: theme.vars.palette[color].main,
    '&:hover': { backgroundColor: `rgba(${theme.vars.palette[color].mainChannel} / 0.24)` },
    '&.Mui-disabled': {
      backgroundColor: `rgba(${theme.vars.palette[color].mainChannel} / 0.08)`,
      color: theme.vars.palette.text.disabled,
    },
  }),
}));

const baseline: Overrides = {
  MuiCssBaseline: {
    styleOverrides: (theme: Theme) => ({
      ':root': {
        colorScheme: theme.palette.mode,
      },
      'html, body, #root': {
        height: '100%',
      },
      body: {
        backgroundColor: theme.vars.palette.surface.canvas,
        // Long sessions: keep glyph rendering crisp rather than fattened.
        WebkitFontSmoothing: 'antialiased',
        MozOsxFontSmoothing: 'grayscale',
        textRendering: 'optimizeLegibility',
      },
      '::selection': {
        backgroundColor: `rgba(${theme.vars.palette.primary.mainChannel} / 0.32)`,
      },
      // A thin, self-effacing scrollbar — IDE convention, not OS chrome.
      '*::-webkit-scrollbar': { width: 10, height: 10 },
      '*::-webkit-scrollbar-track': { backgroundColor: 'transparent' },
      '*::-webkit-scrollbar-thumb': {
        backgroundColor: theme.vars.palette.border.default,
        borderRadius: radius.pill,
        border: '2px solid transparent',
        backgroundClip: 'content-box',
      },
      '*::-webkit-scrollbar-thumb:hover': {
        backgroundColor: theme.vars.palette.border.strong,
      },
      '*': { scrollbarWidth: 'thin', scrollbarColor: `${theme.palette.border.default} transparent` },
      code: { fontFamily: fontFamilyMono },
    }),
  },
};

const surfaces: Overrides = {
  MuiPaper: {
    defaultProps: { elevation: 0 },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        backgroundImage: 'none',
        borderRadius: radius.lg,
        backgroundColor: theme.vars.palette.surface.paper,
      }),
      outlined: ({ theme }: { theme: Theme }) => ({
        border: `1px solid ${theme.vars.palette.border.default}`,
      }),
    },
    variants: [
      {
        props: { variant: 'sunken' as const },
        style: ({ theme }: { theme: Theme }) => ({
          backgroundColor: theme.vars.palette.surface.sunken,
          border: `1px solid ${theme.vars.palette.border.subtle}`,
        }),
      },
    ],
  },
  MuiCard: {
    defaultProps: { elevation: 0 },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.lg,
        backgroundColor: theme.vars.palette.surface.paper,
      }),
    },
  },
  MuiCardHeader: {
    styleOverrides: {
      root: { padding: '14px 16px' },
      title: ({ theme }: { theme: Theme }) => theme.typography.h5,
      subheader: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.body2,
        color: theme.vars.palette.text.secondary,
      }),
    },
  },
  MuiCardContent: {
    styleOverrides: {
      root: { padding: 16, '&:last-child': { paddingBottom: 16 } },
    },
  },
  MuiCardActions: {
    styleOverrides: { root: { padding: '10px 16px', gap: 8 } },
  },
  MuiAppBar: {
    defaultProps: { elevation: 0, color: 'transparent' },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        backgroundColor: `rgba(${theme.vars.palette.background.defaultChannel} / 0.72)`,
        backdropFilter: 'blur(12px) saturate(140%)',
        borderBottom: `1px solid ${theme.vars.palette.border.subtle}`,
        borderRadius: 0,
      }),
    },
  },
  MuiToolbar: {
    styleOverrides: {
      root: { minHeight: 56, '@media (min-width:600px)': { minHeight: 56 } },
    },
  },
  MuiDrawer: {
    styleOverrides: {
      paper: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.paper,
        borderRadius: 0,
        borderColor: theme.vars.palette.border.subtle,
      }),
    },
  },
  MuiDialog: {
    styleOverrides: {
      paper: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.raised,
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.xl,
        backgroundImage: 'none',
      }),
    },
  },
  MuiDialogTitle: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ ...theme.typography.h4, padding: '18px 20px 8px' }),
    },
  },
  MuiDialogContent: { styleOverrides: { root: { padding: '8px 20px' } } },
  MuiDialogActions: { styleOverrides: { root: { padding: '14px 20px', gap: 8 } } },
  MuiBackdrop: {
    styleOverrides: { root: { backgroundColor: 'rgba(3, 5, 9, 0.72)' } },
  },
  MuiPopover: {
    styleOverrides: {
      paper: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.overlay,
        border: `1px solid ${theme.vars.palette.border.default}`,
        backgroundImage: 'none',
      }),
    },
  },
  MuiDivider: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ borderColor: theme.vars.palette.border.subtle }),
    },
  },
};

const actions: Overrides = {
  MuiButtonBase: {
    defaultProps: { disableRipple: true },
    styleOverrides: {
      root: { transition: 'background-color 120ms ease, border-color 120ms ease, color 120ms ease' },
    },
  },
  MuiButton: {
    defaultProps: { disableElevation: true, variant: 'contained' },
    styleOverrides: {
      root: {
        borderRadius: radius.md,
        gap: 6,
        whiteSpace: 'nowrap',
      },
      sizeSmall: { height: 30, padding: '0 10px', fontSize: '0.8125rem' },
      sizeMedium: { height: 36, padding: '0 14px' },
      sizeLarge: { height: 44, padding: '0 20px', fontSize: '0.9375rem' },
      contained: {
        // A one-pixel specular edge stops flat fills reading as dead rectangles.
        boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.16)',
        '&:hover': { boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.16)' },
        '&.Mui-disabled': { boxShadow: 'none' },
      },
      outlined: ({ theme }: { theme: Theme }) => ({
        borderColor: theme.vars.palette.border.default,
        color: theme.vars.palette.text.primary,
        '&:hover': {
          borderColor: theme.vars.palette.border.strong,
          backgroundColor: theme.vars.palette.surface.hover,
        },
      }),
      text: ({ theme }: { theme: Theme }) => ({
        color: theme.vars.palette.text.secondary,
        '&:hover': {
          color: theme.vars.palette.text.primary,
          backgroundColor: theme.vars.palette.surface.hover,
        },
      }),
    },
    variants: softButtonVariants,
  },
  MuiIconButton: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        borderRadius: radius.md,
        color: theme.vars.palette.text.secondary,
        '&:hover': {
          color: theme.vars.palette.text.primary,
          backgroundColor: theme.vars.palette.surface.hover,
        },
      }),
      sizeSmall: { padding: 5 },
      sizeMedium: { padding: 7 },
    },
  },
  MuiButtonGroup: {
    defaultProps: { disableElevation: true, disableRipple: true },
    styleOverrides: {
      root: { borderRadius: radius.md },
      grouped: { borderRadius: radius.md },
    },
  },
  MuiToggleButtonGroup: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.sunken,
        border: `1px solid ${theme.vars.palette.border.subtle}`,
        borderRadius: radius.md,
        padding: 3,
        gap: 3,
      }),
      grouped: {
        border: '0 !important',
        borderRadius: `${radius.sm}px !important`,
      },
    },
  },
  MuiToggleButton: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        textTransform: 'none',
        fontWeight: 600,
        fontSize: '0.8125rem',
        padding: '5px 12px',
        color: theme.vars.palette.text.secondary,
        '&:hover': { backgroundColor: theme.vars.palette.surface.hover },
        '&.Mui-selected': {
          backgroundColor: theme.vars.palette.surface.raised,
          color: theme.vars.palette.text.primary,
          boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.08)',
          '&:hover': { backgroundColor: theme.vars.palette.surface.raised },
        },
      }),
    },
  },
  MuiFab: {
    styleOverrides: { root: { borderRadius: radius.lg, boxShadow: 'none', textTransform: 'none' } },
  },
  MuiLink: {
    defaultProps: { underline: 'hover' },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        color: theme.vars.palette.primary.main,
        textUnderlineOffset: '0.2em',
        fontWeight: 500,
      }),
    },
  },
};

const inputs: Overrides = {
  MuiTextField: { defaultProps: { size: 'small', variant: 'outlined' } },
  MuiFormControl: { defaultProps: { size: 'small' } },
  MuiInputBase: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ ...theme.typography.body2 }),
      input: ({ theme }: { theme: Theme }) => ({
        '&::placeholder': { color: theme.vars.palette.text.disabled, opacity: 1 },
      }),
    },
  },
  MuiOutlinedInput: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.sunken,
        borderRadius: radius.md,
        transition: 'border-color 120ms ease, box-shadow 120ms ease',
        '& .MuiOutlinedInput-notchedOutline': {
          borderColor: theme.vars.palette.border.default,
          transition: 'border-color 120ms ease',
        },
        '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: theme.vars.palette.border.strong },
        '&.Mui-focused': {
          boxShadow: `0 0 0 3px rgba(${theme.vars.palette.primary.mainChannel} / 0.22)`,
        },
        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
          borderWidth: 1,
          borderColor: theme.vars.palette.primary.main,
        },
        '&.Mui-disabled': { backgroundColor: theme.vars.palette.surface.hover },
      }),
      input: { padding: '9px 12px' },
      sizeSmall: { '& .MuiOutlinedInput-input': { padding: '7px 10px' } },
    },
  },
  MuiFilledInput: {
    defaultProps: { disableUnderline: true },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.sunken,
        borderRadius: radius.md,
        border: `1px solid ${theme.vars.palette.border.subtle}`,
        '&:hover, &.Mui-focused': { backgroundColor: theme.vars.palette.surface.sunken },
        '&.Mui-focused': { borderColor: theme.vars.palette.primary.main },
      }),
    },
  },
  MuiInputLabel: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.body2,
        color: theme.vars.palette.text.secondary,
      }),
    },
  },
  MuiFormLabel: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ color: theme.vars.palette.text.secondary }),
    },
  },
  MuiFormHelperText: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ ...theme.typography.caption, marginLeft: 2 }),
    },
  },
  MuiFormControlLabel: {
    styleOverrides: {
      label: ({ theme }: { theme: Theme }) => ({ ...theme.typography.body2 }),
    },
  },
  MuiSelect: {
    styleOverrides: {
      icon: ({ theme }: { theme: Theme }) => ({ color: theme.vars.palette.text.secondary }),
    },
  },
  MuiCheckbox: {
    defaultProps: { size: 'small', disableRipple: true },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        color: theme.vars.palette.border.strong,
        borderRadius: radius.xs,
      }),
    },
  },
  MuiRadio: {
    defaultProps: { size: 'small', disableRipple: true },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ color: theme.vars.palette.border.strong }),
    },
  },
  MuiSwitch: {
    defaultProps: { disableRipple: true },
    styleOverrides: {
      root: { width: 40, height: 24, padding: 0, overflow: 'visible' },
      switchBase: ({ theme }: { theme: Theme }) => ({
        padding: 3,
        '&.Mui-checked': {
          transform: 'translateX(16px)',
          color: '#FFFFFF',
          '& + .MuiSwitch-track': { backgroundColor: theme.vars.palette.primary.main, opacity: 1 },
        },
      }),
      thumb: { width: 18, height: 18, boxShadow: '0 1px 2px rgba(0, 0, 0, 0.4)' },
      track: ({ theme }: { theme: Theme }) => ({
        borderRadius: radius.pill,
        backgroundColor: theme.vars.palette.border.strong,
        opacity: 1,
      }),
    },
  },
  MuiSlider: {
    styleOverrides: {
      root: { height: 4 },
      rail: ({ theme }: { theme: Theme }) => ({ backgroundColor: theme.vars.palette.border.default, opacity: 1 }),
      track: { border: 'none' },
      thumb: ({ theme }: { theme: Theme }) => ({
        width: 14,
        height: 14,
        border: `2px solid ${theme.vars.palette.surface.canvas}`,
        '&:hover, &.Mui-focusVisible': {
          boxShadow: `0 0 0 6px rgba(${theme.vars.palette.primary.mainChannel} / 0.2)`,
        },
      }),
      valueLabel: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.mono,
        backgroundColor: theme.vars.palette.surface.overlay,
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.sm,
      }),
    },
  },
};

const dataDisplay: Overrides = {
  MuiChip: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.mono,
        fontWeight: 600,
        fontSize: '0.75rem',
        borderRadius: radius.sm,
        height: 24,
      }),
      sizeSmall: {
        height: 20,
        fontSize: '0.6875rem',
        '& .MuiChip-label': { paddingLeft: 6, paddingRight: 6 },
      },
      label: { paddingLeft: 8, paddingRight: 8 },
      outlined: ({ theme }: { theme: Theme }) => ({
        borderColor: theme.vars.palette.border.default,
        color: theme.vars.palette.text.secondary,
      }),
      filled: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.hover,
        color: theme.vars.palette.text.primary,
      }),
      icon: { marginLeft: 6, fontSize: '0.875rem' },
      deleteIcon: { fontSize: '0.875rem', marginRight: 5 },
    },
  },
  MuiAvatar: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        // Squircles rather than circles — reads as an app icon, not a social profile.
        borderRadius: radius.md,
        fontSize: '0.8125rem',
        fontWeight: 650,
        backgroundColor: theme.vars.palette.surface.raised,
        color: theme.vars.palette.text.primary,
      }),
    },
  },
  MuiAvatarGroup: {
    styleOverrides: {
      avatar: ({ theme }: { theme: Theme }) => ({
        border: `2px solid ${theme.vars.palette.surface.paper}`,
      }),
    },
  },
  MuiBadge: {
    styleOverrides: {
      badge: ({ theme }: { theme: Theme }) => ({
        fontFamily: fontFamilyMono,
        fontSize: '0.625rem',
        fontWeight: 700,
        minWidth: 18,
        height: 18,
        borderRadius: radius.pill,
        border: `2px solid ${theme.vars.palette.surface.paper}`,
      }),
    },
  },
  MuiTable: { defaultProps: { size: 'small' } },
  MuiTableCell: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        borderBottomColor: theme.vars.palette.border.subtle,
        padding: '10px 14px',
      }),
      head: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.overline,
        color: theme.vars.palette.text.secondary,
        backgroundColor: theme.vars.palette.surface.sunken,
        borderBottom: `1px solid ${theme.vars.palette.border.default}`,
        whiteSpace: 'nowrap',
      }),
      body: ({ theme }: { theme: Theme }) => ({ ...theme.typography.body2 }),
    },
  },
  MuiTableRow: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        '&:hover:not(.MuiTableRow-head)': { backgroundColor: theme.vars.palette.surface.hover },
        '&:last-child td': { borderBottom: 0 },
      }),
    },
  },
  MuiList: { defaultProps: { dense: true }, styleOverrides: { root: { padding: 6 } } },
  MuiListItemButton: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        borderRadius: radius.md,
        paddingTop: 6,
        paddingBottom: 6,
        color: theme.vars.palette.text.secondary,
        '&:hover': {
          backgroundColor: theme.vars.palette.surface.hover,
          color: theme.vars.palette.text.primary,
        },
        '&.Mui-selected': {
          backgroundColor: theme.vars.palette.surface.selected,
          color: theme.vars.palette.primary.main,
          '&:hover': { backgroundColor: theme.vars.palette.surface.selected },
        },
      }),
    },
  },
  MuiListItemIcon: { styleOverrides: { root: { minWidth: 30, color: 'inherit' } } },
  MuiListItemText: {
    styleOverrides: {
      primary: ({ theme }: { theme: Theme }) => ({ ...theme.typography.body2, fontWeight: 500 }),
      secondary: ({ theme }: { theme: Theme }) => ({ ...theme.typography.caption }),
    },
  },
  MuiListSubheader: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.overline,
        backgroundColor: 'transparent',
        color: theme.vars.palette.text.disabled,
        lineHeight: '30px',
      }),
    },
  },
  MuiTooltip: {
    defaultProps: { arrow: true },
    styleOverrides: {
      tooltip: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.overlay,
        color: theme.vars.palette.text.primary,
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.sm,
        fontSize: '0.75rem',
        fontWeight: 500,
        padding: '5px 9px',
        boxShadow: theme.vars.shadows[4],
      }),
      arrow: ({ theme }: { theme: Theme }) => ({ color: theme.vars.palette.surface.overlay }),
    },
  },
  MuiSkeleton: {
    defaultProps: { animation: 'wave' },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.hover,
        borderRadius: radius.sm,
      }),
    },
  },
  MuiAccordion: {
    defaultProps: { disableGutters: true, elevation: 0, square: false },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.md,
        backgroundColor: theme.vars.palette.surface.paper,
        '&::before': { display: 'none' },
        '& + &': { marginTop: 8 },
      }),
    },
  },
  MuiAccordionSummary: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        minHeight: 46,
        padding: '0 14px',
        '&.Mui-expanded': { minHeight: 46 },
        '&:hover': { backgroundColor: theme.vars.palette.surface.hover },
        borderRadius: radius.md,
      }),
      content: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.subtitle2,
        margin: '10px 0',
        '&.Mui-expanded': { margin: '10px 0' },
      }),
    },
  },
  MuiAccordionDetails: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        padding: '4px 14px 14px',
        borderTop: `1px solid ${theme.vars.palette.border.subtle}`,
        paddingTop: 12,
      }),
    },
  },
};

const navigation: Overrides = {
  MuiTabs: {
    styleOverrides: {
      root: { minHeight: 42 },
      indicator: ({ theme }: { theme: Theme }) => ({
        height: 2,
        borderRadius: radius.pill,
        backgroundColor: theme.vars.palette.primary.main,
      }),
    },
  },
  MuiTab: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        textTransform: 'none',
        fontWeight: 600,
        fontSize: '0.8125rem',
        minHeight: 42,
        minWidth: 0,
        padding: '0 14px',
        color: theme.vars.palette.text.secondary,
        '&:hover': { color: theme.vars.palette.text.primary },
        '&.Mui-selected': { color: theme.vars.palette.text.primary },
      }),
    },
  },
  MuiMenu: {
    styleOverrides: {
      paper: ({ theme }: { theme: Theme }) => ({
        backgroundColor: theme.vars.palette.surface.overlay,
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.md,
        backgroundImage: 'none',
        boxShadow: theme.vars.shadows[8],
      }),
      list: { padding: 5 },
    },
  },
  MuiMenuItem: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.body2,
        borderRadius: radius.sm,
        minHeight: 32,
        padding: '5px 9px',
        '&:hover': { backgroundColor: theme.vars.palette.surface.hover },
        '&.Mui-selected': {
          backgroundColor: theme.vars.palette.surface.selected,
          '&:hover': { backgroundColor: theme.vars.palette.surface.selected },
        },
      }),
    },
  },
  MuiBreadcrumbs: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ ...theme.typography.body2 }),
      separator: ({ theme }: { theme: Theme }) => ({
        color: theme.vars.palette.text.disabled,
        marginLeft: 6,
        marginRight: 6,
      }),
    },
  },
  MuiPaginationItem: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        fontFamily: fontFamilyMono,
        fontSize: '0.8125rem',
        fontWeight: 500,
        borderRadius: radius.sm,
        color: theme.vars.palette.text.secondary,
        '&.Mui-selected': {
          backgroundColor: theme.vars.palette.surface.selected,
          color: theme.vars.palette.primary.main,
          fontWeight: 700,
        },
      }),
      outlined: ({ theme }: { theme: Theme }) => ({ borderColor: theme.vars.palette.border.default }),
    },
  },
  MuiStepLabel: {
    styleOverrides: {
      label: ({ theme }: { theme: Theme }) => ({ ...theme.typography.body2, fontWeight: 500 }),
    },
  },
  MuiStepConnector: {
    styleOverrides: {
      line: ({ theme }: { theme: Theme }) => ({ borderColor: theme.vars.palette.border.default }),
    },
  },
};

const feedback: Overrides = {
  MuiAlert: {
    defaultProps: { variant: 'standard' },
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.body2,
        borderRadius: radius.md,
        padding: '9px 14px',
        alignItems: 'flex-start',
      }),
      icon: { padding: '2px 0', marginRight: 10, fontSize: '1.125rem' },
      message: { padding: '2px 0' },
    },
    variants: tintedColors
      .filter(
        (color): color is 'success' | 'error' | 'warning' | 'info' => color !== 'primary' && color !== 'secondary',
      )
      .map((severity) => ({
        props: { severity, variant: 'standard' as const },
        style: ({ theme }: { theme: Theme }) => ({
          backgroundColor: `rgba(${theme.vars.palette[severity].mainChannel} / 0.12)`,
          color: theme.vars.palette.text.primary,
          // A left rule instead of a full border: the severity reads at a glance.
          border: `1px solid rgba(${theme.vars.palette[severity].mainChannel} / 0.24)`,
          borderLeft: `3px solid ${theme.vars.palette[severity].main}`,
          '& .MuiAlert-icon': { color: theme.vars.palette[severity].main },
        }),
      })),
  },
  MuiAlertTitle: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({ ...theme.typography.subtitle2, marginBottom: 2 }),
    },
  },
  MuiSnackbarContent: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        ...theme.typography.body2,
        backgroundColor: theme.vars.palette.surface.overlay,
        color: theme.vars.palette.text.primary,
        border: `1px solid ${theme.vars.palette.border.default}`,
        borderRadius: radius.md,
        boxShadow: theme.vars.shadows[10],
      }),
    },
  },
  MuiLinearProgress: {
    styleOverrides: {
      root: ({ theme }: { theme: Theme }) => ({
        height: 6,
        borderRadius: radius.pill,
        backgroundColor: theme.vars.palette.surface.hover,
      }),
      bar: { borderRadius: radius.pill },
    },
  },
  MuiCircularProgress: {
    styleOverrides: { circle: { strokeLinecap: 'round' } },
  },
};

export const components: Overrides = {
  ...baseline,
  ...surfaces,
  ...actions,
  ...inputs,
  ...dataDisplay,
  ...navigation,
  ...feedback,
};
