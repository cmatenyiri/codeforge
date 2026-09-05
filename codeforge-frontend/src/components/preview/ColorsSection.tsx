import { Box, Paper, Stack, Typography, useColorScheme, useTheme } from '@mui/material';
import { Block, Section } from './Section';

type Swatch = { label: string; path: string };

const readToken = (palette: Record<string, unknown>, path: string): string => {
  let cursor: unknown = palette;
  for (const key of path.split('.')) {
    cursor = cursor && typeof cursor === 'object' && key in cursor ? (cursor as Record<string, unknown>)[key] : null;
  }
  return typeof cursor === 'string' ? cursor : '—';
};

const brand: Swatch[] = [
  { label: 'primary.light', path: 'primary.light' },
  { label: 'primary.main', path: 'primary.main' },
  { label: 'primary.dark', path: 'primary.dark' },
  { label: 'secondary.light', path: 'secondary.light' },
  { label: 'secondary.main', path: 'secondary.main' },
  { label: 'secondary.dark', path: 'secondary.dark' },
  { label: 'brand.ember', path: 'brand.ember' },
];

const semantic: Swatch[] = [
  { label: 'success.main', path: 'success.main' },
  { label: 'warning.main', path: 'warning.main' },
  { label: 'error.main', path: 'error.main' },
  { label: 'info.main', path: 'info.main' },
];

const difficulty: Swatch[] = [
  { label: 'difficulty.easy', path: 'difficulty.easy' },
  { label: 'difficulty.medium', path: 'difficulty.medium' },
  { label: 'difficulty.hard', path: 'difficulty.hard' },
];

const verdict: Swatch[] = [
  { label: 'verdict.accepted', path: 'verdict.accepted' },
  { label: 'verdict.wrongAnswer', path: 'verdict.wrongAnswer' },
  { label: 'verdict.timeLimit', path: 'verdict.timeLimit' },
  { label: 'verdict.runtimeError', path: 'verdict.runtimeError' },
  { label: 'verdict.compileError', path: 'verdict.compileError' },
  { label: 'verdict.pending', path: 'verdict.pending' },
];

const surfaces: Swatch[] = [
  { label: 'surface.canvas', path: 'surface.canvas' },
  { label: 'surface.paper', path: 'surface.paper' },
  { label: 'surface.raised', path: 'surface.raised' },
  { label: 'surface.overlay', path: 'surface.overlay' },
  { label: 'surface.sunken', path: 'surface.sunken' },
  { label: 'border.subtle', path: 'border.subtle' },
  { label: 'border.default', path: 'border.default' },
  { label: 'border.strong', path: 'border.strong' },
];

const codeTokens: Swatch[] = [
  { label: 'code.keyword', path: 'code.keyword' },
  { label: 'code.string', path: 'code.string' },
  { label: 'code.number', path: 'code.number' },
  { label: 'code.func', path: 'code.func' },
  { label: 'code.type', path: 'code.type' },
  { label: 'code.operator', path: 'code.operator' },
  { label: 'code.comment', path: 'code.comment' },
  { label: 'code.variable', path: 'code.variable' },
];

const SwatchTile = ({ label, path, value }: Swatch & { value: string }) => (
  <Paper variant="outlined" sx={{ overflow: 'hidden', width: 148 }}>
    <Box sx={{ height: 52, backgroundColor: path, borderBottom: 1, borderColor: 'border.subtle' }} />
    <Stack sx={{ px: 1.25, py: 1 }}>
      <Typography variant="mono" sx={{ fontSize: '0.6875rem' }} noWrap>
        {label}
      </Typography>
      <Typography variant="mono" sx={{ fontSize: '0.6875rem', color: 'text.disabled' }} noWrap>
        {value}
      </Typography>
    </Stack>
  </Paper>
);

const SwatchRow = ({ items, palette }: { items: Swatch[]; palette: Record<string, unknown> }) => (
  <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
    {items.map((item) => (
      <SwatchTile key={item.label} {...item} value={readToken(palette, item.path)} />
    ))}
  </Stack>
);

export const ColorsSection = () => {
  const theme = useTheme();
  const { colorScheme } = useColorScheme();
  const palette = (theme.colorSchemes[colorScheme ?? 'dark']?.palette ?? {}) as unknown as Record<string, unknown>;

  return (
    <Section
      id="colors"
      eyebrow="Foundations"
      title="Color"
      description="A cool graphite canvas with an electric violet accent and a molten ember reserved for identity.
        Difficulty and verdict colors are first-class tokens so they can never drift from the semantic palette."
    >
      <Block label="Brand" hint="primary carries actions; ember is identity only">
        <SwatchRow items={brand} palette={palette} />
      </Block>
      <Block label="Semantic">
        <SwatchRow items={semantic} palette={palette} />
      </Block>
      <Block label="Difficulty" hint="paired with *Bg tints for chips">
        <SwatchRow items={difficulty} palette={palette} />
      </Block>
      <Block label="Judge verdicts">
        <SwatchRow items={verdict} palette={palette} />
      </Block>
      <Block label="Surfaces & borders" hint="chrome is drawn with borders, not shadows">
        <SwatchRow items={surfaces} palette={palette} />
      </Block>
      <Block label="Syntax tokens">
        <SwatchRow items={codeTokens} palette={palette} />
      </Block>
    </Section>
  );
};
