import { Box, Paper, Stack, Typography } from '@mui/material';
import { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { type ContestProblemResult } from '../../api/types';
import { ProblemCell } from './StandingsTable';

/**
 * Made-up results, one of each kind of cell the grid can show, drawn by the
 * grid's own cell component.
 */
const SAMPLES: { key: 'solved' | 'solvedAfterWrong' | 'wrongUnsolved' | 'untouched'; result?: ContestProblemResult }[] = [
  { key: 'solved', result: { position: 0, label: 'Q1', solved: true, solvedAtSeconds: 754, wrongAttempts: 0, attempts: 1 } },
  {
    key: 'solvedAfterWrong',
    result: { position: 0, label: 'Q1', solved: true, solvedAtSeconds: 1522, wrongAttempts: 1, attempts: 2 },
  },
  { key: 'wrongUnsolved', result: { position: 0, label: 'Q1', solved: false, wrongAttempts: 2, attempts: 2 } },
  { key: 'untouched' },
];

/**
 * How to read the standings, above the table rather than in tooltips on it.
 *
 * <p>The grid is dense by design — a time, a red count and a dash carry
 * everything — which is only fair to somebody who has been told what they
 * mean. Hover text on a column header is found by the people who already knew
 * to look.
 */
export const StandingsLegend = () => {
  const { t } = useTranslation();

  return (
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Typography variant="h4" sx={{ mb: 1.5 }}>
        {t('contest.legend.title')}
      </Typography>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={{ xs: 2, md: 4 }}>
        <Stack component="ul" spacing={0.75} sx={{ m: 0, pl: 2.5, flex: 1, color: 'text.secondary' }}>
          {(['ranking', 'score', 'time'] as const).map((key) => (
            <Typography key={key} component="li" variant="body2">
              {t(`contest.legend.${key}`)}
            </Typography>
          ))}
        </Stack>

        <Box
          sx={{
            flex: 1,
            display: 'grid',
            gridTemplateColumns: 'auto 1fr',
            columnGap: 2,
            rowGap: 1.25,
            alignItems: 'center',
          }}
        >
          {SAMPLES.map((sample) => (
            <Fragment key={sample.key}>
              <Box sx={{ minWidth: 72, display: 'flex', justifyContent: 'center' }}>
                <ProblemCell result={sample.result} />
              </Box>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {t(`contest.legend.${sample.key}`)}
              </Typography>
            </Fragment>
          ))}
        </Box>
      </Stack>
    </Paper>
  );
};
