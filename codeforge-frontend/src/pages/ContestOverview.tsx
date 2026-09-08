import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import EditRounded from '@mui/icons-material/EditRounded';
import GroupsRounded from '@mui/icons-material/GroupsRounded';
import LockRounded from '@mui/icons-material/LockRounded';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Link as MuiLink,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { contestsApi } from '../api/contests-api';
import { type ContestDetail } from '../api/types';
import { useAuth } from '../auth/use-auth';
import { ContestCountdown } from '../components/contest/ContestCountdown';
import { ContestStatusChip } from '../components/contest/ContestStatusChip';
import { RatingDelta } from '../components/contest/RatingDelta';
import { CONTEST_TYPE_LABEL_KEY, formatContestTime } from '../components/contest/contest';
import { useContestClock } from '../components/contest/use-contest-clock';
import { AppHeader } from '../components/layout/AppHeader';
import { DifficultyChip } from '../components/problems/DifficultyChip';
import { MarkdownBody } from '../components/solve/MarkdownBody';
import { useMessages } from '../i18n/use-messages';
import { contestProblemPath, contestRankingPath, paths } from '../routes/paths';

/** Fast enough that the page turns over within seconds of the contest starting. */
const POLL_INTERVAL_MS = 15_000;

/**
 * A contest's own page: the announcement before it starts, the way in while it
 * runs, and the result afterwards.
 *
 * <p>Polled, and for one reason: this is the page people sit on waiting for the
 * clock. When it reaches zero the server starts serving the problems, and the
 * page has to notice without being reloaded.
 */
export const ContestOverviewPage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();
  const { slug } = useParams<{ slug: string }>();
  const { user } = useAuth();

  const [contest, setContest] = useState<ContestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(
    (initial: boolean) => {
      if (slug === undefined) {
        return;
      }
      if (initial) {
        setLoading(true);
      }

      contestsApi
        .detail(slug)
        .then((data) => {
          setContest(data);
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
    [slug, message],
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

  const scheduled = contest?.status === 'SCHEDULED';
  const running = contest?.status === 'RUNNING';
  const untilStart = useContestClock(contest?.secondsUntilStart, scheduled);
  const remaining = useContestClock(contest?.remainingSeconds, running);

  const toggleRegistration = useCallback(() => {
    if (contest === null || slug === undefined) {
      return;
    }
    setBusy(true);

    const call = contest.registered ? contestsApi.unregister(slug) : contestsApi.register(slug);
    call
      .then(() => {
        load(false);
      })
      .catch((caught: unknown) => {
        const apiError = toApiError(caught);
        setError(message(apiError.code, apiError.message));
      })
      .finally(() => {
        setBusy(false);
      });
  }, [contest, slug, load, message]);

  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Box sx={{ display: 'grid', placeItems: 'center', py: 10 }}>
          <CircularProgress />
        </Box>
      </Box>
    );
  }

  if (contest === null) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Container maxWidth="md" sx={{ py: 5 }}>
          <Alert severity="error">{error ?? t('error.internal')}</Alert>
        </Container>
      </Box>
    );
  }

  const started = contest.status !== 'SCHEDULED' && contest.status !== 'DRAFT';
  const over = contest.status === 'ENDED' || contest.status === 'FINALIZED';

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            <Stack direction="row" spacing={2} sx={{ mb: 1.5, alignItems: 'center' }}>
              <MuiLink
                component={Link}
                to={paths.contests}
                sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2' }}
              >
                <ArrowBackRounded sx={{ fontSize: 16 }} />
                {t('contest.backToContests')}
              </MuiLink>

              {/* The shortest path from "Q3 is broken" to fixing it. */}
              {user?.role === 'ADMIN' ? (
                <MuiLink
                  component={Link}
                  to={paths.adminContests}
                  sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2' }}
                >
                  <EditRounded sx={{ fontSize: 16 }} />
                  {t('admin.contest.title')}
                </MuiLink>
              ) : null}
            </Stack>

            <Stack
              direction={{ xs: 'column', md: 'row' }}
              spacing={2}
              sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
            >
              <Box>
                <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                  <Typography variant="h1">{contest.title}</Typography>
                  <ContestStatusChip status={contest.status} />
                  <Chip label={t(CONTEST_TYPE_LABEL_KEY[contest.type])} variant="outlined" />
                  <Chip label={t(contest.rated ? 'contest.rated' : 'contest.unrated')} />
                </Stack>

                <Stack
                  direction="row"
                  spacing={2}
                  useFlexGap
                  sx={{ flexWrap: 'wrap', color: 'text.disabled', mt: 1 }}
                >
                  <Typography variant="body2">
                    {new Date(contest.startsAt).toLocaleString(i18n.language, {
                      dateStyle: 'full',
                      timeStyle: 'short',
                    })}
                  </Typography>
                  <Typography variant="body2">
                    {t('contest.duration', { count: contest.durationMinutes })}
                  </Typography>
                  <Typography variant="body2">
                    {t('contest.totalPoints', { count: contest.totalPoints })}
                  </Typography>
                  <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                    <GroupsRounded sx={{ fontSize: 14 }} />
                    <Typography variant="body2">
                      {over
                        ? t('contest.participants', { count: contest.participantCount })
                        : t('contest.registrations', { count: contest.registrationCount })}
                    </Typography>
                  </Stack>
                </Stack>
              </Box>

              <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                {scheduled ? (
                  <ContestCountdown seconds={untilStart} mode="until" durationMinutes={contest.durationMinutes} />
                ) : null}
                {running ? (
                  <ContestCountdown seconds={remaining} mode="remaining" durationMinutes={contest.durationMinutes} />
                ) : null}

                {over ? null : contest.registered ? (
                  <Button variant="outlined" onClick={toggleRegistration} disabled={busy || started}>
                    {t(started ? 'contest.registered' : 'contest.unregister')}
                  </Button>
                ) : (
                  <Button onClick={toggleRegistration} disabled={busy}>
                    {t(busy ? 'contest.registering' : 'contest.register')}
                  </Button>
                )}

                <Button component={Link} to={contestRankingPath(contest.slug)} variant="outlined">
                  {t('contest.standings')}
                </Button>
              </Stack>
            </Stack>
          </Box>

          {error ? <Alert severity="error">{error}</Alert> : null}

          {contest.rated ? null : (
            <Alert severity="warning">
              <AlertTitle>{t('contest.unratedTitle')}</AlertTitle>
              {contest.unratedReason ?? t('contest.unratedBody')}
            </Alert>
          )}

          {contest.status === 'ENDED' && contest.rated ? (
            <Alert severity="info">
              <AlertTitle>{t('contest.ratingPendingTitle')}</AlertTitle>
              {t('contest.ratingPendingBody')}
            </Alert>
          ) : null}

          {/* Somebody's own result is what they came back for, so it sits above
              the problem list rather than behind the standings link. */}
          {contest.myResult ? (
            <Paper variant="outlined" sx={{ p: 2.5 }}>
              <Typography variant="overline" sx={{ color: 'text.disabled' }}>
                {t('contest.myResult')}
              </Typography>
              <Stack direction="row" spacing={4} sx={{ mt: 1, flexWrap: 'wrap' }} useFlexGap>
                <Box>
                  <Typography variant="metric">#{contest.myResult.rank}</Typography>
                  <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                    {t('contest.rank')}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="metric">{contest.myResult.score}</Typography>
                  <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                    {t('contest.score')}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="metric">
                    {formatContestTime(contest.myResult.totalTimeSeconds)}
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                    {t('contest.totalTime')}
                  </Typography>
                </Box>
                {contest.myResult.ratingDelta === undefined ? null : (
                  <Box>
                    <RatingDelta delta={contest.myResult.ratingDelta} size="metric" />
                    <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                      {t('contest.yourRating')}
                    </Typography>
                  </Box>
                )}
              </Stack>
            </Paper>
          ) : null}

          {contest.description ? (
            <Paper variant="outlined" sx={{ p: 2.5 }}>
              <MarkdownBody>{contest.description}</MarkdownBody>
            </Paper>
          ) : null}

          <Stack spacing={1.5}>
            <Typography variant="h3">
              {t('contest.problemCount', { count: contest.problems.length })}
            </Typography>

            {started ? null : (
              <Alert severity="info" icon={<LockRounded fontSize="small" />}>
                <AlertTitle>{t('contest.problemsLocked')}</AlertTitle>
                {t('contest.problemsLockedBody')}
              </Alert>
            )}

            {/* Started, but the server has still sent no titles: the caller has
                not entered. Reading the problems is competing, so the way in is
                to register — which is a commitment made before seeing them. */}
            {started && !over && !contest.registered ? (
              <Alert
                severity="warning"
                icon={<LockRounded fontSize="small" />}
                action={
                  <Button size="small" onClick={toggleRegistration} disabled={busy}>
                    {t('contest.register')}
                  </Button>
                }
              >
                <AlertTitle>{t('contest.registerToView')}</AlertTitle>
                {t('contest.registerToViewBody')}
              </Alert>
            ) : null}

            {contest.problems.map((problem) => (
              <Paper key={problem.position} variant="outlined" sx={{ p: 2 }}>
                <Stack
                  direction="row"
                  spacing={2}
                  sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                  <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                    <Typography variant="overline" sx={{ color: 'text.disabled', minWidth: 28 }}>
                      {problem.label}
                    </Typography>

                    {/* Blank until the contest starts — the server does not send
                        the title, so there is nothing here to read early. */}
                    {problem.title === undefined ? (
                      <Box
                        sx={{
                          width: 180,
                          height: 18,
                          borderRadius: 0.5,
                          backgroundColor: 'surface.sunken',
                        }}
                      />
                    ) : (
                      <Typography variant="h4">{problem.title}</Typography>
                    )}

                    {problem.difficulty ? <DifficultyChip difficulty={problem.difficulty} size="small" /> : null}
                    <Chip label={t('contest.points', { count: problem.points })} size="small" variant="outlined" />
                    {problem.solved ? (
                      <Chip
                        icon={<CheckCircleRounded />}
                        label={t('problems.solved')}
                        size="small"
                        sx={{
                          color: 'verdict.accepted',
                          backgroundColor: 'verdict.acceptedBg',
                          '& .MuiChip-icon': { color: 'inherit' },
                        }}
                      />
                    ) : null}
                  </Stack>

                  <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                    {problem.solveCount === undefined ? null : (
                      <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                        {t('contest.solvedBy', { count: problem.solveCount })}
                      </Typography>
                    )}
                    <Button
                      component={Link}
                      to={contestProblemPath(contest.slug, problem.position)}
                      disabled={!started || (running && !contest.registered)}
                      variant={running ? 'contained' : 'outlined'}
                      size="small"
                    >
                      {t(running ? 'contest.enter' : 'contest.viewProblems')}
                    </Button>
                  </Stack>
                </Stack>
              </Paper>
            ))}
          </Stack>
        </Stack>
      </Container>
    </Box>
  );
};
