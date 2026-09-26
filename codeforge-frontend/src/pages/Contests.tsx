import {
  Alert,
  Box,
  CircularProgress,
  Container,
  Divider,
  Pagination,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { contestsApi } from '../api/contests-api';
import { type ContestSummary } from '../api/types';
import { ContestCard } from '../components/contest/ContestCard';
import { ContestRulesButton } from '../components/contest/ContestRulesButton';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { type ContestChange, topics } from '../realtime/topics';
import { useContestChanges } from '../realtime/use-topic';

/** A card shows a contest's clock and turnout, not its scoreboard. */
const LOBBY_CHANGES: ContestChange[] = ['STATUS', 'REGISTRATION'];

/** Past contests per page. */
const PAST_PAGE_SIZE = 10;

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
 * <p>Live rather than loaded once. Somebody sitting on this page waiting for ten
 * o'clock should see the card turn into "Enter" without reaching for reload, so
 * the page listens on the lobby topic and re-reads whenever a contest starts,
 * ends, is announced or changes, or gains a registration.
 */
export const ContestsPage = () => {
  const { t } = useTranslation();
  const message = useMessages();

  // The archive page lives in the URL, one-based as it reads, so the back button
  // returns to it and a link to page 4 is a link to page 4.
  const [searchParams, setSearchParams] = useSearchParams();
  const pastPage = Math.max(1, Number(searchParams.get('page')) || 1);
  const pastHeading = useRef<HTMLHeadingElement>(null);

  const [upcoming, setUpcoming] = useState<ContestSummary[]>([]);
  const [past, setPast] = useState<ContestSummary[]>([]);
  const [pastPages, setPastPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [registering, setRegistering] = useState<string | null>(null);

  const load = useCallback(
    (initial: boolean) => {
      if (initial) {
        setLoading(true);
      }

      // The archive is only contests that have ended, asked for as such: the
      // upcoming rail already leads with anything live or imminent, and a page
      // filtered after the fact would be short by however many that was.
      Promise.all([contestsApi.upcoming(), contestsApi.past(pastPage - 1, PAST_PAGE_SIZE)])
        .then(([next, page]) => {
          setUpcoming(next);
          setPast(page.content);
          setPastPages(page.totalPages);
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
    [message, pastPage],
  );

  // The spinner is for the first load only. Turning a page re-reads in place,
  // so the upcoming rail and the heading the page scrolls to stay where they are.
  const loadedOnce = useRef(false);
  useEffect(() => {
    load(!loadedOnce.current);
    loadedOnce.current = true;
  }, [load]);

  useContestChanges(topics.contests, LOBBY_CHANGES, () => {
    load(false);
  });

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
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' } }}
          >
            <Box>
              <Typography variant="h1">{t('contest.title')}</Typography>
              <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
                {t('contest.subtitle')}
              </Typography>
            </Box>
            <ContestRulesButton />
          </Stack>

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
                <Typography ref={pastHeading} variant="h3" sx={{ scrollMarginTop: 80 }}>
                  {t('contest.past')}
                </Typography>
                {past.length === 0 ? (
                  <EmptyState title={t('contest.noPast')} body={t('contest.noPastBody')} />
                ) : (
                  past.map((contest) => <ContestCard key={contest.id} contest={contest} />)
                )}

                {pastPages > 1 ? (
                  <Stack sx={{ alignItems: 'center', pt: 1 }}>
                    <Pagination
                      count={pastPages}
                      page={Math.min(pastPage, pastPages)}
                      onChange={(_, value) => {
                        setSearchParams(value === 1 ? {} : { page: String(value) });
                        pastHeading.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                      }}
                    />
                  </Stack>
                ) : null}
              </Stack>

            </>
          )}
        </Stack>
      </Container>
    </Box>
  );
};
