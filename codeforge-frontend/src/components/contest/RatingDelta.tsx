import ArrowDownwardRounded from '@mui/icons-material/ArrowDownwardRounded';
import ArrowUpwardRounded from '@mui/icons-material/ArrowUpwardRounded';
import { Stack, Typography } from '@mui/material';
import { formatDelta } from './contest';

/**
 * What a contest did to somebody's rating.
 *
 * <p>Carries an arrow as well as a colour. A number that is only green or red is
 * unreadable to a good proportion of people, and this is the number they came to
 * the page for.
 */
export const RatingDelta = ({ delta, size = 'body2' }: { delta: number; size?: 'body2' | 'metric' }) => {
  const rose = delta >= 0;
  const Icon = rose ? ArrowUpwardRounded : ArrowDownwardRounded;

  return (
    <Stack
      direction="row"
      spacing={0.25}
      sx={{ alignItems: 'center', color: rose ? 'verdict.accepted' : 'verdict.wrongAnswer' }}
    >
      <Icon sx={{ fontSize: size === 'metric' ? 18 : 14 }} />
      <Typography variant={size} sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
        {formatDelta(delta)}
      </Typography>
    </Stack>
  );
};
