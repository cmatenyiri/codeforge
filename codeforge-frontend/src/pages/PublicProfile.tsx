import LocalFireDepartmentRounded from '@mui/icons-material/LocalFireDepartmentRounded';
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
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { profilesApi } from '../api/profiles-api';
import { type PublicProfile } from '../api/types';
import { RatingDelta } from '../components/contest/RatingDelta';
import { formatRating } from '../components/contest/contest';
import { AppHeader } from '../components/layout/AppHeader';
import { DifficultyChip } from '../components/problems/DifficultyChip';
import { DIFFICULTY_TOKEN } from '../components/problems/difficulty';
import { ActivityCalendar } from '../components/profile/ActivityCalendar';
import { RatingGraph } from '../components/profile/RatingGraph';
import { UserAvatar } from '../components/user/UserAvatar';
import { isAvatarId } from '../components/user/avatars';
import { useMessages } from '../i18n/use-messages';
import { contestPath, problemPath } from '../routes/paths';

const Metric = ({ label, value, hint }: { label: string; value: string; hint?: string }) => (
  <Box>
    <Typography variant="metric">{value}</Typography>
    <Typography variant="body2" sx={{ color: 'text.disabled' }}>
      {label}
    </Typography>
    {hint ? (
      <Typography variant="caption" sx={{ color: 'text.disabled' }}>
        {hint}
      </Typography>
    ) : null}
  </Box>
);

/**
 * Somebody's public profile.
 *
 * <p>Three things, in the order people look for them: what they have solved, how
 * they have competed, and whether they keep at it. The last is the calendar, and
 * it is the one no total on the page can answer — fifty solves in a weekend and
 * fifty over a year are the same number and completely different facts.
 */
export const PublicProfilePage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();
  const { username } = useParams<{ username: string }>();

  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [loading, setLoading] = useState(true);
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

  const totalActivity = profile.activity.reduce((sum, day) => sum + day.submissions, 0);

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack
              direction={{ xs: 'column', md: 'row' }}
              spacing={3}
              sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
            >
              <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                {isAvatarId(profile.avatar) ? <UserAvatar avatar={profile.avatar} size={64} /> : null}
                <Box>
                  <Typography variant="h1">{profile.username}</Typography>
                  <Typography variant="body2" sx={{ color: 'text.disabled', mt: 0.5 }}>
                    {t('publicProfile.joined', {
                      date: new Date(profile.joinedAt).toLocaleDateString(i18n.language, { dateStyle: 'medium' }),
                    })}
                  </Typography>
                  {profile.globalRank === undefined ? null : (
                    <Chip
                      size="small"
                      sx={{ mt: 1 }}
                      label={t('publicProfile.globalRank', {
                        rank: profile.globalRank,
                        total: profile.globalRankTotal,
                      })}
                    />
                  )}
                </Box>
              </Stack>

              <Stack direction="row" spacing={4} useFlexGap sx={{ flexWrap: 'wrap' }}>
                <Metric label={t('publicProfile.solved')} value={`${profile.solved}`} />
                <Metric label={t('publicProfile.submissions')} value={`${profile.submissions}`} />
                <Metric
                  label={t('publicProfile.acceptance')}
                  value={
                    profile.acceptanceRate === undefined ? '—' : `${(profile.acceptanceRate * 100).toFixed(1)}%`
                  }
                />
                <Stack direction="row" spacing={0.5} sx={{ alignItems: 'flex-start' }}>
                  <LocalFireDepartmentRounded
                    sx={{ fontSize: 20, mt: 0.5, color: profile.streak > 0 ? 'brand.ember' : 'text.disabled' }}
                  />
                  <Metric
                    label={t('publicProfile.streak')}
                    value={`${profile.streak}`}
                    hint={t('publicProfile.maxStreak') + ` ${profile.maxStreak}`}
                  />
                </Stack>
              </Stack>
            </Stack>
          </Paper>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
            <Paper variant="outlined" sx={{ p: 2.5, flex: 1 }}>
              <Typography variant="h4" sx={{ mb: 1.5 }}>
                {t('publicProfile.solved')}
              </Typography>
              <Stack spacing={1.5}>
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
            </Paper>

            <Paper variant="outlined" sx={{ p: 2.5, flex: 1 }}>
              <Typography variant="h4" sx={{ mb: 1.5 }}>
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
                <Stack direction="row" spacing={4} useFlexGap sx={{ flexWrap: 'wrap' }}>
                  <Metric
                    label={t('publicProfile.rating')}
                    value={formatRating(profile.rating)}
                    hint={t('publicProfile.maxRating', { rating: formatRating(profile.maxRating ?? 0) })}
                  />
                  <Metric label={t('publicProfile.contests')} value={`${profile.contestsAttended ?? 0}`} />
                  {profile.ratingRank === undefined ? null : (
                    <Metric
                      label={t('contest.rank')}
                      value={`#${profile.ratingRank}`}
                      hint={`/ ${profile.ratingRankTotal}`}
                    />
                  )}
                </Stack>
              )}
            </Paper>
          </Stack>

          <ActivityCalendar days={profile.activity} total={totalActivity} />

          {profile.ratingHistory.length > 0 ? <RatingGraph history={profile.ratingHistory} /> : null}

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
                      <TableCell align="right">{t('leaderboard.rating')}</TableCell>
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
      </Container>
    </Box>
  );
};
