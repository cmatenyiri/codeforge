import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import LeaderboardRounded from '@mui/icons-material/LeaderboardRounded';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  CircularProgress,
  Container,
  Divider,
  Link as MuiLink,
  Stack,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { contestsApi } from '../api/contests-api';
import { type ContestDetail, type ContestProblemDetail } from '../api/types';
import { ContestCountdown } from '../components/contest/ContestCountdown';
import { ContestEditorPanel } from '../components/contest/ContestEditorPanel';
import { ContestProblemNav } from '../components/contest/ContestProblemNav';
import { ContestProblemPanel } from '../components/contest/ContestProblemPanel';
import { useContestClock } from '../components/contest/use-contest-clock';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { contestPath, contestProblemPath, contestRankingPath } from '../routes/paths';

/**
 * How often the header re-reads the contest.
 *
 * <p>Slower than the overview page's poll: this one is loaded while somebody is
 * actually working, and the only things it refreshes are the clock anchor and
 * the solved ticks in the tab strip.
 */
const POLL_INTERVAL_MS = 30_000;

/**
 * The contest workspace: question on the left, editor on the right, clock above.
 *
 * <p>All four questions stay reachable throughout — unlike the mock interview,
 * which is deliberately sequential. Choosing which problem to spend the last
 * twenty minutes on is most of what a contest measures, and a UI that made that
 * choice for you would be measuring something else.
 */
export const ContestArenaPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();
  const { slug, position } = useParams<{ slug: string; position: string }>();
  const activePosition = Number(position ?? 0);

  const [contest, setContest] = useState<ContestDetail | null>(null);
  const [problem, setProblem] = useState<ContestProblemDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => {
    setRefreshKey((key) => key + 1);
  }, []);

  // The contest itself: the clock, and the solved state of every question.
  useEffect(() => {
    if (slug === undefined) {
      return;
    }
    let cancelled = false;

    contestsApi
      .detail(slug)
      .then((data) => {
        if (!cancelled) {
          setContest(data);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [slug, refreshKey, message]);

  useEffect(() => {
    const timer = setInterval(refresh, POLL_INTERVAL_MS);

    return () => {
      clearInterval(timer);
    };
  }, [refresh]);

  // The question on screen, refetched when the tab changes or a verdict lands.
  useEffect(() => {
    if (slug === undefined) {
      return;
    }
    let cancelled = false;
    setLoading((current) => current || refreshKey === 0);

    contestsApi
      .problem(slug, activePosition)
      .then((data) => {
        if (!cancelled) {
          setProblem(data);
          setError(null);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
          setProblem(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [slug, activePosition, refreshKey, message]);

  const running = contest?.status === 'RUNNING';
  const remaining = useContestClock(contest?.remainingSeconds, running);

  const selectProblem = useCallback(
    (next: number) => {
      if (slug !== undefined) {
        void navigate(contestProblemPath(slug, next));
      }
    },
    [navigate, slug],
  );

  if (slug === undefined) {
    return null;
  }

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Stack
        direction="row"
        spacing={2}
        sx={{ px: 2, pt: 1.5, alignItems: 'center', justifyContent: 'space-between' }}
      >
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center', minWidth: 0 }}>
          <MuiLink
            component={Link}
            to={contestPath(slug)}
            sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2' }}
          >
            <ArrowBackRounded sx={{ fontSize: 16 }} />
            {t('contest.backToContest')}
          </MuiLink>
          {contest ? (
            <Typography variant="body2" sx={{ color: 'text.disabled', minWidth: 0 }} noWrap>
              {contest.title}
            </Typography>
          ) : null}
        </Stack>

        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          {contest && running ? (
            <ContestCountdown seconds={remaining} mode="remaining" durationMinutes={contest.durationMinutes} />
          ) : null}
          <Button
            component={Link}
            to={contestRankingPath(slug)}
            variant="outlined"
            size="small"
            startIcon={<LeaderboardRounded />}
          >
            {t('contest.standings')}
          </Button>
        </Stack>
      </Stack>

      {contest ? (
        <Box sx={{ px: 2, pt: 1 }}>
          <ContestProblemNav
            problems={contest.problems}
            active={activePosition}
            onSelect={selectProblem}
            started={contest.status !== 'SCHEDULED' && contest.status !== 'DRAFT'}
          />
        </Box>
      ) : null}

      {contest && contest.status !== 'RUNNING' && contest.status !== 'SCHEDULED' ? (
        <Box sx={{ px: 2, pt: 1.5 }}>
          <Alert severity="info">
            <AlertTitle>{t('contest.practiceTitle')}</AlertTitle>
            {t('contest.practiceBody')}
          </Alert>
        </Box>
      ) : null}

      {loading ? (
        <Box sx={{ flex: 1, display: 'grid', placeItems: 'center' }}>
          <CircularProgress />
        </Box>
      ) : error !== null && problem === null ? (
        <Container maxWidth="sm" sx={{ py: 6 }}>
          <Alert severity="info">
            <AlertTitle>{t('contest.notStartedTitle')}</AlertTitle>
            {error}
          </Alert>
        </Container>
      ) : problem ? (
        <Stack
          direction={{ xs: 'column', lg: 'row' }}
          divider={<Divider orientation="vertical" flexItem />}
          sx={{ flex: 1, minHeight: 0, p: 2, pt: 1.5, gap: { xs: 2, lg: 0 } }}
        >
          <Box
            sx={{
              flex: { lg: '1 1 44%' },
              minWidth: 0,
              minHeight: 0,
              border: 1,
              borderColor: 'border.default',
              borderRadius: 1.5,
              backgroundColor: 'surface.paper',
              overflow: 'hidden',
            }}
          >
            <ContestProblemPanel problem={problem} />
          </Box>

          <Box
            sx={{
              flex: { lg: '1 1 56%' },
              minWidth: 0,
              minHeight: 0,
              ml: { lg: 2 },
              border: 1,
              borderColor: 'border.default',
              borderRadius: 1.5,
              backgroundColor: 'surface.paper',
              overflow: 'hidden',
            }}
          >
            <ContestEditorPanel contestSlug={slug} problem={problem} onSubmitted={refresh} />
          </Box>
        </Stack>
      ) : null}
    </Box>
  );
};
