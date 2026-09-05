import { Alert, Box, Container, Pagination, Stack, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { problemsApi } from '../api/problems-api';
import { type PageResponse, type ProblemSummary, type Tag } from '../api/types';
import { AppHeader } from '../components/layout/AppHeader';
import { ProblemFilters, type Filters } from '../components/problems/ProblemFilters';
import { ProblemsTable } from '../components/problems/ProblemsTable';
import { useMessages } from '../i18n/use-messages';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

/**
 * The problem catalogue.
 *
 * <p>Filters live in the URL rather than component state, so a filtered view is
 * shareable, survives a refresh, and works with the browser's back button.
 */
export const ProblemsPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const [searchParams, setSearchParams] = useSearchParams();

  const filters = useMemo<Filters>(
    () => ({
      search: searchParams.get('search') ?? '',
      difficulty: (searchParams.get('difficulty') ?? '') as Filters['difficulty'],
      tag: searchParams.get('tag') ?? '',
    }),
    [searchParams],
  );
  const page = Number(searchParams.get('page') ?? '1');

  const [result, setResult] = useState<PageResponse<ProblemSummary> | null>(null);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    problemsApi
      .tags()
      .then(setTags)
      .catch(() => {
        // A missing tag list only costs the topic filter; the list still works.
      });
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    // Debounced so typing in the search box does not fire a request per keystroke.
    const timer = setTimeout(() => {
      problemsApi
        .list({
          search: filters.search,
          difficulty: filters.difficulty,
          tag: filters.tag,
          page: Math.max(page - 1, 0),
          size: PAGE_SIZE,
        })
        .then((data) => {
          if (!cancelled) {
            setResult(data);
            setError(null);
          }
        })
        .catch((error_: unknown) => {
          if (!cancelled) {
            const apiError = toApiError(error_);
            setError(message(apiError.code, apiError.message));
          }
        })
        .finally(() => {
          if (!cancelled) {
            setLoading(false);
          }
        });
    }, SEARCH_DEBOUNCE_MS);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [filters.search, filters.difficulty, filters.tag, page, message]);

  const updateFilters = (next: Filters) => {
    const params = new URLSearchParams();
    if (next.search) params.set('search', next.search);
    if (next.difficulty) params.set('difficulty', next.difficulty);
    if (next.tag) params.set('tag', next.tag);
    // Any filter change invalidates the current page number.
    setSearchParams(params, { replace: true });
  };

  const goToPage = (nextPage: number) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(nextPage));
    setSearchParams(params);
  };

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />
      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">{t('problems.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('problems.subtitle')}
            </Typography>
          </Box>

          <ProblemFilters filters={filters} tags={tags} onChange={updateFilters} />

          {error ? <Alert severity="error">{error}</Alert> : null}

          {result && !loading ? (
            <Typography variant="overline" sx={{ color: 'text.disabled' }}>
              {t('problems.count', { count: result.totalElements })}
            </Typography>
          ) : null}

          <ProblemsTable problems={result?.content ?? []} loading={loading} />

          {result && result.totalPages > 1 ? (
            <Stack sx={{ alignItems: 'center' }}>
              <Pagination
                count={result.totalPages}
                page={page}
                shape="rounded"
                onChange={(_, value) => {
                  goToPage(value);
                }}
              />
            </Stack>
          ) : null}
        </Stack>
      </Container>
    </Box>
  );
};
