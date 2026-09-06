import { Box, LinearProgress, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { clockTone, formatClock } from './interview';

const TONE_COLOR = {
  normal: 'text.primary',
  warning: 'verdict.timeLimit',
  critical: 'verdict.wrongAnswer',
} as const;

/**
 * The countdown.
 *
 * <p>Deliberately the loudest thing on the screen. A mock interview whose clock
 * is easy to ignore trains the one habit the round exists to test.
 */
export const InterviewTimer = ({
  remainingSeconds,
  durationMinutes,
}: {
  remainingSeconds: number;
  durationMinutes: number;
}) => {
  const { t } = useTranslation();
  const budget = durationMinutes * 60;
  const tone = clockTone(remainingSeconds);
  const spent = Math.min(100, Math.max(0, ((budget - remainingSeconds) / budget) * 100));

  return (
    <Box sx={{ minWidth: 132 }}>
      <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}>
        <Typography variant="overline" sx={{ color: 'text.disabled' }}>
          {t('interview.timeLeft')}
        </Typography>
        <Typography
          variant="metric"
          aria-live={tone === 'critical' ? 'assertive' : 'off'}
          sx={{
            fontVariantNumeric: 'tabular-nums',
            color: TONE_COLOR[tone],
            // The last minute pulses. Colour alone is not a signal everybody
            // receives, and this is the one moment worth insisting on.
            animation: tone === 'critical' ? 'interview-pulse 1s ease-in-out infinite' : undefined,
            '@keyframes interview-pulse': { '50%': { opacity: 0.45 } },
            '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
          }}
        >
          {formatClock(remainingSeconds)}
        </Typography>
      </Stack>

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
    </Box>
  );
};
