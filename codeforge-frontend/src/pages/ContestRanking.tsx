import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import {
  Alert,
  AlertTitle,
  Box,
  CircularProgress,
  Container,
  Link as MuiLink,
  Pagination,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { contestsApi } from '../api/contests-api';
import { type ContestDetail, type ContestStandings } from '../api/types';
import { useAuth } from '../auth/use-auth';
import { StandingsTable } from '../components/contest/StandingsTable';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { contestPath } from '../routes/paths';

const PAGE_SIZE = 25;

/** While a contest is live the board moves; once it is over it never does again. */
const LIVE_POLL_INTERVAL_MS = 20_000;

/**
 * The scoreboard.
 *
 * <p>The caller's own row is pinned above the table as well as highlighted
 * inside it, because somebody in 812th place opens this page to find themselves
 * and paging through eight screens for it would be a strange thing to ask.
 */
export const ContestRankingPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { slug } = useParams<{ slug: string }>();
  const { user } = useAuth();

  const [contest, setContest] = useState<ContestDetail | null>(null);
  const [standings, setStandings] = useState<ContestStandings | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    (initial: boolean) => {
      if (slug === undefined) {
        return;
      }
      if (initial) {
        setLoading(true);
      }

      Promise.all([contestsApi.detail(slug), contestsApi.standings(slug, page, PAGE_SIZE)])
        .then(([detail, board]) => {
          setContest(detail);
          setStandings(board);
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
    [slug, page, message],
  );

  useEffect(() => {
    load(true);
  }, [load]);

  useEffect(() => {
    if (contest?.status !== 'RUNNING') {
      return;
    }
    const timer = setInterval(() => {
      load(false);
    }, LIVE_POLL_INTERVAL_MS);

    return () => {
      clearInterval(timer);
    };
  }, [contest?.status, load]);

  // Only once the ratings have actually landed; before that the column would be
  // a page of dashes promising something that has not happened.
  const showRatingDelta = standings?.rated === true && contest?.status === 'FINALIZED';

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            {slug === undefined ? null : (
              <MuiLink
                component={Link}
                to={contestPath(slug)}
                sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2', mb: 1.5 }}
              >
                <ArrowBackRounded sx={{ fontSize: 16 }} />
                {t('contest.backToContest')}
              </MuiLink>
            )}
            <Typography variant="h1">{t('contest.standings')}</Typography>
            {contest ? (
              <Typography variant="body1" sx={{ color: 'text.secondary', mt: 0.5 }}>
                {contest.title}
              </Typography>
            ) : null}
          </Box>

          {error ? <Alert severity="error">{error}</Alert> : null}

          {standings && !standings.rated ? (
            <Alert severity="warning">
              <AlertTitle>{t('contest.unratedTitle')}</AlertTitle>
              {standings.unratedReason ?? t('contest.unratedBody')}
            </Alert>
          ) : null}

          {loading ? (
            <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : standings === null || contest === null ? null : standings.page.totalElements === 0 ? (
            <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
              <Typography variant="h4">{t('contest.standingsEmpty')}</Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
                {t('contest.standingsEmptyBody')}
              </Typography>
            </Paper>
          ) : (
            <>
              <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                {t(standings.finalised ? 'contest.standingsFinal' : 'contest.standingsLive')}
              </Typography>

              {standings.me ? (
                <Paper variant="outlined" sx={{ p: 0, borderColor: 'primary.main' }}>
                  <StandingsTable
                    rows={[standings.me]}
                    problems={contest.problems}
                    currentUserId={user?.id}
                    showRatingDelta={showRatingDelta}
                  />
                </Paper>
              ) : (
                <Alert severity="info">{t('contest.notRanked')}</Alert>
              )}

              <Paper variant="outlined">
                <StandingsTable
                  rows={standings.page.content}
                  problems={contest.problems}
                  currentUserId={user?.id}
                  showRatingDelta={showRatingDelta}
                />
              </Paper>

              {standings.page.totalPages > 1 ? (
                <Stack sx={{ alignItems: 'center' }}>
                  <Pagination
                    count={standings.page.totalPages}
                    page={page + 1}
                    onChange={(_, value) => {
                      setPage(value - 1);
                    }}
                  />
                </Stack>
              ) : null}
            </>
          )}
        </Stack>
      </Container>
    </Box>
  );
};
