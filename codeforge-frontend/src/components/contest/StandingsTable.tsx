import {
  Box,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type ContestProblemSummary, type ContestResult } from '../../api/types';
import { profilePath } from '../../routes/paths';
import { UserAvatar } from '../user/UserAvatar';
import { isAvatarId } from '../user/avatars';
import { RatingDelta } from './RatingDelta';
import { formatContestTime } from './contest';

/**
 * One cell of the grid: whether they solved it, when, and at what cost.
 *
 * <p>The wrong-attempt count is drawn small and red under the time rather than
 * as a separate column, because it is a footnote on the solve — five minutes
 * each, already inside the total to the left.
 */
const ProblemCell = ({ result }: { result?: ContestResult['problems'][number] }) => {
  const { t } = useTranslation();

  if (result === undefined || (!result.solved && result.wrongAttempts === 0)) {
    return (
      <Typography variant="body2" sx={{ color: 'text.disabled' }}>
        —
      </Typography>
    );
  }

  if (!result.solved) {
    return (
      <Tooltip title={t('contest.wrongAttempts', { count: result.wrongAttempts })}>
        <Typography variant="body2" sx={{ color: 'verdict.wrongAnswer' }}>
          −{result.wrongAttempts}
        </Typography>
      </Tooltip>
    );
  }

  return (
    <Stack sx={{ alignItems: 'center' }}>
      <Typography variant="body2" sx={{ color: 'verdict.accepted', fontVariantNumeric: 'tabular-nums' }}>
        {formatContestTime(result.solvedAtSeconds ?? 0)}
      </Typography>
      {result.wrongAttempts > 0 ? (
        <Typography variant="caption" sx={{ color: 'verdict.wrongAnswer' }}>
          −{result.wrongAttempts}
        </Typography>
      ) : null}
    </Stack>
  );
};

/**
 * The scoreboard.
 *
 * <p>Read far more often than anything else in a contest, and mostly for one
 * thing: finding yourself. So the caller's own row is highlighted wherever it
 * appears, and the page that draws this also pins it above the table when it is
 * on a different page entirely.
 */
export const StandingsTable = ({
  rows,
  problems,
  currentUserId,
  showRatingDelta,
}: {
  rows: ContestResult[];
  problems: ContestProblemSummary[];
  currentUserId?: number;
  /** Only once the ratings have landed; before that the column would be all dashes. */
  showRatingDelta: boolean;
}) => {
  const { t } = useTranslation();

  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ minWidth: 720 }}>
        <TableHead>
          <TableRow>
            <TableCell sx={{ width: 64 }}>{t('contest.rank')}</TableCell>
            <TableCell>{t('contest.user')}</TableCell>
            <TableCell align="right" sx={{ width: 72 }}>
              <Tooltip title={t('contest.scoreHint')}>
                <span>{t('contest.score')}</span>
              </Tooltip>
            </TableCell>
            <TableCell align="right" sx={{ width: 96 }}>
              <Tooltip title={t('contest.penaltyHint')}>
                <span>{t('contest.totalTime')}</span>
              </Tooltip>
            </TableCell>
            {problems.map((problem) => (
              <TableCell key={problem.position} align="center" sx={{ width: 88 }}>
                <Stack sx={{ alignItems: 'center' }}>
                  <Box component="span">{problem.label}</Box>
                  <Typography variant="caption" sx={{ color: 'text.disabled' }}>
                    {problem.points}
                  </Typography>
                </Stack>
              </TableCell>
            ))}
            {showRatingDelta ? (
              <TableCell align="right" sx={{ width: 88 }}>
                {t('leaderboard.rating')}
              </TableCell>
            ) : null}
          </TableRow>
        </TableHead>

        <TableBody>
          {rows.map((row) => {
            const mine = row.userId === currentUserId;

            return (
              <TableRow
                key={row.userId}
                sx={{
                  backgroundColor: mine ? 'surface.selected' : undefined,
                  '& td': mine ? { fontWeight: 600 } : undefined,
                }}
              >
                <TableCell sx={{ fontVariantNumeric: 'tabular-nums' }}>{row.rank}</TableCell>
                <TableCell>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    {isAvatarId(row.avatar) ? <UserAvatar avatar={row.avatar} size={22} /> : null}
                    <Box
                      component={Link}
                      to={profilePath(row.username)}
                      sx={{ color: 'inherit', textDecoration: 'none', '&:hover': { color: 'primary.main' } }}
                    >
                      {row.username}
                    </Box>
                  </Stack>
                </TableCell>
                <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                  {row.score}
                </TableCell>
                <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                  <Tooltip
                    title={
                      row.penaltySeconds > 0
                        ? `${formatContestTime(row.finishSeconds)} + ${Math.round(row.penaltySeconds / 60)}m`
                        : ''
                    }
                  >
                    <span>{formatContestTime(row.totalTimeSeconds)}</span>
                  </Tooltip>
                </TableCell>
                {problems.map((problem) => (
                  <TableCell key={problem.position} align="center">
                    <ProblemCell
                      result={row.problems.find((entry) => entry.position === problem.position)}
                    />
                  </TableCell>
                ))}
                {showRatingDelta ? (
                  <TableCell align="right">
                    {row.ratingDelta === undefined ? (
                      <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                        —
                      </Typography>
                    ) : (
                      <Stack sx={{ alignItems: 'flex-end' }}>
                        <RatingDelta delta={row.ratingDelta} />
                      </Stack>
                    )}
                  </TableCell>
                ) : null}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </Box>
  );
};
