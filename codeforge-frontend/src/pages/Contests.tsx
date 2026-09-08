import { Alert, Box, CircularProgress, Container, Divider, Paper, Stack, Typography } from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toApiError } from '../api/api-error';
import { contestsApi } from '../api/contests-api';
import { type ContestSummary } from '../api/types';
import { ContestCard } from '../components/contest/ContestCard';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';

/** How often the lobby re-reads the clock while something is imminent or live. */
const POLL_INTERVAL_MS = 30_000;

const EmptyState = ({ title, body }: { title: string; body: string }) => (
  <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
    <Typography variant="h4">{title}</Typography>
    <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
      {body}
    </Typography>
  </Paper>
);

/**
 * The contest lobby: what is on now, what is next, and what has already run.
 *
 * <p>Polled rather than loaded once. A contest starting is a moment, not an
 * event the client is told about, and somebody sitting on this page waiting for
 * ten o'clock should see the card turn into "Enter" without reaching for reload.
 */
export const ContestsPage = () => {
  const { t } = useTranslation();
  const message = useMessages();

  const [upcoming, setUpcoming] = useState<ContestSummary[]>([]);
  const [past, setPast] = useState<ContestSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [registering, setRegistering] = useState<string | null>(null);

  const load = useCallback(
    (initial: boolean) => {
      if (initial) {
        setLoading(true);
      }

      Promise.all([contestsApi.upcoming(), contestsApi.list(0, 20)])
        .then(([next, page]) => {
          setUpcoming(next);
          // The upcoming rail already leads with anything live or imminent, so
          // the list below is the archive — showing a contest twice would make
          // the page read as though there were two.
          const shown = new Set(next.map((contest) => contest.id));
          setPast(page.content.filter((contest) => !shown.has(contest.id)));
          setError(null);
        })
        .catch((caught: unknown) => {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
        })
        .finally(() => {
          setLoading(false);
        });
    },
    [message],
  );

  useEffect(() => {
    load(true);
  }, [load]);

  useEffect(() => {
    const timer = setInterval(() => {
      load(false);
    }, POLL_INTERVAL_MS);

    return () => {
      clearInterval(timer);
    };
  }, [load]);

  const register = useCallback(
    (contest: ContestSummary) => {
      setRegistering(contest.slug);
      contestsApi
        .register(contest.slug)
        .then(() => {
          load(false);
        })
        .catch((caught: unknown) => {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
        })
        .finally(() => {
          setRegistering(null);
        });
    },
    [load, message],
  );

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={4}>
          <Box>
            <Typography variant="h1">{t('contest.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('contest.subtitle')}
            </Typography>
          </Box>

          {error ? <Alert severity="error">{error}</Alert> : null}

          {loading ? (
            <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : (
            <>
              <Stack spacing={2}>
                <Typography variant="h3">{t('contest.upcoming')}</Typography>
                {upcoming.length === 0 ? (
                  <EmptyState title={t('contest.noUpcoming')} body={t('contest.noUpcomingBody')} />
                ) : (
                  upcoming.map((contest) => (
                    <ContestCard
                      key={contest.id}
                      contest={contest}
                      onRegister={register}
                      registering={registering === contest.slug}
                    />
                  ))
                )}
              </Stack>

              <Divider />

              <Stack spacing={2}>
                <Typography variant="h3">{t('contest.past')}</Typography>
                {past.length === 0 ? (
                  <EmptyState title={t('contest.noPast')} body={t('contest.noPastBody')} />
                ) : (
                  past.map((contest) => <ContestCard key={contest.id} contest={contest} />)
                )}
              </Stack>

              <Paper variant="outlined" sx={{ p: 2.5 }}>
                <Typography variant="h4" sx={{ mb: 1 }}>
                  {t('contest.rulesTitle')}
                </Typography>
                <Stack component="ul" spacing={0.75} sx={{ m: 0, pl: 2.5, color: 'text.secondary' }}>
                  {(
                    ['rulesClock', 'rulesScore', 'rulesPenalty', 'rulesFrozen', 'rulesRating'] as const
                  ).map((key) => (
                    <Typography key={key} component="li" variant="body2">
                      {t(`contest.${key}`)}
                    </Typography>
                  ))}
                </Stack>
              </Paper>
            </>
          )}
        </Stack>
      </Container>
    </Box>
  );
};
