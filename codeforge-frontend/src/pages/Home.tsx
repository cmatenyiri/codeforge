import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import {
  Box,
  Button,
  Container,
  Link as MuiLink,
  Paper,
  Skeleton,
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
import { Link } from 'react-router';
import { submissionsApi } from '../api/submissions-api';
import { type SubmissionSummary, type UserStats } from '../api/types';
import { usersApi } from '../api/users-api';
import { useAuth } from '../auth/use-auth';
import { AppHeader } from '../components/layout/AppHeader';
import { DifficultyChip } from '../components/problems/DifficultyChip';
import { ProblemProgress } from '../components/problems/ProblemProgress';
import { VerdictChip } from '../components/solve/VerdictChip';
import { LANGUAGE_LABEL } from '../components/solve/verdict';
import { paths, problemPath } from '../routes/paths';

/** How many recent attempts the dashboard shows before deferring to the full history. */
const RECENT_COUNT = 8;

const StatTile = ({ label, value, loading }: { label: string; value: string; loading: boolean }) => (
  <Paper variant="outlined" sx={{ p: 2, flex: '1 1 160px' }}>
    <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
      {label}
    </Typography>
    {loading ? <Skeleton width={70} height={34} /> : <Typography variant="metric">{value}</Typography>}
  </Paper>
);

/**
 * The signed-in landing page.
 *
 * <p>Answers the two questions somebody opening the app actually has — how far
 * along am I, and what was I last working on — and then gets out of the way.
 */
export const HomePage = () => {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();

  const [stats, setStats] = useState<UserStats | null>(null);
  const [recent, setRecent] = useState<SubmissionSummary[] | null>(null);

  useEffect(() => {
    // Neither panel is worth an error state: a dashboard that cannot load its
    // numbers should still let somebody reach the problems.
    usersApi.stats().then(setStats).catch(() => undefined);
    submissionsApi
      .listMine(0, RECENT_COUNT)
      .then((page) => {
        setRecent(page.content);
      })
      .catch(() => {
        setRecent([]);
      });
  }, []);

  if (!user) {
    return null;
  }

  const timestamp = (iso: string) =>
    new Intl.DateTimeFormat(i18n.resolvedLanguage, { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(iso),
    );

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
          >
            <Box>
              <Typography variant="h1">{t('home.welcome', { username: user.username })}</Typography>
              <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
                {t('home.subtitle')}
              </Typography>
            </Box>

            <Button
              component={Link}
              to={paths.problems}
              variant="contained"
              endIcon={<ArrowForwardRounded />}
              sx={{ alignSelf: { xs: 'flex-start', sm: 'center' }, flexShrink: 0 }}
            >
              {t('home.browseProblems')}
            </Button>
          </Stack>

          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
            <StatTile
              label={t('home.solved')}
              value={stats ? `${stats.solved}` : ''}
              loading={stats === null}
            />
            <StatTile
              label={t('home.submissions')}
              value={stats ? `${stats.submissions}` : ''}
              loading={stats === null}
            />
            <StatTile
              label={t('home.acceptanceRate')}
              value={
                stats?.acceptanceRate === undefined ? '—' : `${(stats.acceptanceRate * 100).toFixed(0)}%`
              }
              loading={stats === null}
            />
          </Stack>

          <ProblemProgress stats={stats} />

          <Paper variant="outlined">
            <Stack
              direction="row"
              sx={{ px: 2, py: 1.5, alignItems: 'center', justifyContent: 'space-between' }}
            >
              <Typography variant="subtitle1">{t('home.recentSubmissions')}</Typography>
            </Stack>

            {recent === null ? (
              <Box sx={{ p: 2 }}>
                <Skeleton height={28} />
                <Skeleton height={28} />
                <Skeleton height={28} />
              </Box>
            ) : recent.length === 0 ? (
              <Stack spacing={0.5} sx={{ alignItems: 'center', py: 6, px: 2 }}>
                <Typography variant="subtitle1">{t('home.noSubmissions')}</Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('home.noSubmissionsBody')}
                </Typography>
              </Stack>
            ) : (
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>{t('problems.problem')}</TableCell>
                    <TableCell width={120}>{t('problems.difficulty')}</TableCell>
                    <TableCell width={160}>{t('solve.status')}</TableCell>
                    <TableCell width={120}>{t('solve.programmingLanguage')}</TableCell>
                    <TableCell align="right">{t('solve.submittedAt')}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recent.map((submission) => (
                    <TableRow key={submission.id} hover>
                      <TableCell>
                        <MuiLink
                          component={Link}
                          to={problemPath(submission.problemSlug)}
                          underline="hover"
                          sx={{ typography: 'body2', fontWeight: 550, color: 'text.primary' }}
                        >
                          {submission.problemTitle}
                        </MuiLink>
                      </TableCell>
                      <TableCell>
                        <DifficultyChip difficulty={submission.difficulty} />
                      </TableCell>
                      <TableCell>
                        <VerdictChip status={submission.status} size="small" />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{LANGUAGE_LABEL[submission.language]}</Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                          {timestamp(submission.createdAt)}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Paper>
        </Stack>
      </Container>
    </Box>
  );
};
