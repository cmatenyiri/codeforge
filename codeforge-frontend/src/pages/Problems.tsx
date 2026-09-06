import { Alert, Box, Container, MenuItem, Pagination, Stack, TextField, Typography } from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toApiError } from '../api/api-error';
import { problemsApi } from '../api/problems-api';
import { usersApi } from '../api/users-api';
import { type PageResponse, type ProblemSort, type ProblemSummary, type Tag, type UserStats } from '../api/types';
import { AppHeader } from '../components/layout/AppHeader';
import { ProblemFilters } from '../components/problems/ProblemFilters';
import { ProblemProgress } from '../components/problems/ProblemProgress';
import { ProblemsTable } from '../components/problems/ProblemsTable';
import { PAGE_SIZES, useProblemQuery, type ProblemQueryState } from '../components/problems/problem-query';
import { useMessages } from '../i18n/use-messages';
import { problemPath } from '../routes/paths';

/**
 * The problem catalogue.
 *
 * <p>Built to stay readable at a hundred problems and beyond: every filter and
 * the sort order live in the URL, the list is paged with a size the reader
 * chooses, and the toolbar sticks under the header so narrowing a long list
 * never means scrolling back up to do it.
 */
export const ProblemsPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();
  const { query, update, reset, filtered } = useProblemQuery();

  const [result, setResult] = useState<PageResponse<ProblemSummary> | null>(null);
  const [tags, setTags] = useState<Tag[]>([]);
  const [stats, setStats] = useState<UserStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [picking, setPicking] = useState(false);
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
    usersApi
      .stats()
      .then(setStats)
      .catch(() => {
        // The progress strip is a flourish; its absence must not hide the list.
      });
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    problemsApi
      .list({
        search: query.search,
        difficulty: query.difficulty,
        tag: query.tag,
        status: query.status,
        sort: query.sort,
        order: query.order,
        page: query.page - 1,
        size: query.size,
      })
      .then((data) => {
        if (!cancelled) {
          setResult(data);
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
  }, [query.search, query.difficulty, query.tag, query.status, query.sort, query.order, query.page, query.size, message]);

  const changeFilters = useCallback(
    (patch: Partial<ProblemQueryState>) => {
      update(patch);
    },
    [update],
  );

  /** Clicking the active column flips it; a new column starts ascending. */
  const changeSort = (sort: ProblemSort) => {
    update(sort === query.sort ? { order: query.order === 'asc' ? 'desc' : 'asc' } : { sort, order: 'asc' });
  };

  const pickRandom = () => {
    setPicking(true);
    problemsApi
      .random({ search: query.search, difficulty: query.difficulty, tag: query.tag, status: query.status })
      .then((problem) => navigate(problemPath(problem.slug)))
      .catch((caught: unknown) => {
        const apiError = toApiError(caught);
        setError(apiError.status === 404 ? t('problems.emptyTitle') : message(apiError.code, apiError.message));
      })
      .finally(() => {
        setPicking(false);
      });
  };

  const firstOnPage = (query.page - 1) * query.size + 1;
  const lastOnPage = firstOnPage + (result?.content.length ?? 0) - 1;

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">{t('problems.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('problems.subtitle')}
            </Typography>
          </Box>

          <ProblemProgress stats={stats} />

          <Box
            sx={{
              // Under the sticky header rather than at the top of the viewport,
              // so the two do not overlap on a long list.
              position: 'sticky',
              top: 64,
              zIndex: 2,
              py: 1.5,
              backgroundColor: 'surface.canvas',
            }}
          >
            <ProblemFilters
              query={query}
              tags={tags}
              onChange={changeFilters}
              onClear={reset}
              onPickRandom={pickRandom}
              picking={picking}
              filtered={filtered}
            />
          </Box>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <ProblemsTable
            problems={result?.content ?? []}
            loading={loading}
            rows={Math.min(query.size, 10)}
            sort={query.sort}
            order={query.order}
            onSort={changeSort}
          />

          {result && !loading ? (
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              sx={{ alignItems: 'center', justifyContent: 'space-between' }}
            >
              <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                {result.totalElements === 0
                  ? t('problems.count', { count: 0 })
                  : t('problems.showing', {
                      from: firstOnPage,
                      to: lastOnPage,
                      total: result.totalElements,
                    })}
              </Typography>

              <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                <TextField
                  select
                  size="small"
                  label={t('problems.perPage')}
                  value={query.size}
                  onChange={(event) => {
                    update({ size: Number(event.target.value) });
                  }}
                  sx={{ width: 116 }}
                >
                  {PAGE_SIZES.map((size) => (
                    <MenuItem key={size} value={size}>
                      {size}
                    </MenuItem>
                  ))}
                </TextField>

                {result.totalPages > 1 ? (
                  <Pagination
                    count={result.totalPages}
                    page={Math.min(query.page, result.totalPages)}
                    shape="rounded"
                    siblingCount={1}
                    boundaryCount={1}
                    onChange={(_, value) => {
                      update({ page: value });
                      window.scrollTo({ top: 0, behavior: 'smooth' });
                    }}
                  />
                ) : null}
              </Stack>
            </Stack>
          ) : null}
        </Stack>
      </Container>
    </Box>
  );
};
