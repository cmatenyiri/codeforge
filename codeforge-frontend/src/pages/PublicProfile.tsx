import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Container,
  Divider,
  LinearProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { profilesApi } from '../api/profiles-api';
import { type ActivityCalendar as ActivityCalendarData, type PublicProfile } from '../api/types';
import { RatingDelta } from '../components/contest/RatingDelta';
import { formatRating } from '../components/contest/contest';
import { AppHeader } from '../components/layout/AppHeader';
import { DifficultyChip } from '../components/problems/DifficultyChip';
import { DIFFICULTY_TOKEN } from '../components/problems/difficulty';
import { LANGUAGE_LABEL } from '../components/solve/verdict';
import { BadgeList } from '../components/profile/BadgeList';
import { ActivityCalendar } from '../components/profile/ActivityCalendar';
import { SolvedRing } from '../components/profile/SolvedRing';
import { RatingGraph } from '../components/profile/RatingGraph';
import { UserAvatar } from '../components/user/UserAvatar';
import { isAvatarId } from '../components/user/avatars';
import { useMessages } from '../i18n/use-messages';
import { contestPath, problemPath } from '../routes/paths';

/** One number with its label — the unit both summary panels are built from. */
const Metric = ({ label, value, hint }: { label: string; value: string; hint?: string }) => (
  <Box>
    <Typography variant="metric">{value}</Typography>
    <Typography variant="body2" sx={{ color: 'text.disabled' }}>
      {label}
    </Typography>
    {hint ? (
      <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block' }}>
        {hint}
      </Typography>
    ) : null}
  </Box>
);

/**
 * Everything about what they have solved, in one panel.
 *
 * <p>The ring and the three bars sit side by side because they answer the same
 * question at two resolutions — how far through the catalogue, and where that
 * progress is concentrated. Splitting them across cards made the reader join
 * them up by eye, which is work the layout should be doing.
 */
const SolvedPanel = ({ profile }: { profile: PublicProfile }) => {
  const { t } = useTranslation();

  return (
    <Paper variant="outlined" sx={{ p: 2.5, flex: 1, minWidth: 0 }}>
      <Typography variant="h4" sx={{ mb: 2 }}>
        {t('publicProfile.solvedTitle')}
      </Typography>

      <Stack direction="row" spacing={2.5} sx={{ alignItems: 'center' }}>
        <SolvedRing
          solved={profile.solved}
          total={profile.totalProblems}
          label={t('publicProfile.solved')}
          size={112}
        />

        <Stack spacing={1.5} sx={{ flex: 1, width: '100%', minWidth: 0 }}>
          {profile.progress.map((entry) => (
            <Box key={entry.difficulty}>
              <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
                <DifficultyChip difficulty={entry.difficulty} size="small" />
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {entry.solved} / {entry.total}
                </Typography>
              </Stack>
              <LinearProgress
                variant="determinate"
                value={entry.total === 0 ? 0 : (entry.solved / entry.total) * 100}
                sx={{
                  height: 6,
                  borderRadius: 3,
                  '& .MuiLinearProgress-bar': {
                    backgroundColor: `difficulty.${DIFFICULTY_TOKEN[entry.difficulty]}`,
                    borderRadius: 3,
                  },
                }}
              />
            </Box>
          ))}
        </Stack>
      </Stack>

      <Typography variant="body2" sx={{ color: 'text.disabled', mt: 2 }}>
        {t('publicProfile.acceptance')}{' '}
        <Box component="span" sx={{ color: 'text.primary', fontWeight: 600 }}>
          {profile.acceptanceRate === undefined ? '—' : `${(profile.acceptanceRate * 100).toFixed(1)}%`}
        </Box>{' '}
        · {t('publicProfile.ofSubmissions', { count: profile.submissions })}
      </Typography>
    </Paper>
  );
};

/**
 * Everything about how they compete, in one panel: rating, global ranking,
 * contests attended, and the curve those three came from.
 *
 * <p>The graph is inside rather than beside, for the same reason as above — the
 * number and its history are one statement.
 */
const ContestRatingPanel = ({ profile }: { profile: PublicProfile }) => {
  const { t } = useTranslation();

  return (
    <Paper variant="outlined" sx={{ p: 2.5, flex: 1, minWidth: 0 }}>
      <Typography variant="h4" sx={{ mb: 2 }}>
        {t('publicProfile.rating')}
      </Typography>

      {/* Absent rather than 1500 for somebody who has never competed: the
          starting number is an assumption, not a measurement. */}
      {profile.rating === undefined ? (
        <Box>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('publicProfile.noRating')}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.disabled' }}>
            {t('publicProfile.noRatingBody')}
          </Typography>
        </Box>
      ) : (
        <>
          <Stack direction="row" spacing={4} sx={{ flexWrap: 'wrap' }} useFlexGap>
            <Metric
              label={t('publicProfile.ratingLabel')}
              value={formatRating(profile.rating)}
              hint={t('publicProfile.maxRating', { rating: formatRating(profile.maxRating ?? 0) })}
            />
            {profile.ratingRank === undefined ? null : (
              <Metric
                label={t('publicProfile.globalRanking')}
                value={`#${profile.ratingRank}`}
                hint={t('publicProfile.outOf', { total: profile.ratingRankTotal })}
              />
            )}
            <Metric label={t('publicProfile.attended')} value={`${profile.contestsAttended ?? 0}`} />
          </Stack>

          <RatingGraph history={profile.ratingHistory} />
        </>
      )}
    </Paper>
  );
};

/**
 * The left column: who they are, then what they write in.
 *
 * <p>One panel with dividers rather than a stack of cards, because it is all
 * one subject — the person — and three separate boxes for a name, a rank and a
 * language list reads as three unrelated facts.
 */
const IdentityPanel = ({ profile }: { profile: PublicProfile }) => {
  const { t, i18n } = useTranslation();

  return (
    <Paper variant="outlined" sx={{ position: { md: 'sticky' }, top: { md: 88 } }}>
      <Stack spacing={2} sx={{ p: 2.5 }}>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          {isAvatarId(profile.avatar) ? <UserAvatar avatar={profile.avatar} size={64} /> : null}
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h3" noWrap>
              {profile.username}
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.disabled' }}>
              {t('publicProfile.joined', {
                date: new Date(profile.joinedAt).toLocaleDateString(i18n.language, { dateStyle: 'medium' }),
              })}
            </Typography>
          </Box>
        </Stack>

        {/* The global rank, which is the one number a stranger reads first. */}
        {profile.globalRank === undefined ? null : (
          <Box>
            <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
              {t('publicProfile.globalRanking')}
            </Typography>
            <Typography variant="metric">#{profile.globalRank}</Typography>
            <Typography variant="caption" sx={{ color: 'text.disabled' }}>
              {t('publicProfile.outOf', { total: profile.globalRankTotal })}
            </Typography>
          </Box>
        )}
      </Stack>

      {profile.languages.length > 0 ? (
        <>
          <Divider />
          <Stack spacing={1.25} sx={{ p: 2.5 }}>
            <Typography variant="overline" sx={{ color: 'text.disabled' }}>
              {t('publicProfile.languages')}
            </Typography>
            {profile.languages.map((entry) => (
              <Stack
                key={entry.language}
                direction="row"
                spacing={1}
                sx={{ alignItems: 'center', justifyContent: 'space-between' }}
              >
                <Chip label={LANGUAGE_LABEL[entry.language]} size="small" variant="outlined" />
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('publicProfile.problemsSolved', { count: entry.solved })}
                </Typography>
              </Stack>
            ))}
          </Stack>
        </>
      ) : null}
    </Paper>
  );
};

/**
 * Somebody's public profile.
 *
 * <p>Laid out the way the questions are asked: who they are, then what they
 * have solved and how they compete side by side, then how often they turn up,
 * then the detail behind each.
 */
export const PublicProfilePage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();
  const { username } = useParams<{ username: string }>();

  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [calendar, setCalendar] = useState<ActivityCalendarData | null>(null);
  const [loading, setLoading] = useState(true);
  const [calendarLoading, setCalendarLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (username === undefined) {
      return;
    }
    let cancelled = false;
    setLoading(true);

    profilesApi
      .get(username)
      .then((data) => {
        if (!cancelled) {
          setProfile(data);
          setCalendar(data.calendar);
          setError(null);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(caught);
          setError(apiError.status === 404 ? t('publicProfile.notFoundBody') : message(apiError.code, apiError.message));
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
  }, [username, message, t]);

  /** Only the grid is re-fetched: nothing else on the page depends on the year. */
  const changeYear = useCallback(
    (year: number | undefined) => {
      if (username === undefined || year === calendar?.year) {
        return;
      }
      setCalendarLoading(true);

      profilesApi
        .calendar(username, year)
        .then(setCalendar)
        .catch((caught: unknown) => {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
        })
        .finally(() => {
          setCalendarLoading(false);
        });
    },
    [username, calendar?.year, message],
  );

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

  if (profile === null) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Container maxWidth="sm" sx={{ py: 6 }}>
          <Alert severity="info">{error ?? t('publicProfile.notFoundBody')}</Alert>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={3} sx={{ alignItems: 'flex-start' }}>
          {/* Left: the person. One panel, dividers between its sections. */}
          <Box sx={{ width: { xs: '100%', md: 320 }, flexShrink: 0 }}>
            <IdentityPanel profile={profile} />
          </Box>

          {/* Right: what they have done, one panel per subject. */}
          <Stack spacing={3} sx={{ flex: 1, minWidth: 0, width: '100%' }}>
            {/* Side by side once the column is wide enough to take them, which
                keeps the two summary panels within one screen; stacked below
                that, where two half-width cards would be unreadable. */}
            <Stack direction={{ xs: 'column', lg: 'row' }} spacing={3} sx={{ alignItems: 'stretch' }}>
              <SolvedPanel profile={profile} />
              <ContestRatingPanel profile={profile} />
            </Stack>

            <Paper variant="outlined" sx={{ p: 2.5 }}>
              <Typography variant="h4" sx={{ mb: 2 }}>
                {t('publicProfile.badges')}
              </Typography>
              <BadgeList badges={profile.badges} />
            </Paper>

            <Paper variant="outlined" sx={{ p: 2.5 }}>
              {calendar ? (
                <ActivityCalendar
                  calendar={calendar}
                  years={profile.activeYears}
                  onYearChange={changeYear}
                  loading={calendarLoading}
                />
              ) : null}
            </Paper>

            {profile.ratingHistory.length > 0 ? (
              <Paper variant="outlined">
                <Typography variant="h4" sx={{ p: 2.5, pb: 1.5 }}>
                  {t('publicProfile.contestHistory')}
                </Typography>
                <Divider />
                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small" sx={{ minWidth: 520 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>{t('contest.title')}</TableCell>
                        <TableCell align="right">{t('contest.rank')}</TableCell>
                        <TableCell align="right">{t('publicProfile.ratingLabel')}</TableCell>
                        <TableCell align="right" sx={{ width: 88 }} />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {[...profile.ratingHistory].reverse().map((point) => (
                        <TableRow key={point.contestSlug}>
                          <TableCell>
                            <Box
                              component={Link}
                              to={contestPath(point.contestSlug)}
                              sx={{ color: 'inherit', textDecoration: 'none', '&:hover': { color: 'primary.main' } }}
                            >
                              {point.contestTitle}
                            </Box>
                          </TableCell>
                          <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                            {point.rank} / {point.participantCount}
                          </TableCell>
                          <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                            {formatRating(point.ratingAfter)}
                          </TableCell>
                          <TableCell align="right">
                            <Stack sx={{ alignItems: 'flex-end' }}>
                              <RatingDelta delta={point.delta} />
                            </Stack>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              </Paper>
            ) : null}

            <Paper variant="outlined" sx={{ p: 2.5 }}>
              <Typography variant="h4" sx={{ mb: 1.5 }}>
                {t('publicProfile.recentSolves')}
              </Typography>
              {profile.recentSolves.length === 0 ? (
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('publicProfile.recentEmpty')}
                </Typography>
              ) : (
                <Stack spacing={1}>
                  {profile.recentSolves.map((solve) => (
                    <Stack
                      key={solve.slug}
                      direction="row"
                      spacing={1.5}
                      sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                    >
                      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', minWidth: 0 }}>
                        <DifficultyChip difficulty={solve.difficulty} size="small" />
                        <Box
                          component={Link}
                          to={problemPath(solve.slug)}
                          sx={{ color: 'inherit', textDecoration: 'none', '&:hover': { color: 'primary.main' } }}
                        >
                          {solve.title}
                        </Box>
                      </Stack>
                      <Typography variant="body2" sx={{ color: 'text.disabled', whiteSpace: 'nowrap' }}>
                        {new Date(solve.solvedAt).toLocaleDateString(i18n.language, { dateStyle: 'medium' })}
                      </Typography>
                    </Stack>
                  ))}
                </Stack>
              )}
            </Paper>
          </Stack>
        </Stack>
      </Container>
    </Box>
  );
};
