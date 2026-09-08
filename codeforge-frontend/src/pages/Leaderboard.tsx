import {
  Alert,
  Box,
  CircularProgress,
  Container,
  Pagination,
  Paper,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  Tooltip,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { toApiError } from '../api/api-error';
import { profilesApi } from '../api/profiles-api';
import { type LeaderboardRow, type PageResponse } from '../api/types';
import { useAuth } from '../auth/use-auth';
import { formatRating } from '../components/contest/contest';
import { AppHeader } from '../components/layout/AppHeader';
import { UserAvatar } from '../components/user/UserAvatar';
import { isAvatarId } from '../components/user/avatars';
import { useMessages } from '../i18n/use-messages';
import { profilePath } from '../routes/paths';

const PAGE_SIZE = 25;

type Board = 'rating' | 'solved';

/**
 * The two global tables.
 *
 * <p>Two boards rather than one, because they answer different questions and
 * neither subsumes the other: a rating says how you do against other people
 * under a clock, and a solved count says how much of the catalogue you have
 * worked through. Plenty of people are near the top of one and nowhere on the
 * other, and flattening them into a single number would hide that.
 */
export const LeaderboardPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { user } = useAuth();

  const [board, setBoard] = useState<Board>('rating');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<LeaderboardRow> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const call =
      board === 'rating'
        ? profilesApi.ratingLeaderboard(page, PAGE_SIZE)
        : profilesApi.solvedLeaderboard(page, PAGE_SIZE);

    call
      .then((result) => {
        if (!cancelled) {
          setData(result);
          setError(null);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
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
  }, [board, page, message]);

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="md" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">{t('leaderboard.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('leaderboard.subtitle')}
            </Typography>
          </Box>

          <Tabs
            value={board}
            onChange={(_, value: Board) => {
              setBoard(value);
              // A rank on one board means nothing on the other, so the page
              // resets rather than landing somebody on page 7 of a shorter table.
              setPage(0);
            }}
          >
            <Tab value="rating" label={t('leaderboard.byRating')} />
            <Tab value="solved" label={t('leaderboard.bySolved')} />
          </Tabs>

          {error ? <Alert severity="error">{error}</Alert> : null}

          {loading ? (
            <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : data === null || data.totalElements === 0 ? (
            <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
              <Typography variant="h4">{t('leaderboard.empty')}</Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
                {t(board === 'rating' ? 'leaderboard.emptyRating' : 'leaderboard.emptySolved')}
              </Typography>
            </Paper>
          ) : (
            <>
              <Paper variant="outlined">
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ width: 64 }}>{t('leaderboard.rank')}</TableCell>
                      <TableCell>{t('leaderboard.user')}</TableCell>
                      {board === 'rating' ? (
                        <>
                          <TableCell align="right">{t('leaderboard.rating')}</TableCell>
                          <TableCell align="right">{t('leaderboard.contests')}</TableCell>
                        </>
                      ) : (
                        <>
                          <TableCell align="right">{t('leaderboard.solved')}</TableCell>
                          <TableCell align="right">
                            <Tooltip title={t('leaderboard.pointsHint')}>
                              <span>{t('leaderboard.points')}</span>
                            </Tooltip>
                          </TableCell>
                        </>
                      )}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {data.content.map((row) => {
                      const mine = row.userId === user?.id;

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
                                sx={{
                                  color: 'inherit',
                                  textDecoration: 'none',
                                  '&:hover': { color: 'primary.main' },
                                }}
                              >
                                {row.username}
                              </Box>
                            </Stack>
                          </TableCell>
                          {board === 'rating' ? (
                            <>
                              <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                                {formatRating(row.rating ?? 0)}
                              </TableCell>
                              <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                                {row.contestsAttended}
                              </TableCell>
                            </>
                          ) : (
                            <>
                              <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                                {row.solved}
                              </TableCell>
                              <TableCell align="right" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                                {row.points}
                              </TableCell>
                            </>
                          )}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </Paper>

              {data.totalPages > 1 ? (
                <Stack sx={{ alignItems: 'center' }}>
                  <Pagination
                    count={data.totalPages}
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
