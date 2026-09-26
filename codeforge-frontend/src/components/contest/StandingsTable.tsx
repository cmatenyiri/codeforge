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
import { type TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type ContestProblemSummary, type ContestResult } from '../../api/types';
import { profilePath } from '../../routes/paths';
import { UserAvatar } from '../user/UserAvatar';
import { isAvatarId } from '../user/avatars';
import { RatingDelta } from './RatingDelta';
import { formatContestTime } from './contest';

/**
 * Minutes each wrong attempt on a solved problem adds to Time. The server's
 * rule; repeated here only so a cell can say what its own red count cost.
 */
const PENALTY_MINUTES = 5;

/**
 * One cell of the grid: whether they solved it, when, and at what cost.
 *
 * <p>The wrong-attempt count is drawn small and red under the time rather than
 * as a separate column, because it is a footnote on the solve — five minutes
 * each, already inside the total to the left.
 *
 * <p>Every state says what it means on hover. The grid is dense by design — a
 * time, a red count and a dash carry everything — and the explanation belongs
 * on the thing being read, not in a legend somewhere above it.
 */
const ProblemCell = ({ result }: { result?: ContestResult['problems'][number] }) => {
  const { t } = useTranslation();

  if (result === undefined || (!result.solved && result.wrongAttempts === 0)) {
    return (
      <Tooltip title={t('contest.cell.untouched')}>
        <Typography variant="body2" sx={{ color: 'text.disabled' }}>
          —
        </Typography>
      </Tooltip>
    );
  }

  if (!result.solved) {
    return (
      <Tooltip title={t('contest.cell.wrongUnsolved', { count: result.wrongAttempts })}>
        <Typography variant="body2" sx={{ color: 'verdict.wrongAnswer' }}>
          −{result.wrongAttempts}
        </Typography>
      </Tooltip>
    );
  }

  const time = formatContestTime(result.solvedAtSeconds ?? 0);

  return (
    <Tooltip
      title={
        result.wrongAttempts > 0
          ? t('contest.cell.solvedAfterWrong', {
              time,
              count: result.wrongAttempts,
              penalty: result.wrongAttempts * PENALTY_MINUTES,
            })
          : t('contest.cell.solved', { time })
      }
    >
      <Stack sx={{ alignItems: 'center' }}>
        <Typography variant="body2" sx={{ color: 'verdict.accepted', fontVariantNumeric: 'tabular-nums' }}>
          {time}
        </Typography>
        {result.wrongAttempts > 0 ? (
          <Typography variant="caption" sx={{ color: 'verdict.wrongAnswer' }}>
            −{result.wrongAttempts}
          </Typography>
        ) : null}
      </Stack>
    </Tooltip>
  );
};

/** What the score is made of — "Q1 (3) + Q3 (5)" — or null when nothing was solved. */
const scoreHint = (row: ContestResult, problems: ContestProblemSummary[]): string | null => {
  const parts = problems
    .filter((problem) => row.problems.some((entry) => entry.position === problem.position && entry.solved))
    .map((problem) => `${problem.label} (${problem.points})`);

  return parts.length === 0 ? null : parts.join(' + ');
};

/** How the time was reached: the last solve, plus whatever wrong attempts cost. */
const timeHint = (row: ContestResult, t: TFunction): string => {
  const solved = row.problems.filter((entry) => entry.solved);
  if (solved.length === 0) {
    return t('contest.cell.nothingSolved');
  }

  const finish = formatContestTime(row.finishSeconds);
  if (row.penaltySeconds <= 0) {
    return t('contest.cell.timeNoPenalty', { finish });
  }
  return t('contest.cell.time', {
    finish,
    penalty: Math.round(row.penaltySeconds / 60),
    count: solved.reduce((sum, entry) => sum + entry.wrongAttempts, 0),
  });
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
              <Tooltip title={t('contest.timeHint')}>
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
                  <Tooltip title={scoreHint(row, problems) ?? t('contest.cell.nothingSolved')}>
                    <span>{row.score}</span>
                  </Tooltip>
                </TableCell>
                <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                  <Tooltip title={timeHint(row, t)}>
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
