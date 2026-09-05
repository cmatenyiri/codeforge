import LocalFireDepartmentRounded from '@mui/icons-material/LocalFireDepartmentRounded';
import SpeedRounded from '@mui/icons-material/SpeedRounded';
import EmojiEventsRounded from '@mui/icons-material/EmojiEventsRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import { Box, Grid, LinearProgress, Paper, Stack, Typography } from '@mui/material';
import { type ReactNode } from 'react';

type Tile = { label: string; value: string; delta: string; icon: ReactNode; tint: string };

const tiles: Tile[] = [
  {
    label: 'Problems solved',
    value: '428',
    delta: '+12 this week',
    icon: <CheckCircleRounded fontSize="small" />,
    tint: 'primary.main',
  },
  {
    label: 'Acceptance rate',
    value: '63.2%',
    delta: '+1.8 pts',
    icon: <SpeedRounded fontSize="small" />,
    tint: 'secondary.main',
  },
  {
    label: 'Current streak',
    value: '17d',
    delta: 'Personal best 24d',
    icon: <LocalFireDepartmentRounded fontSize="small" />,
    tint: 'brand.ember',
  },
  {
    label: 'Global rank',
    value: '#1,204',
    delta: 'Top 4%',
    icon: <EmojiEventsRounded fontSize="small" />,
    tint: 'warning.main',
  },
];

const byDifficulty = [
  { level: 'easy' as const, label: 'Easy', solved: 201, total: 244 },
  { level: 'medium' as const, label: 'Medium', solved: 178, total: 512 },
  { level: 'hard' as const, label: 'Hard', solved: 49, total: 231 },
];

const StatTile = ({ label, value, delta, icon, tint }: Tile) => (
  <Paper variant="outlined" sx={{ p: 2, height: '100%' }}>
    <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1.25 }}>
      <Typography variant="overline" sx={{ color: 'text.disabled' }}>
        {label}
      </Typography>
      <Box sx={{ color: tint, display: 'flex' }}>{icon}</Box>
    </Stack>
    <Typography variant="metric">{value}</Typography>
    <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mt: 0.5 }}>
      {delta}
    </Typography>
  </Paper>
);

/** Dashboard-style summary tiles — the strongest test of the metric type variant. */
export const StatTiles = () => (
  <Grid container spacing={2}>
    {tiles.map((tile) => (
      <Grid key={tile.label} size={{ xs: 12, sm: 6, lg: 3 }}>
        <StatTile {...tile} />
      </Grid>
    ))}
    <Grid size={12}>
      <Paper variant="outlined" sx={{ p: 2 }}>
        <Typography variant="overline" sx={{ color: 'text.disabled' }}>
          Progress by difficulty
        </Typography>
        <Stack spacing={2} sx={{ mt: 1.5 }}>
          {byDifficulty.map((row) => (
            <Stack key={row.level} spacing={0.75}>
              <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between' }}>
                <Typography variant="body2" sx={{ color: `difficulty.${row.level}`, fontWeight: 600 }}>
                  {row.label}
                </Typography>
                <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                  {row.solved} / {row.total}
                </Typography>
              </Stack>
              <LinearProgress
                variant="determinate"
                value={(row.solved / row.total) * 100}
                sx={{ '& .MuiLinearProgress-bar': { backgroundColor: `difficulty.${row.level}` } }}
              />
            </Stack>
          ))}
        </Stack>
      </Paper>
    </Grid>
  </Grid>
);
