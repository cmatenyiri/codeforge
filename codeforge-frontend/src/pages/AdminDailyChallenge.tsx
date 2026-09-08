import ChevronLeftRounded from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi } from '../api/admin-api';
import { toApiError } from '../api/api-error';
import { dailyApi } from '../api/daily-api';
import { type AdminProblemSummary, type DailyCalendarDay, type DailyCalendarMonth } from '../api/types';
import { AppHeader } from '../components/layout/AppHeader';
import { DifficultyChip } from '../components/problems/DifficultyChip';
import { useMessages } from '../i18n/use-messages';

const WEEKDAY_COLUMNS = 7;

/**
 * Curating the problem of the day.
 *
 * <p>The rotation fills every date on its own, so this screen is an override
 * rather than a chore: an author only touches the days they care about, and the
 * rest are already decided. That is why past days are read-only and future days
 * are editable — the rotation's choice for a day that has happened is part of
 * the record, and people's streaks were earned against it.
 */
export const AdminDailyChallengePage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();

  const [offset, setOffset] = useState(0);
  const [month, setMonth] = useState<DailyCalendarMonth | null>(null);
  const [problems, setProblems] = useState<AdminProblemSummary[]>([]);
  const [editing, setEditing] = useState<DailyCalendarDay | null>(null);
  const [choice, setChoice] = useState<AdminProblemSummary | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const today = new Date();
  const viewing = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth() + offset, 1));

  const load = useCallback(() => {
    const target = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth() + offset, 1));

    dailyApi
      .calendar(target.getUTCFullYear(), target.getUTCMonth() + 1)
      .then(setMonth)
      .catch((caught: unknown) => {
        setError(message(toApiError(caught).code, toApiError(caught).message));
      });
    // `today` is derived per render but only its month matters, and that cannot
    // change between renders of this screen.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [offset, message]);

  useEffect(load, [load]);

  useEffect(() => {
    adminApi
      .list({ size: 100, sort: 'id', order: 'asc', state: 'PUBLISHED' })
      .then((page) => {
        setProblems(page.content.filter((problem) => problem.solvable));
      })
      .catch(() => {
        // The picker degrades to empty; the rotation still fills every day.
      });
  }, []);

  const save = () => {
    if (editing === null || choice === null) {
      return;
    }
    setBusy(true);
    setError(null);

    dailyApi
      .pin(editing.date, choice.id)
      .then(() => {
        setEditing(null);
        setChoice(null);
        load();
      })
      .catch((caught: unknown) => {
        const apiError = toApiError(caught);
        setError(message(apiError.code, apiError.message));
      })
      .finally(() => {
        setBusy(false);
      });
  };

  const release = (day: DailyCalendarDay) => {
    setBusy(true);
    dailyApi
      .unpin(day.date)
      .then(() => {
        setEditing(null);
        load();
      })
      .catch((caught: unknown) => {
        const apiError = toApiError(caught);
        setError(message(apiError.code, apiError.message));
      })
      .finally(() => {
        setBusy(false);
      });
  };

  const todayIso = new Date().toISOString().slice(0, 10);
  const leading = new Date(`${month?.days[0]?.date ?? todayIso}T00:00:00Z`).getUTCDay();

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="md" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">{t('admin.daily.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('admin.daily.subtitle')}
            </Typography>
          </Box>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
              <IconButton aria-label={t('daily.previousMonth')} onClick={() => { setOffset((o) => o - 1); }}>
                <ChevronLeftRounded />
              </IconButton>
              <Typography variant="h4">
                {viewing.toLocaleDateString(i18n.language, { month: 'long', year: 'numeric', timeZone: 'UTC' })}
              </Typography>
              <IconButton aria-label={t('daily.nextMonth')} onClick={() => { setOffset((o) => o + 1); }}>
                <ChevronRightRounded />
              </IconButton>
            </Stack>

            {month === null ? (
              <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
                <CircularProgress />
              </Box>
            ) : (
              <Box sx={{ display: 'grid', gridTemplateColumns: `repeat(${WEEKDAY_COLUMNS}, 1fr)`, gap: 1 }}>
                {Array.from({ length: WEEKDAY_COLUMNS }, (_, index) => (
                  <Typography key={index} variant="caption" align="center" sx={{ color: 'text.disabled' }}>
                    {new Date(Date.UTC(2024, 0, 7 + index)).toLocaleDateString(i18n.language, {
                      weekday: 'short',
                      timeZone: 'UTC',
                    })}
                  </Typography>
                ))}

                {Array.from({ length: leading }, (_, index) => (
                  <Box key={`pad-${index}`} />
                ))}

                {month.days.map((day) => {
                  // A day keeps what it ran with from the moment it starts —
                  // today included. Swapping today's problem would rewrite who
                  // is considered to have solved "the daily", both revoking and
                  // fabricating streak days, so only the future is editable.
                  const editable = day.date > todayIso;

                  // Three states, not two. "This day has started" is true of
                  // today and nonsense about a Tuesday three weeks ago, so a
                  // finished day says it is over and today says it is running.
                  const tooltip = editable
                    ? t('admin.daily.pin')
                    : day.date === todayIso
                      ? t('admin.daily.today')
                      : t('admin.daily.settled');

                  return (
                    <Tooltip key={day.date} title={tooltip}>
                      <Box
                        onClick={() => {
                          if (editable) {
                            setEditing(day);
                            setChoice(null);
                          }
                        }}
                        sx={{
                          minHeight: 78,
                          p: 1,
                          borderRadius: 1,
                          border: 1,
                          borderColor: day.today ? 'primary.main' : 'border.subtle',
                          backgroundColor: editable ? 'surface.raised' : 'surface.sunken',
                          cursor: editable ? 'pointer' : 'default',
                          opacity: editable ? 1 : 0.6,
                          '&:hover': editable ? { borderColor: 'primary.main' } : undefined,
                        }}
                      >
                        <Typography variant="caption" sx={{ fontWeight: day.today ? 700 : 400 }}>
                          {Number(day.date.slice(-2))}
                        </Typography>
                        {day.title ? (
                          <Typography
                            variant="caption"
                            sx={{ display: 'block', mt: 0.5, lineHeight: 1.25, color: 'text.secondary' }}
                          >
                            {day.title}
                          </Typography>
                        ) : (
                          <Typography variant="caption" sx={{ display: 'block', mt: 0.5, color: 'text.disabled' }}>
                            {t('daily.notYet')}
                          </Typography>
                        )}
                      </Box>
                    </Tooltip>
                  );
                })}
              </Box>
            )}
          </Paper>

          <Alert severity="info">{t('admin.daily.rotationHint')}</Alert>
        </Stack>
      </Container>

      <Dialog open={editing !== null} onClose={() => { setEditing(null); }} maxWidth="sm" fullWidth>
        <DialogTitle>{editing?.date}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <Autocomplete
              options={problems}
              getOptionLabel={(option) => option.title}
              value={choice}
              onChange={(_, option) => {
                setChoice(option);
              }}
              renderOption={(props, option) => (
                <Box component="li" {...props} key={option.id}>
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', width: '100%' }}>
                    <Box sx={{ flex: 1, minWidth: 0 }}>{option.title}</Box>
                    <DifficultyChip difficulty={option.difficulty} size="small" />
                  </Stack>
                </Box>
              )}
              renderInput={(params) => <TextField {...params} label={t('admin.daily.pickProblem')} />}
            />
            {editing?.title ? (
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {t('admin.daily.currently', { title: editing.title })}
              </Typography>
            ) : null}
          </Stack>
        </DialogContent>
        <DialogActions>
          {editing?.title ? (
            <Button
              variant="text"
              disabled={busy}
              onClick={() => {
                if (editing) {
                  release(editing);
                }
              }}
            >
              {t('admin.daily.unpin')}
            </Button>
          ) : null}
          <Box sx={{ flex: 1 }} />
          <Button variant="text" onClick={() => { setEditing(null); }}>
            {t('common.cancel')}
          </Button>
          <Button onClick={save} disabled={busy || choice === null}>
            {t('common.confirm')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
