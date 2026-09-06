import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import OpenInNewRounded from '@mui/icons-material/OpenInNewRounded';
import SaveRounded from '@mui/icons-material/SaveRounded';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Link as MuiLink,
  Paper,
  Snackbar,
  Stack,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { adminApi } from '../api/admin-api';
import { toApiError } from '../api/api-error';
import { problemsApi } from '../api/problems-api';
import { useApiErrors } from '../api/use-api-errors';
import { type AdminProblemDetail, type Tag } from '../api/types';
import { ProblemBasicsSection } from '../components/admin/ProblemBasicsSection';
import { ProblemEditorialSection } from '../components/admin/ProblemEditorialSection';
import { ProblemExamplesSection } from '../components/admin/ProblemExamplesSection';
import { ProblemHintsSection } from '../components/admin/ProblemHintsSection';
import { ProblemSignatureSection } from '../components/admin/ProblemSignatureSection';
import { ProblemStateChip } from '../components/admin/ProblemStateChip';
import { ProblemStatementSection } from '../components/admin/ProblemStatementSection';
import { ProblemTestCasesSection } from '../components/admin/ProblemTestCasesSection';
import {
  emptyForm,
  formFromDetail,
  formToPayload,
  type ProblemFormState,
} from '../components/admin/problem-form';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { paths, problemPath } from '../routes/paths';

/** What the dirty check compares. `slugLocked` is a UI preference, not part of the document. */
const fingerprint = (form: ProblemFormState): string => JSON.stringify(formToPayload(form));

/**
 * Writing one problem.
 *
 * <p>A single long form rather than a wizard: the sections are all one save, and
 * an author fixing a typo in a hint should not have to walk through five steps to
 * reach it. Creating and editing are the same screen because they send the same
 * document — the only difference is whether there is an id to send it to.
 *
 * <p>The form is the source of truth for what will be stored, and the server's
 * answer is the source of truth for what is: every save replaces the form with
 * the saved state, so rows that were just created adopt their ids instead of
 * being inserted a second time on the next save.
 */
export const AdminProblemEditorPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const problemId = id === undefined ? null : Number(id);

  const [form, setForm] = useState<ProblemFormState>(emptyForm);
  const [saved, setSaved] = useState<AdminProblemDetail | null>(null);
  const [savedPrint, setSavedPrint] = useState(() => fingerprint(emptyForm()));
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(problemId !== null);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [leaving, setLeaving] = useState(false);

  const { fieldErrors, generalError, capture, reset, clearMatching, errorsFor } = useApiErrors();

  const dirty = fingerprint(form) !== savedPrint;

  const adopt = useCallback((detail: AdminProblemDetail) => {
    const next = formFromDetail(detail);
    setSaved(detail);
    setForm(next);
    setSavedPrint(fingerprint(next));
  }, []);

  useEffect(() => {
    problemsApi
      .tags()
      .then(setTags)
      .catch(() => {
        // The topic picker degrades to what is already selected; the rest works.
      });
  }, []);

  useEffect(() => {
    if (problemId === null) {
      return;
    }

    let cancelled = false;
    setLoading(true);

    adminApi
      .get(problemId)
      .then((detail) => {
        if (!cancelled) {
          adopt(detail);
          setLoadError(null);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(caught);
          setLoadError(apiError.status === 404 ? t('solve.notFound') : message(apiError.code, apiError.message));
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
  }, [problemId, adopt, message, t]);

  /**
   * The browser's own guard against losing work.
   *
   * <p>The in-app back link asks its own question; this covers a closed tab or a
   * typed URL, which React Router never sees.
   */
  useEffect(() => {
    if (!dirty) {
      return;
    }

    const warn = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    globalThis.addEventListener('beforeunload', warn);

    return () => {
      globalThis.removeEventListener('beforeunload', warn);
    };
  }, [dirty]);

  /**
   * Applies an edit, and drops the server messages it has just answered.
   *
   * <p>A patch names the part of the document it touches — `examples`,
   * `testCases`, `functionName` — and the server's field paths are rooted in
   * those same names, so editing a section clears exactly that section's
   * complaints and leaves the rest standing until the next save.
   */
  const change = useCallback(
    (patch: Partial<ProblemFormState>) => {
      setForm((current) => ({ ...current, ...patch }));

      const touched = Object.keys(patch);
      clearMatching((field) =>
        touched.some((key) => field === key || field.startsWith(`${key}[`) || field.startsWith(`${key}.`)),
      );
    },
    [clearMatching],
  );

  const errorOf = useCallback(
    (field: string): string | undefined =>
      fieldErrors[field] === undefined ? undefined : message(fieldErrors[field]),
    [fieldErrors, message],
  );

  const publishErrors = useMemo(() => errorsFor('published'), [errorsFor]);

  const save = async () => {
    reset();
    setSaving(true);
    try {
      const payload = formToPayload(form);
      const detail = problemId === null ? await adminApi.create(payload) : await adminApi.update(problemId, payload);

      adopt(detail);
      setNotice(t('admin.form.saved'));

      if (problemId === null) {
        // Replaced rather than pushed: the "new problem" URL is not somewhere to
        // go back to once the problem exists.
        await navigate(`${paths.adminProblems}/${detail.id}/edit`, { replace: true });
      }
    } catch (caught) {
      capture(caught);
      // The failure is a list of field messages spread across a long form, so
      // the top of the page is where the summary belongs.
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setSaving(false);
    }
  };

  const leave = () => {
    void navigate(paths.adminProblems);
  };

  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Box sx={{ display: 'grid', placeItems: 'center', py: 12 }}>
          <CircularProgress />
        </Box>
      </Box>
    );
  }

  if (loadError) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Container maxWidth="md" sx={{ py: 6 }}>
          <Alert severity="error">{loadError}</Alert>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas', pb: 12 }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Stack spacing={3}>
          <Box>
            <MuiLink
              component="button"
              type="button"
              onClick={() => {
                if (dirty) {
                  setLeaving(true);
                  return;
                }
                leave();
              }}
              sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2', mb: 1.5 }}
            >
              <ArrowBackRounded sx={{ fontSize: 16 }} />
              {t('admin.form.backToList')}
            </MuiLink>

            <Stack direction="row" spacing={2} sx={{ alignItems: 'center', flexWrap: 'wrap' }} useFlexGap>
              <Typography variant="h1">
                {problemId === null ? t('admin.form.newTitle') : form.title || t('admin.form.untitled')}
              </Typography>
              {saved ? <ProblemStateChip state={saved.state} /> : null}
              {saved ? (
                <MuiLink
                  component={Link}
                  to={problemPath(saved.slug)}
                  sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2' }}
                >
                  <OpenInNewRounded sx={{ fontSize: 16 }} />
                  {t('admin.form.openInSolver')}
                </MuiLink>
              ) : null}
            </Stack>

            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {problemId === null ? t('admin.form.newSubtitle') : t('admin.form.editSubtitle')}
            </Typography>
          </Box>

          {generalError ? <Alert severity="error">{message(generalError.code, generalError.message)}</Alert> : null}
          {Object.keys(fieldErrors).length > 0 ? <Alert severity="error">{t('error.validation.failed')}</Alert> : null}

          {saved && saved.totalSubmissions > 0 ? (
            <Alert severity="info">{t('admin.form.hasHistory', { count: saved.totalSubmissions })}</Alert>
          ) : null}

          <ProblemBasicsSection
            form={form}
            onChange={change}
            errorOf={errorOf}
            publishErrors={publishErrors}
            tags={tags}
            onTagCreated={(tag) => {
              setTags((current) => [...current, tag].sort((a, b) => a.name.localeCompare(b.name)));
            }}
          />

          <ProblemStatementSection form={form} onChange={change} errorOf={errorOf} />
          <ProblemExamplesSection form={form} onChange={change} errorOf={errorOf} />
          <ProblemSignatureSection form={form} onChange={change} errorOf={errorOf} />
          <ProblemTestCasesSection
            form={form}
            onChange={change}
            errorOf={errorOf}
            problemId={problemId}
            dirty={dirty}
          />
          <ProblemHintsSection form={form} onChange={change} errorOf={errorOf} />
          <ProblemEditorialSection form={form} onChange={change} errorOf={errorOf} />
        </Stack>
      </Container>

      {/* The form is long enough that a footer at its end would be off-screen
          for most of the editing session. */}
      <Paper
        elevation={0}
        sx={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          borderTop: 1,
          borderColor: 'border.default',
          borderRadius: 0,
          backgroundColor: 'surface.paper',
          zIndex: 3,
        }}
      >
        <Container maxWidth="lg" sx={{ py: 1.5 }}>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <Typography variant="body2" sx={{ color: dirty ? 'warning.main' : 'text.disabled' }}>
              {dirty ? t('admin.form.unsaved') : t('admin.form.upToDate')}
            </Typography>
            <Box sx={{ flex: 1 }} />
            <Button
              variant="text"
              onClick={() => {
                if (dirty) {
                  setLeaving(true);
                  return;
                }
                leave();
              }}
            >
              {t('admin.form.cancel')}
            </Button>
            <Button
              startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveRounded />}
              disabled={saving}
              onClick={() => {
                void save();
              }}
            >
              {saving ? t('admin.form.saving') : t('admin.form.save')}
            </Button>
          </Stack>
        </Container>
      </Paper>

      <Dialog
        open={leaving}
        onClose={() => {
          setLeaving(false);
        }}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>{t('admin.form.leaveTitle')}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t('admin.form.leaveBody')}</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button
            variant="text"
            onClick={() => {
              setLeaving(false);
            }}
          >
            {t('admin.form.keepEditing')}
          </Button>
          <Button color="error" onClick={leave}>
            {t('admin.form.discard')}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={notice !== null}
        autoHideDuration={3000}
        onClose={() => {
          setNotice(null);
        }}
        message={notice}
      />
    </Box>
  );
};
