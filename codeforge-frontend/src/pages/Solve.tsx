import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import { Alert, Box, CircularProgress, Divider, Link as MuiLink, Stack } from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { problemsApi } from '../api/problems-api';
import { type ProblemDetail } from '../api/types';
import { AppHeader } from '../components/layout/AppHeader';
import { EditorPanel } from '../components/solve/EditorPanel';
import { ProblemPanel } from '../components/solve/ProblemPanel';
import { useMessages } from '../i18n/use-messages';
import { paths } from '../routes/paths';

/**
 * The solving workspace: problem on the left, editor on the right.
 *
 * <p>The editor half is self-contained: it owns the language choice, the draft
 * and the run cycle, so this page only fetches the problem and lays the two
 * halves out.
 */
export const SolvePage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const { slug } = useParams<{ slug: string }>();

  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Bumped by every submission. It reloads the submissions tab, and — via the
  // effect below — the problem itself, so an acceptance updates the solved
  // badge and the acceptance rate without a manual refresh.
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => {
    setRefreshKey((key) => key + 1);
  }, []);

  useEffect(() => {
    if (!slug) {
      return;
    }

    let cancelled = false;
    // Only the first load blanks the page; a refresh after a submission keeps
    // the editor and the verdict on screen while it happens.
    setLoading((current) => current || refreshKey === 0);

    problemsApi
      .getBySlug(slug)
      .then((data) => {
        if (!cancelled) {
          setProblem(data);
          setError(null);
        }
      })
      .catch((error_: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(error_);
          setError(apiError.status === 404 ? t('solve.notFound') : message(apiError.code, apiError.message));
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
  }, [slug, refreshKey, message, t]);

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Box sx={{ px: 2, pt: 1.5 }}>
        <MuiLink
          component={Link}
          to={paths.problems}
          sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2' }}
        >
          <ArrowBackRounded sx={{ fontSize: 16 }} />
          {t('solve.backToProblems')}
        </MuiLink>
      </Box>

      {loading ? (
        <Box sx={{ flex: 1, display: 'grid', placeItems: 'center' }}>
          <CircularProgress />
        </Box>
      ) : error ? (
        <Box sx={{ p: 3 }}>
          <Alert severity="error">{error}</Alert>
        </Box>
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
            <ProblemPanel problem={problem} refreshKey={refreshKey} />
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
            <EditorPanel problem={problem} onSubmitted={refresh} />
          </Box>
        </Stack>
      ) : null}
    </Box>
  );
};
