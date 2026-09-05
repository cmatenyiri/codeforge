import { Box, Paper, Stack, Typography } from '@mui/material';
import { Block, Section } from './Section';

const elevations = [0, 1, 2, 4, 8, 12, 16, 24];

export const SurfacesSection = () => (
  <Section
    id="surfaces"
    eyebrow="Foundations"
    title="Surfaces & elevation"
    description="Four stacked surfaces — canvas, paper, raised, overlay — plus a sunken well for anything
      machine-generated. Elevation exists, but on a near-black canvas a hairline border usually does the job better."
  >
    <Block label="Surface ladder">
      <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
        {(['canvas', 'paper', 'raised', 'overlay', 'sunken'] as const).map((name) => (
          <Paper key={name} variant="outlined" sx={{ width: 168, p: 2, backgroundColor: `surface.${name}` }}>
            <Typography variant="mono" sx={{ fontSize: '0.6875rem' }}>
              surface.{name}
            </Typography>
          </Paper>
        ))}
      </Stack>
    </Block>

    <Block label="Paper variants">
      <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
        <Paper sx={{ width: 200, p: 2 }}>
          <Typography variant="body2">elevation (default)</Typography>
        </Paper>
        <Paper variant="outlined" sx={{ width: 200, p: 2 }}>
          <Typography variant="body2">outlined</Typography>
        </Paper>
        <Paper variant="sunken" sx={{ width: 200, p: 2 }}>
          <Typography variant="body2">sunken (custom)</Typography>
        </Paper>
      </Stack>
    </Block>

    <Block label="Elevation scale" hint="tuned for dark: deeper and more transparent than stock MUI">
      <Stack direction="row" spacing={2.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
        {elevations.map((level) => (
          <Box
            key={level}
            sx={{
              width: 92,
              height: 68,
              borderRadius: 1.5,
              backgroundColor: 'surface.raised',
              boxShadow: level,
              display: 'grid',
              placeItems: 'center',
            }}
          >
            <Typography variant="mono" sx={{ fontSize: '0.6875rem', color: 'text.disabled' }}>
              {level}
            </Typography>
          </Box>
        ))}
      </Stack>
    </Block>
  </Section>
);
