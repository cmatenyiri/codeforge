import GroupsRounded from '@mui/icons-material/GroupsRounded';
import { Box, Button, Chip, Paper, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type ContestSummary } from '../../api/types';
import { contestPath } from '../../routes/paths';
import { ContestCountdown } from './ContestCountdown';
import { ContestStatusChip } from './ContestStatusChip';
import { RatingDelta } from './RatingDelta';
import { CONTEST_TYPE_LABEL_KEY } from './contest';
import { useContestClock } from './use-contest-clock';

/**
 * One contest, as a card in the lobby.
 *
 * <p>The primary action changes with the clock, because there is only ever one
 * sensible thing to do with a contest: register for one that has not started,
 * walk into one that is running, and read the standings of one that is over.
 */
export const ContestCard = ({
  contest,
  onRegister,
  registering,
}: {
  contest: ContestSummary;
  onRegister?: (contest: ContestSummary) => void;
  registering?: boolean;
}) => {
  const { t, i18n } = useTranslation();

  // Anchored to the server's number and re-anchored on every reload; this only
  // makes the digits move in between.
  const untilStart = useContestClock(contest.secondsUntilStart, contest.status === 'SCHEDULED');
  const remaining = useContestClock(contest.remainingSeconds, contest.status === 'RUNNING');

  const scheduled = contest.status === 'SCHEDULED';
  const running = contest.status === 'RUNNING';
  const over = contest.status === 'ENDED' || contest.status === 'FINALIZED';

  return (
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
      >
        <Stack spacing={1} sx={{ minWidth: 0, flex: 1 }}>
          <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
            <Typography variant="h4" sx={{ minWidth: 0 }}>
              <Box
                component={Link}
                to={contestPath(contest.slug)}
                sx={{ color: 'inherit', textDecoration: 'none', '&:hover': { color: 'primary.main' } }}
              >
                {contest.title}
              </Box>
            </Typography>
            <ContestStatusChip status={contest.status} size="small" />
            <Chip label={t(CONTEST_TYPE_LABEL_KEY[contest.type])} variant="outlined" size="small" />
            {contest.rated ? null : <Chip label={t('contest.unrated')} size="small" />}
          </Stack>

          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap', color: 'text.disabled' }}>
            <Typography variant="body2">
              {new Date(contest.startsAt).toLocaleString(i18n.language, {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </Typography>
            <Typography variant="body2">{t('contest.duration', { count: contest.durationMinutes })}</Typography>
            <Typography variant="body2">
              {t('contest.problemCount', { count: contest.problemCount })}
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

          {/* Somebody's own result is the reason they open a past contest, so it
              sits on the card rather than one click further in. */}
          {over && contest.myRank !== undefined ? (
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {t('contest.rank')} <strong>#{contest.myRank}</strong> · {contest.myScore} {t('contest.score')}
              </Typography>
              {contest.myRatingDelta === undefined ? null : <RatingDelta delta={contest.myRatingDelta} />}
            </Stack>
          ) : null}
        </Stack>

        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
          {scheduled ? <ContestCountdown seconds={untilStart} mode="until" durationMinutes={contest.durationMinutes} /> : null}
          {running ? <ContestCountdown seconds={remaining} mode="remaining" durationMinutes={contest.durationMinutes} /> : null}

          {running ? (
            <Button component={Link} to={contestPath(contest.slug)}>
              {t(contest.myRank === undefined ? 'contest.enter' : 'contest.resume')}
            </Button>
          ) : over ? (
            <Button component={Link} to={contestPath(contest.slug)} variant="outlined">
              {t('contest.standings')}
            </Button>
          ) : contest.registered ? (
            <Button variant="outlined" disabled>
              {t('contest.registered')}
            </Button>
          ) : (
            <Button onClick={() => onRegister?.(contest)} disabled={registering}>
              {t(registering ? 'contest.registering' : 'contest.register')}
            </Button>
          )}
        </Stack>
      </Stack>
    </Paper>
  );
};
