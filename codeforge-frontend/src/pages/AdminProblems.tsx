import {
  Alert,
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  MenuItem,
  Pagination,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { adminApi, type AdminProblemSort } from '../api/admin-api';
import { toApiError } from '../api/api-error';
import { problemsApi } from '../api/problems-api';
import { type AdminProblemSummary, type PageResponse, type Tag } from '../api/types';
import { AdminProblemFilters } from '../components/admin/AdminProblemFilters';
import { AdminProblemsTable, type AdminProblemAction } from '../components/admin/AdminProblemsTable';
import { PAGE_SIZES, useAdminProblemQuery } from '../components/admin/admin-problem-query';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { adminProblemEditPath } from '../routes/paths';

/**
 * The authoring catalogue.
 *
 * <p>Every destructive or publishing action lives in a row menu rather than as a
 * column of buttons: they are rare, they are not symmetric — a published problem
 * offers "unpublish", an archived one offers "restore" — and the row itself is
 * for reading.
 *
 * <p>Deleting is the one action that asks first, and the one that can be refused:
 * a problem people have solved keeps its submissions, so the server says no and
 * this page explains that archiving is what was wanted.
 */
export const AdminProblemsPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();
  const { query, update, reset, filtered } = useAdminProblemQuery();

  const [result, setResult] = useState<PageResponse<AdminProblemSummary> | null>(null);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [pendingDelete, setPendingDelete] = useState<AdminProblemSummary | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => {
    setRefreshKey((key) => key + 1);
  }, []);

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

    adminApi
      .list({
        search: query.search,
        difficulty: query.difficulty,
        tag: query.tag,
        state: query.state,
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
  }, [
    query.search,
    query.difficulty,
    query.tag,
    query.state,
    query.sort,
    query.order,
    query.page,
    query.size,
    refreshKey,
    message,
  ]);

  const changeSort = (sort: AdminProblemSort) => {
    update(sort === query.sort ? { order: query.order === 'asc' ? 'desc' : 'asc' } : { sort, order: 'asc' });
  };

  /**
   * Runs one row action.
   *
   * <p>Failures are shown as an alert rather than swallowed: publishing is the
   * action most likely to be refused, and the reasons — no signature, no sample
   * case — are exactly what the author needs to read.
   */
  const runAction = async (action: AdminProblemAction, problem: AdminProblemSummary) => {
    setBusyId(problem.id);
    setError(null);
    try {
      switch (action) {
        case 'edit': {
          await navigate(adminProblemEditPath(problem.id));
          return;
        }
        case 'duplicate': {
          const copy = await adminApi.duplicate(problem.id);
          await navigate(adminProblemEditPath(copy.id));
          return;
        }
        case 'publish': {
          await adminApi.setPublished(problem.id, true);
          setNotice(t('admin.list.publishedNotice', { title: problem.title }));
          break;
        }
        case 'unpublish': {
          await adminApi.setPublished(problem.id, false);
          setNotice(t('admin.list.unpublishedNotice', { title: problem.title }));
          break;
        }
        case 'archive': {
          await adminApi.setArchived(problem.id, true);
          setNotice(t('admin.list.archivedNotice', { title: problem.title }));
          break;
        }
        case 'restore': {
          await adminApi.setArchived(problem.id, false);
          setNotice(t('admin.list.restoredNotice', { title: problem.title }));
          break;
        }
        case 'delete': {
          setPendingDelete(problem);
          return;
        }
      }
      refresh();
    } catch (caught) {
      const apiError = toApiError(caught);
      setError(
        apiError.hasFieldErrors
          ? `${t('admin.list.publishRefused', { title: problem.title })} ${apiError.fieldErrors
              .map((fieldError) => message(fieldError.code, fieldError.message))
              .join(' ')}`
          : message(apiError.code, apiError.message),
      );
    } finally {
      setBusyId(null);
    }
  };

  const confirmDelete = async () => {
    if (!pendingDelete) {
      return;
    }

    setBusyId(pendingDelete.id);
    try {
      await adminApi.remove(pendingDelete.id);
      setNotice(t('admin.list.deletedNotice', { title: pendingDelete.title }));
      setPendingDelete(null);
      refresh();
    } catch (caught) {
      const apiError = toApiError(caught);
      setError(message(apiError.code, apiError.message));
      setPendingDelete(null);
    } finally {
      setBusyId(null);
    }
  };

  const firstOnPage = (query.page - 1) * query.size + 1;
  const lastOnPage = firstOnPage + (result?.content.length ?? 0) - 1;

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="xl" sx={{ py: 4 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">{t('admin.list.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('admin.list.subtitle')}
            </Typography>
          </Box>

          <Box sx={{ position: 'sticky', top: 64, zIndex: 2, py: 1.5, backgroundColor: 'surface.canvas' }}>
            <AdminProblemFilters
              query={query}
              tags={tags}
              onChange={update}
              onClear={reset}
              filtered={filtered}
            />
          </Box>

          {error ? (
            <Alert severity="error" onClose={() => { setError(null); }}>
              {error}
            </Alert>
          ) : null}

          <AdminProblemsTable
            problems={result?.content ?? []}
            loading={loading}
            rows={Math.min(query.size, 10)}
            sort={query.sort}
            order={query.order}
            onSort={changeSort}
            busyId={busyId}
            onAction={(action, problem) => {
              void runAction(action, problem);
            }}
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
                  : t('problems.showing', { from: firstOnPage, to: lastOnPage, total: result.totalElements })}
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

      <Dialog
        open={pendingDelete !== null}
        onClose={() => {
          setPendingDelete(null);
        }}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>{t('admin.list.deleteTitle')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('admin.list.deleteBody', { title: pendingDelete?.title ?? '' })}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button
            variant="text"
            onClick={() => {
              setPendingDelete(null);
            }}
          >
            {t('admin.form.cancel')}
          </Button>
          <Button
            color="error"
            onClick={() => {
              void confirmDelete();
            }}
          >
            {t('admin.list.delete')}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={notice !== null}
        autoHideDuration={4000}
        onClose={() => {
          setNotice(null);
        }}
        message={notice}
      />
    </Box>
  );
};
