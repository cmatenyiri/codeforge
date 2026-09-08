import CalendarMonthRounded from '@mui/icons-material/CalendarMonthRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import ChevronLeftRounded from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import LocalFireDepartmentRounded from '@mui/icons-material/LocalFireDepartmentRounded';
import {
  Box,
  Button,
  Chip,
  IconButton,
  Paper,
  Popover,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { dailyApi } from '../../api/daily-api';
import { type DailyCalendarMonth, type DailyChallenge } from '../../api/types';
import { formatClock } from '../contest/contest';
import { useContestClock } from '../contest/use-contest-clock';
import { DifficultyChip } from '../problems/DifficultyChip';
import { problemPath } from '../../routes/paths';

/** Sunday-first, matching the calendar grids elsewhere in the app. */
const WEEKDAY_COLUMNS = 7;

/**
 * The month grid behind the calendar button.
 *
 * <p>Days that have not arrived are deliberately blank rather than greyed
 * problems: their question has not been decided yet, and deciding it early
 * would fix a future day against today's catalogue.
 */
const MonthGrid = ({ month, onNavigate }: { month: DailyCalendarMonth; onNavigate: (delta: number) => void }) => {
  const { t, i18n } = useTranslation();

  const first = new Date(Date.UTC(month.year, month.month - 1, 1));
  const leading = first.getUTCDay();

  return (
    <Box sx={{ p: 2, width: 300 }}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
        <IconButton size="small" aria-label={t('daily.previousMonth')} onClick={() => { onNavigate(-1); }}>
          <ChevronLeftRounded fontSize="small" />
        </IconButton>
        <Typography variant="subtitle2">
          {first.toLocaleDateString(i18n.language, { month: 'long', year: 'numeric', timeZone: 'UTC' })}
        </Typography>
        <IconButton size="small" aria-label={t('daily.nextMonth')} onClick={() => { onNavigate(1); }}>
          <ChevronRightRounded fontSize="small" />
        </IconButton>
      </Stack>

      <Box sx={{ display: 'grid', gridTemplateColumns: `repeat(${WEEKDAY_COLUMNS}, 1fr)`, gap: 0.5 }}>
        {/* Weekday initials, taken from the locale rather than hard-coded, so a
            German or French reader gets their own — and so the row cannot drift
            out of step with the Sunday-first grid below it. */}
        {Array.from({ length: WEEKDAY_COLUMNS }, (_, index) => (
          <Typography
            key={`weekday-${index}`}
            variant="caption"
            align="center"
            sx={{ color: 'text.disabled' }}
          >
            {new Date(Date.UTC(2024, 0, 7 + index)).toLocaleDateString(i18n.language, {
              weekday: 'narrow',
              timeZone: 'UTC',
            })}
          </Typography>
        ))}

        {Array.from({ length: leading }, (_, index) => (
          <Box key={`pad-${index}`} />
        ))}

        {month.days.map((day) => {
          const number = Number(day.date.slice(-2));
          const reachable = day.slug !== undefined;

          const cell = (
            <Box
              sx={{
                height: 34,
                display: 'grid',
                placeItems: 'center',
                borderRadius: 1,
                position: 'relative',
                border: 1,
                borderColor: day.today ? 'primary.main' : 'transparent',
                color: reachable ? 'text.primary' : 'text.disabled',
                backgroundColor: day.solved ? 'verdict.acceptedBg' : 'transparent',
                textDecoration: 'none',
                '&:hover': reachable ? { backgroundColor: 'surface.hover' } : undefined,
              }}
            >
              <Typography variant="caption" sx={{ fontWeight: day.today ? 700 : 400 }}>
                {number}
              </Typography>
              {day.solved ? (
                <CheckCircleRounded
                  sx={{ position: 'absolute', bottom: 1, right: 1, fontSize: 10, color: 'verdict.accepted' }}
                />
              ) : null}
            </Box>
          );

          return (
            <Tooltip key={day.date} title={reachable ? (day.title ?? '') : t('daily.notYet')}>
              {reachable ? (
                <Box component={Link} to={problemPath(day.slug!)} sx={{ textDecoration: 'none' }}>
                  {cell}
                </Box>
              ) : (
                <Box>{cell}</Box>
              )}
            </Tooltip>
          );
        })}
      </Box>
    </Box>
  );
};

/**
 * The problem of the day, at the top of the problem list — where LeetCode puts
 * it, and for the same reason: it is the one thing on the page that expires.
 *
 * <p>The flame is the whole mechanism. A streak is the only part of this
 * feature that makes somebody open the tab on a day they had not planned to,
 * which is why the countdown to midnight is on screen rather than implied.
 */
export const DailyChallengeCard = () => {
  const { t } = useTranslation();
  const [daily, setDaily] = useState<DailyChallenge | null>(null);
  const [month, setMonth] = useState<DailyCalendarMonth | null>(null);
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const [offset, setOffset] = useState(0);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;

    dailyApi
      .today()
      .then((data) => {
        if (!cancelled) {
          setDaily(data);
        }
      })
      .catch(() => {
        // A catalogue with nothing the rotation can use is the only real cause,
        // and the card simply does not appear — the problem list is still the
        // point of the page.
        if (!cancelled) {
          setFailed(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const loadMonth = useCallback((monthsFromNow: number) => {
    const now = new Date();
    const target = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + monthsFromNow, 1));

    dailyApi
      .calendar(target.getUTCFullYear(), target.getUTCMonth() + 1)
      .then(setMonth)
      .catch(() => {
        setMonth(null);
      });
  }, []);

  useEffect(() => {
    if (anchor !== null) {
      loadMonth(offset);
    }
  }, [anchor, offset, loadMonth]);

  // Anchored to the server's answer and ticking between reads, like every other
  // clock in the app.
  const remaining = useContestClock(daily?.secondsUntilRollover, daily !== null);

  if (failed || daily === null) {
    return null;
  }

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
      >
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
          <Tooltip title={t('daily.calendar')}>
            <IconButton
              size="small"
              onClick={(event) => {
                setAnchor(event.currentTarget);
              }}
              aria-label={t('daily.calendar')}
            >
              <CalendarMonthRounded />
            </IconButton>
          </Tooltip>

          <Box sx={{ minWidth: 0 }}>
            <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
              {t('daily.todayTitle')}
            </Typography>
            <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center', flexWrap: 'wrap' }} useFlexGap>
              <Typography variant="h4" sx={{ minWidth: 0 }}>
                <Box
                  component={Link}
                  to={problemPath(daily.slug)}
                  sx={{ color: 'inherit', textDecoration: 'none', '&:hover': { color: 'primary.main' } }}
                >
                  {daily.title}
                </Box>
              </Typography>
              <DifficultyChip difficulty={daily.difficulty} size="small" />
              {daily.solved ? (
                <Chip
                  icon={<CheckCircleRounded />}
                  label={t('daily.solved')}
                  size="small"
                  sx={{
                    color: 'verdict.accepted',
                    backgroundColor: 'verdict.acceptedBg',
                    '& .MuiChip-icon': { color: 'inherit' },
                  }}
                />
              ) : null}
            </Stack>
          </Box>
        </Stack>

        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          <Tooltip title={t('daily.resetsHint')}>
            <Stack sx={{ alignItems: 'flex-end' }}>
              <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                <LocalFireDepartmentRounded
                  sx={{ fontSize: 20, color: daily.streak > 0 ? 'brand.ember' : 'text.disabled' }}
                />
                <Typography sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {daily.streak}
                </Typography>
              </Stack>
              <Typography variant="caption" sx={{ color: 'text.disabled' }}>
                {t('daily.resetsIn', { time: formatClock(remaining) })}
              </Typography>
            </Stack>
          </Tooltip>

          <Button component={Link} to={problemPath(daily.slug)} variant={daily.solved ? 'outlined' : 'contained'}>
            {t(daily.solved ? 'daily.solved' : 'daily.solve')}
          </Button>
        </Stack>
      </Stack>

      <Popover
        open={anchor !== null}
        anchorEl={anchor}
        onClose={() => {
          setAnchor(null);
          setOffset(0);
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        {month ? (
          <MonthGrid
            month={month}
            onNavigate={(delta) => {
              setOffset((current) => Math.min(0, current + delta));
            }}
          />
        ) : null}
      </Popover>
    </Paper>
  );
};
