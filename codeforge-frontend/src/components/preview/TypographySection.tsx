import { Paper, Stack, Typography } from '@mui/material';
import { type TypographyProps } from '@mui/material/Typography';
import { Block, Section } from './Section';

type Specimen = { variant: NonNullable<TypographyProps['variant']>; sample: string; note: string };

const headings: Specimen[] = [
  { variant: 'h1', sample: 'Forge your algorithms', note: '2.25rem / 700 / -0.03em' },
  { variant: 'h2', sample: 'Two Sum', note: '1.75rem / 700 / -0.025em' },
  { variant: 'h3', sample: 'Constraints', note: '1.375rem / 650' },
  { variant: 'h4', sample: 'Example 1', note: '1.125rem / 650' },
  { variant: 'h5', sample: 'Hidden test cases', note: '1rem / 600' },
  { variant: 'h6', sample: 'Runtime distribution', note: '0.875rem / 600' },
];

const body: Specimen[] = [
  { variant: 'subtitle1', sample: 'Given an array of integers, return indices of the two numbers.', note: 'subtitle1' },
  { variant: 'subtitle2', sample: 'Submitted 3 minutes ago by you', note: 'subtitle2' },
  {
    variant: 'body1',
    sample:
      'You may assume that each input would have exactly one solution, and you may not use the same element twice.',
    note: 'body1 — default reading size',
  },
  {
    variant: 'body2',
    sample: 'Dense UI copy: table cells, side panels, helper text and list rows all land here.',
    note: 'body2 — dense UI default',
  },
  { variant: 'caption', sample: 'Last accepted 2 days ago', note: 'caption' },
  { variant: 'overline', sample: 'Test cases', note: 'overline — monospaced eyebrow' },
];

const technical: Specimen[] = [
  { variant: 'code', sample: 'const seen = new Map<number, number>();', note: 'code — JetBrains Mono' },
  { variant: 'mono', sample: 'runtime 42 ms · memory 17.4 MB · beats 94.21%', note: 'mono — tabular figures' },
  { variant: 'metric', sample: '1,284', note: 'metric — stat tiles' },
];

const SpecimenRow = ({ variant, sample, note }: Specimen) => (
  <Stack
    direction={{ xs: 'column', md: 'row' }}
    spacing={{ xs: 0.5, md: 3 }}
    sx={{
      alignItems: { md: 'baseline' },
      py: 1.25,
      borderBottom: 1,
      borderColor: 'border.subtle',
      '&:last-of-type': { borderBottom: 0 },
    }}
  >
    <Typography variant="mono" sx={{ color: 'text.disabled', width: 168, flexShrink: 0, fontSize: '0.6875rem' }}>
      {note}
    </Typography>
    <Typography variant={variant} sx={{ minWidth: 0 }}>
      {sample}
    </Typography>
  </Stack>
);

export const TypographySection = () => (
  <Section
    id="typography"
    eyebrow="Foundations"
    title="Typography"
    description="Inter for the interface, JetBrains Mono for anything a machine produced — code, runtimes, IDs,
      timers and tags. The base size is 14px rather than MUI's 16px so tables and panels stay information-dense."
  >
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Headings">
        <Stack>
          {headings.map((item) => (
            <SpecimenRow key={item.variant} {...item} />
          ))}
        </Stack>
      </Block>
    </Paper>
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Body & labels">
        <Stack>
          {body.map((item) => (
            <SpecimenRow key={item.variant} {...item} />
          ))}
        </Stack>
      </Block>
    </Paper>
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Technical variants" hint="custom to CodeForge">
        <Stack>
          {technical.map((item) => (
            <SpecimenRow key={item.variant} {...item} />
          ))}
        </Stack>
      </Block>
    </Paper>
  </Section>
);
