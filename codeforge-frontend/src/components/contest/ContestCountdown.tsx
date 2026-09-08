import { Box, LinearProgress, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { clockTone, formatClock, formatCountdown } from './contest';

const TONE_COLOR = {
  normal: 'text.primary',
  warning: 'verdict.timeLimit',
  critical: 'verdict.wrongAnswer',
} as const;

/**
 * The clock, in its two moods.
 *
 * <p>Before the start it counts down to the moment everybody begins, and it is
 * calm — a contest four days away is information, not pressure. Once running it
 * behaves like the interview timer: loud, and louder in the last minute, because
 * budgeting the ninety minutes is half of what a contest measures.
 */
export const ContestCountdown = ({
  seconds,
  mode,
  durationMinutes,
}: {
  seconds: number;
  /** `until` counts down to the start; `remaining` counts down the contest itself. */
  mode: 'until' | 'remaining';
  durationMinutes: number;
}) => {
  const { t } = useTranslation();
  const running = mode === 'remaining';
  const tone = running ? clockTone(seconds) : 'normal';
  const budget = durationMinutes * 60;
  const spent = Math.min(100, Math.max(0, ((budget - seconds) / budget) * 100));

  return (
    <Box sx={{ minWidth: 148 }}>
      <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}>
        <Typography variant="overline" sx={{ color: 'text.disabled' }}>
          {t(running ? 'contest.timeLeft' : 'contest.startsIn')}
        </Typography>
        <Typography
          variant="metric"
          aria-live={tone === 'critical' ? 'assertive' : 'off'}
          sx={{
            fontVariantNumeric: 'tabular-nums',
            color: TONE_COLOR[tone],
            // The last minute pulses. Colour alone is not a signal everybody
            // receives, and this is the one moment worth insisting on.
            animation: tone === 'critical' ? 'contest-pulse 1s ease-in-out infinite' : undefined,
            '@keyframes contest-pulse': { '50%': { opacity: 0.45 } },
            '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
          }}
        >
          {running ? formatClock(seconds) : formatCountdown(seconds)}
        </Typography>
      </Stack>

      {running ? (
        <LinearProgress
          variant="determinate"
          value={spent}
          sx={{
            mt: 0.5,
            height: 4,
            borderRadius: 2,
            '& .MuiLinearProgress-bar': { backgroundColor: TONE_COLOR[tone], borderRadius: 2 },
          }}
        />
      ) : null}
    </Box>
  );
};
