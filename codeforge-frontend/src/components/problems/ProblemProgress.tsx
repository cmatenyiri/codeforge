import { Box, LinearProgress, Paper, Skeleton, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type UserStats } from '../../api/types';
import { DIFFICULTY_LABEL_KEY, DIFFICULTY_TOKEN } from './difficulty';

/** Solved / total at one difficulty, in that difficulty's colour. */
const DifficultyBar = ({ difficulty, solved, total }: UserStats['progress'][number]) => {
  const { t } = useTranslation();
  const token = DIFFICULTY_TOKEN[difficulty];

  return (
    <Box sx={{ flex: 1, minWidth: 130 }}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline', mb: 0.75 }}>
        <Typography variant="overline" sx={{ color: `difficulty.${token}` }}>
          {t(DIFFICULTY_LABEL_KEY[difficulty])}
        </Typography>
        <Typography variant="mono" sx={{ color: 'text.secondary' }}>
          {solved}
          <Box component="span" sx={{ color: 'text.disabled' }}>{` / ${total}`}</Box>
        </Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        // A zero-length catalogue would otherwise divide by zero and render NaN.
        value={total === 0 ? 0 : (solved / total) * 100}
        sx={{
          height: 6,
          borderRadius: 3,
          backgroundColor: `difficulty.${token}Bg`,
          '& .MuiLinearProgress-bar': { backgroundColor: `difficulty.${token}`, borderRadius: 3 },
        }}
      />
    </Box>
  );
};

/**
 * The "how far along am I" strip above the catalogue.
 *
 * <p>Rendered from the server's totals rather than from the page on screen: what
 * matters is progress through the whole catalogue, which a filtered page of
 * twenty rows says nothing about.
 */
export const ProblemProgress = ({ stats }: { stats: UserStats | null }) => {
  const { t } = useTranslation();

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={{ xs: 2, sm: 3 }}
        sx={{ alignItems: { sm: 'center' } }}
      >
        <Box sx={{ minWidth: 116 }}>
          <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
            {t('problems.solvedTitle')}
          </Typography>
          {stats ? (
            <Typography variant="metric">
              {stats.solved}
              <Box component="span" sx={{ color: 'text.disabled', typography: 'body2' }}>
                {` / ${stats.totalProblems}`}
              </Box>
            </Typography>
          ) : (
            <Skeleton width={80} height={34} />
          )}
        </Box>

        <Stack direction="row" spacing={2.5} useFlexGap sx={{ flex: 1, flexWrap: 'wrap' }}>
          {stats
            ? stats.progress.map((entry) => <DifficultyBar key={entry.difficulty} {...entry} />)
            : ['a', 'b', 'c'].map((key) => <Skeleton key={key} sx={{ flex: 1, minWidth: 130 }} height={40} />)}
        </Stack>
      </Stack>
    </Paper>
  );
};
