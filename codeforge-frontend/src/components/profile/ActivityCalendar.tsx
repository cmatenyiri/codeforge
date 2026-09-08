import { Box, MenuItem, Select, Stack, Tooltip, Typography } from '@mui/material';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { type ActivityCalendar as ActivityCalendarData } from '../../api/types';

/**
 * Submission counts mapped onto five steps.
 *
 * <p>Fixed thresholds rather than quantiles of the person's own history. A
 * calendar that normalised per user would paint somebody's two-submission
 * Tuesday the same dark green as somebody else's thirty, and the whole point of
 * the picture is that it reads the same for everybody.
 */
const level = (count: number): 0 | 1 | 2 | 3 | 4 => {
  if (count === 0) return 0;
  if (count < 3) return 1;
  if (count < 6) return 2;
  if (count < 12) return 3;
  return 4;
};

/** Alpha ramp over the accepted-verdict green, so the calendar sits in the existing palette. */
const LEVEL_ALPHA = [0, 0.22, 0.42, 0.66, 1] as const;

const isoOf = (date: Date) => date.toISOString().slice(0, 10);

/** Sentinel for the rolling window, since a Select cannot hold `undefined`. */
const CURRENT = 'current';

/**
 * The contribution heatmap, for one calendar year.
 *
 * <p>Answers one question — do they keep at this? — which no total on the page
 * can. Fifty solves in one weekend and fifty over a year are the same number and
 * completely different facts about somebody.
 *
 * <p>A whole calendar year rather than a rolling window, because the picker
 * beside the heading selects years: the counters, the heading and the grid all
 * have to be describing the same span, or "total active days" under a 2025 grid
 * would quietly be counting 2026 as well.
 *
 * <p>Built on a UTC date grid so the columns line up with the buckets the server
 * grouped by; re-bucketing per viewer would move somebody's streak when they
 * travelled.
 */
export const ActivityCalendar = ({
  calendar,
  years,
  onYearChange,
  loading,
}: {
  calendar: ActivityCalendarData;
  /** Only years with activity — a picker that can select an empty grid can be wrong. */
  years: number[];
  /** undefined selects the rolling twelve months. */
  onYearChange: (year: number | undefined) => void;
  loading: boolean;
}) => {
  const { t, i18n } = useTranslation();

  const { weeks, monthLabels } = useMemo(() => {
    const counts = new Map(calendar.days.map((day) => [day.date, day.submissions]));

    // The window's own bounds, as the server drew them: a calendar year runs
    // January to December, the rolling one ends today. Reading them off the
    // response rather than recomputing here is what keeps the grid, the
    // counters and the heading describing the same span.
    const first = new Date(`${calendar.from}T00:00:00Z`);
    const last = new Date(`${calendar.to}T00:00:00Z`);

    // Whole weeks, so the grid has no ragged edge.
    const start = new Date(first);
    start.setUTCDate(start.getUTCDate() - start.getUTCDay());
    const end = new Date(last);
    end.setUTCDate(end.getUTCDate() + (6 - end.getUTCDay()));

    const built: { date: string; count: number; inWindow: boolean }[][] = [];
    const labels: { week: number; label: string }[] = [];
    const cursor = new Date(start);
    let lastMonth = -1;
    let week = 0;

    while (cursor <= end) {
      const column: { date: string; count: number; inWindow: boolean }[] = [];

      for (let day = 0; day < 7; day++) {
        const iso = isoOf(cursor);
        // Days outside the window pad the first and last columns; they keep the
        // grid rectangular and are drawn as gaps.
        const inWindow = cursor >= first && cursor <= last;
        column.push({ date: iso, count: counts.get(iso) ?? 0, inWindow });

        // One label per month, at the column it first appears in, and never
        // within three columns of the previous one — a window that starts
        // mid-month would otherwise print two labels 14px apart and draw them
        // on top of each other.
        if (day === 0 && inWindow && cursor.getUTCMonth() !== lastMonth) {
          lastMonth = cursor.getUTCMonth();
          const previous = labels.at(-1);
          if (previous === undefined || week - previous.week >= 3) {
            labels.push({
              week,
              label: cursor.toLocaleDateString(i18n.language, { month: 'short', timeZone: 'UTC' }),
            });
          }
        }
        cursor.setUTCDate(cursor.getUTCDate() + 1);
      }
      built.push(column);
      week++;
    }

    return { weeks: built, monthLabels: labels };
  }, [calendar, i18n.language]);

  return (
    <Box>
      {/* The header LeetCode puts above the grid: how much, how often, and the
          longest run — all three describing the selected year. */}
      <Stack
        direction="row"
        spacing={2}
        sx={{ alignItems: 'baseline', justifyContent: 'space-between', mb: 1.5, flexWrap: 'wrap' }}
        useFlexGap
      >
        <Typography variant="h4">
          {calendar.year === undefined
            ? t('publicProfile.submissionsPastYear', { count: calendar.submissions })
            : t('publicProfile.submissionsInYear', { count: calendar.submissions, year: calendar.year })}
        </Typography>

        <Stack direction="row" spacing={2.5} sx={{ alignItems: 'center' }} useFlexGap>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('publicProfile.totalActiveDays')}{' '}
            <Box component="span" sx={{ color: 'text.primary', fontWeight: 600 }}>
              {calendar.activeDays}
            </Box>
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('publicProfile.maxStreakLabel')}{' '}
            <Box component="span" sx={{ color: 'text.primary', fontWeight: 600 }}>
              {calendar.maxStreak}
            </Box>
          </Typography>

          {/* "Current" is the rolling twelve months and the default: on the
              second of January a calendar year would report two active days,
              which is a true number and a useless one. The specific years are
              there for looking back. */}
          <Select
            size="small"
            value={calendar.year ?? CURRENT}
            onChange={(event) => {
              const value = event.target.value;
              onYearChange(value === CURRENT ? undefined : Number(value));
            }}
            disabled={loading}
            inputProps={{ 'aria-label': t('publicProfile.selectYear') }}
            sx={{ minWidth: 108 }}
          >
            <MenuItem value={CURRENT}>{t('publicProfile.currentWindow')}</MenuItem>
            {years.map((year) => (
              <MenuItem key={year} value={year}>
                {year}
              </MenuItem>
            ))}
          </Select>
        </Stack>
      </Stack>

      <Box sx={{ overflowX: 'auto', pb: 0.5, opacity: loading ? 0.5 : 1, transition: 'opacity 120ms' }}>
        <Box sx={{ minWidth: weeks.length * 14 }}>
          <Box sx={{ position: 'relative', height: 16, mb: 0.25 }}>
            {monthLabels.map((label) => (
              <Typography
                key={`${label.week}-${label.label}`}
                variant="caption"
                sx={{ position: 'absolute', left: label.week * 14, color: 'text.disabled' }}
              >
                {label.label}
              </Typography>
            ))}
          </Box>

          <Stack direction="row" spacing="2px">
            {weeks.map((column, index) => (
              <Stack key={index} spacing="2px">
                {column.map((cell) =>
                  !cell.inWindow ? (
                    <Box key={cell.date} sx={{ width: 12, height: 12 }} />
                  ) : (
                    <Tooltip
                      key={cell.date}
                      title={t(cell.count === 0 ? 'publicProfile.activityDayNone' : 'publicProfile.activityDay', {
                        count: cell.count,
                        date: new Date(`${cell.date}T00:00:00Z`).toLocaleDateString(i18n.language, {
                          dateStyle: 'medium',
                          timeZone: 'UTC',
                        }),
                      })}
                    >
                      <Box
                        sx={{
                          width: 12,
                          height: 12,
                          borderRadius: '2px',
                          backgroundColor: (theme) =>
                            cell.count === 0
                              ? theme.vars.palette.surface.sunken
                              : `rgba(61, 214, 140, ${LEVEL_ALPHA[level(cell.count)]})`,
                          border: 1,
                          borderColor: 'border.subtle',
                        }}
                      />
                    </Tooltip>
                  ),
                )}
              </Stack>
            ))}
          </Stack>
        </Box>
      </Box>

      <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', justifyContent: 'flex-end', mt: 1 }}>
        <Typography variant="caption" sx={{ color: 'text.disabled' }}>
          {t('publicProfile.less')}
        </Typography>
        {LEVEL_ALPHA.map((alpha, index) => (
          <Box
            key={index}
            sx={{
              width: 12,
              height: 12,
              borderRadius: '2px',
              backgroundColor: (theme) =>
                alpha === 0 ? theme.vars.palette.surface.sunken : `rgba(61, 214, 140, ${alpha})`,
              border: 1,
              borderColor: 'border.subtle',
            }}
          />
        ))}
        <Typography variant="caption" sx={{ color: 'text.disabled' }}>
          {t('publicProfile.more')}
        </Typography>
      </Stack>
    </Box>
  );
};
