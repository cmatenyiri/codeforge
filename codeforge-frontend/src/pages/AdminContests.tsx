import AddRounded from '@mui/icons-material/AddRounded';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Pagination,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { adminContestsApi } from '../api/admin-contests-api';
import { toApiError } from '../api/api-error';
import { type AdminContestSummary, type PageResponse } from '../api/types';
import { ContestStatusChip } from '../components/contest/ContestStatusChip';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { adminContestEditPath, paths } from '../routes/paths';

const PAGE_SIZE = 20;

/** The authoring catalogue: every contest, drafts included. */
export const AdminContestsPage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<AdminContestSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    // Debounced so typing a title does not fire a query per keystroke.
    const timer = setTimeout(() => {
      adminContestsApi
        .list(search, page, PAGE_SIZE)
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
    }, 250);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [search, page, message]);

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
            <Box>
              <Typography variant="h1">{t('admin.contest.title')}</Typography>
              <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
                {t('admin.contest.subtitle')}
              </Typography>
            </Box>
            <Button component={Link} to={paths.adminContestNew} startIcon={<AddRounded />}>
              {t('admin.contest.new')}
            </Button>
          </Stack>

          <TextField
            placeholder={t('admin.contest.searchPlaceholder')}
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            sx={{ maxWidth: 360 }}
          />

          {error ? <Alert severity="error">{error}</Alert> : null}

          {loading ? (
            <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : data === null || data.totalElements === 0 ? (
            <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
              <Typography variant="h4">{t('admin.contest.empty')}</Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
                {t('admin.contest.emptyBody')}
              </Typography>
            </Paper>
          ) : (
            <>
              <Paper variant="outlined">
                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small" sx={{ minWidth: 860 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>{t('admin.contest.name')}</TableCell>
                        <TableCell>{t('admin.contest.startsAt')}</TableCell>
                        <TableCell>{t('admin.contest.state')}</TableCell>
                        <TableCell align="right">{t('admin.contest.problems')}</TableCell>
                        <TableCell align="right">{t('admin.contest.people')}</TableCell>
                        <TableCell>{t('admin.contest.rejudge')}</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {data.content.map((contest) => (
                        <TableRow key={contest.id} hover>
                          <TableCell>
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                              <Box
                                component={Link}
                                to={adminContestEditPath(contest.id)}
                                sx={{
                                  color: 'inherit',
                                  textDecoration: 'none',
                                  fontWeight: 600,
                                  '&:hover': { color: 'primary.main' },
                                }}
                              >
                                {contest.title}
                              </Box>
                              {contest.rated ? null : <Chip label={t('contest.unrated')} size="small" />}
                              {contest.sealed ? (
                                <Chip label={t('admin.contest.sealed')} size="small" variant="outlined" />
                              ) : null}
                            </Stack>
                          </TableCell>
                          <TableCell sx={{ whiteSpace: 'nowrap' }}>
                            {new Date(contest.startsAt).toLocaleString(i18n.language, {
                              dateStyle: 'short',
                              timeStyle: 'short',
                            })}
                          </TableCell>
                          <TableCell>
                            <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                              <ContestStatusChip status={contest.status} size="small" />
                              {contest.status === 'ENDED' && contest.rated ? (
                                <Chip label={t('admin.contest.ratingsPending')} size="small" variant="outlined" />
                              ) : null}
                            </Stack>
                          </TableCell>
                          <TableCell align="right">{contest.problemCount}</TableCell>
                          <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                            {contest.participantCount} / {contest.registrationCount}
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                              {contest.rejudgeState === 'NONE' ? '—' : contest.rejudgeState}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
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
