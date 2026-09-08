import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import OpenInNewRounded from '@mui/icons-material/OpenInNewRounded';
import ReplayRounded from '@mui/icons-material/ReplayRounded';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  LinearProgress,
  Link as MuiLink,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { adminContestsApi } from '../api/admin-contests-api';
import { useApiErrors } from '../api/use-api-errors';
import { type AdminContestDetail, type ContestType } from '../api/types';
import { ContestProblemPicker, type ContestQuestion } from '../components/admin/ContestProblemPicker';
import { FormSection } from '../components/admin/FormSection';
import { ContestStatusChip } from '../components/contest/ContestStatusChip';
import { CONTEST_TYPE_LABEL_KEY } from '../components/contest/contest';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { adminContestEditPath, contestPath, paths } from '../routes/paths';

/** While a rejudge is running the screen polls for its progress. */
const REJUDGE_POLL_MS = 3_000;

const CONTEST_TYPES: ContestType[] = ['WEEKLY', 'BIWEEKLY', 'SPECIAL'];

/**
 * `<input type="datetime-local">` speaks local wall-clock time with no zone, so
 * the instant has to be converted in both directions rather than sliced off an
 * ISO string — which would silently shift every contest by the author's offset.
 */
const toLocalInput = (iso: string): string => {
  const date = new Date(iso);
  const pad = (value: number) => value.toString().padStart(2, '0');

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const fromLocalInput = (value: string): string => new Date(value).toISOString();

type FormState = {
  title: string;
  slug: string;
  description: string;
  type: ContestType;
  startsAt: string;
  durationMinutes: number;
  published: boolean;
  rated: boolean;
  questions: ContestQuestion[];
};

const EMPTY: FormState = {
  title: '',
  slug: '',
  description: '',
  type: 'WEEKLY',
  // Defaulted an hour out so a new contest is never accidentally in the past,
  // which the validator would refuse on the first save.
  startsAt: toLocalInput(new Date(Date.now() + 3_600_000).toISOString()),
  durationMinutes: 90,
  published: false,
  rated: true,
  questions: [],
};

const toForm = (contest: AdminContestDetail): FormState => ({
  title: contest.title,
  slug: contest.slug,
  description: contest.description ?? '',
  type: contest.type,
  startsAt: toLocalInput(contest.startsAt),
  durationMinutes: contest.durationMinutes,
  published: contest.published,
  rated: contest.rated,
  questions: contest.problems.map((problem) => ({
    problemId: problem.problemId,
    points: problem.points,
    title: problem.title,
    slug: problem.slug,
    difficulty: problem.difficulty,
    state: problem.state,
    solvable: problem.solvable,
  })),
});

/**
 * Writing a contest, and repairing one afterwards.
 *
 * <p>The form is a long document rather than a wizard, like the problem editor.
 * What is different is the bottom half: once a contest has started its questions
 * and its clock are settled — the server refuses to change them — and what is
 * left are the three deliberate acts of repair. Those are separated from the
 * save button on purpose. Each one reaches every competitor, and two of them
 * reach every rating since.
 */
export const AdminContestEditorPage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const contestId = id === undefined ? null : Number(id);

  const { fieldErrors, generalError, capture, reset, clearField, clearMatching } = useApiErrors();
  const [contest, setContest] = useState<AdminContestDetail | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [loading, setLoading] = useState(contestId !== null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState<'unrate' | 'rejudge' | 'delete' | null>(null);
  const [unratedReason, setUnratedReason] = useState('');

  const adopt = useCallback((data: AdminContestDetail) => {
    setContest(data);
    setForm(toForm(data));
    setUnratedReason(data.unratedReason ?? '');
  }, []);

  useEffect(() => {
    if (contestId === null) {
      return;
    }
    let cancelled = false;
    setLoading(true);

    adminContestsApi
      .get(contestId)
      .then((data) => {
        if (!cancelled) {
          adopt(data);
        }
      })
      .catch(capture)
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [contestId, adopt, capture]);

  // A rejudge outlives the request that asked for it, so the screen watches the
  // progress recorded on the contest rather than waiting on a response.
  useEffect(() => {
    if (contestId === null || contest?.rejudgeState !== 'RUNNING') {
      return;
    }

    const timer = setInterval(() => {
      adminContestsApi
        .get(contestId)
        .then(adopt)
        .catch(() => {
          // A failed poll is not worth surfacing: the next one is three seconds
          // away and the job is unaffected either way.
        });
    }, REJUDGE_POLL_MS);

    return () => {
      clearInterval(timer);
    };
  }, [contestId, contest?.rejudgeState, adopt]);

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    setSaved(false);
    clearField(key === 'questions' ? 'problems' : key);
  };

  const save = () => {
    reset();
    setSaving(true);

    const payload = {
      title: form.title,
      slug: form.slug,
      description: form.description === '' ? null : form.description,
      type: form.type,
      startsAt: fromLocalInput(form.startsAt),
      durationMinutes: form.durationMinutes,
      published: form.published,
      rated: form.rated,
      problems: form.questions.map((question) => ({
        problemId: question.problemId,
        points: question.points,
      })),
    };

    const call =
      contestId === null
        ? adminContestsApi.create(payload)
        : adminContestsApi.update(contestId, payload);

    call
      .then((data) => {
        adopt(data);
        setSaved(true);
        if (contestId === null) {
          void navigate(adminContestEditPath(data.id), { replace: true });
        }
      })
      .catch(capture)
      .finally(() => {
        setSaving(false);
      });
  };

  const act = (call: Promise<AdminContestDetail>) => {
    reset();
    setBusy(true);
    setConfirm(null);

    call
      .then(adopt)
      .catch(capture)
      .finally(() => {
        setBusy(false);
      });
  };

  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Box sx={{ display: 'grid', placeItems: 'center', py: 10 }}>
          <CircularProgress />
        </Box>
      </Box>
    );
  }

  const sealed = contest?.sealed === true;
  const ended = contest?.status === 'ENDED' || contest?.status === 'FINALIZED';
  const rejudging = contest?.rejudgeState === 'RUNNING';

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="md" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            <MuiLink
              component={Link}
              to={paths.adminContests}
              sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2', mb: 1.5 }}
            >
              <ArrowBackRounded sx={{ fontSize: 16 }} />
              {t('admin.contest.title')}
            </MuiLink>

            <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
              <Typography variant="h1">
                {contest === null ? t('admin.contest.new') : contest.title}
              </Typography>
              {contest ? <ContestStatusChip status={contest.status} /> : null}
              {contest?.rated === false ? <Chip label={t('contest.unrated')} /> : null}
              {sealed ? <Chip label={t('admin.contest.sealed')} variant="outlined" /> : null}
              {contest?.ratingsAppliedAt ? (
                <Chip label={t('admin.contest.ratingsApplied')} variant="outlined" />
              ) : null}
            </Stack>

            {contest ? (
              <MuiLink
                component={Link}
                to={contestPath(contest.slug)}
                sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, typography: 'body2', mt: 1 }}
              >
                <OpenInNewRounded sx={{ fontSize: 16 }} />
                {t('admin.contest.openContest')}
              </MuiLink>
            ) : null}
          </Box>

          {generalError ? (
            <Alert severity="error">{message(generalError.code, generalError.message)}</Alert>
          ) : null}
          {saved ? <Alert severity="success">{t('admin.contest.saved')}</Alert> : null}

          {sealed ? (
            <Alert severity="info">
              <AlertTitle>{t('admin.contest.sealed')}</AlertTitle>
              {t('admin.contest.sealedHint')}
            </Alert>
          ) : null}

          <FormSection title={t('admin.contest.basics')}>
            <TextField
              label={t('admin.contest.titleField')}
              value={form.title}
              onChange={(event) => {
                update('title', event.target.value);
              }}
              error={Boolean(fieldErrors.title)}
              helperText={fieldErrors.title ? message(fieldErrors.title) : ' '}
              fullWidth
            />

            <TextField
              label={t('admin.contest.slug')}
              value={form.slug}
              onChange={(event) => {
                update('slug', event.target.value);
              }}
              error={Boolean(fieldErrors.slug)}
              helperText={fieldErrors.slug ? message(fieldErrors.slug) : t('admin.contest.slugHelp')}
              fullWidth
            />

            <TextField
              label={t('admin.contest.description')}
              value={form.description}
              onChange={(event) => {
                update('description', event.target.value);
              }}
              helperText={t('admin.contest.descriptionHelp')}
              multiline
              minRows={3}
              fullWidth
            />

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                select
                label={t('admin.contest.type')}
                value={form.type}
                onChange={(event) => {
                  update('type', event.target.value as ContestType);
                }}
                sx={{ minWidth: 180 }}
              >
                {CONTEST_TYPES.map((type) => (
                  <MenuItem key={type} value={type}>
                    {t(CONTEST_TYPE_LABEL_KEY[type])}
                  </MenuItem>
                ))}
              </TextField>

              <TextField
                type="datetime-local"
                label={t('admin.contest.startsAtField')}
                value={form.startsAt}
                onChange={(event) => {
                  update('startsAt', event.target.value);
                }}
                disabled={sealed}
                error={Boolean(fieldErrors.startsAt)}
                helperText={fieldErrors.startsAt ? message(fieldErrors.startsAt) : t('admin.contest.startsAtHelp')}
                slotProps={{ inputLabel: { shrink: true } }}
                sx={{ flex: 1 }}
              />

              <TextField
                type="number"
                label={t('admin.contest.duration')}
                value={form.durationMinutes}
                onChange={(event) => {
                  update('durationMinutes', Number(event.target.value));
                }}
                disabled={sealed}
                error={Boolean(fieldErrors.durationMinutes)}
                helperText={fieldErrors.durationMinutes ? message(fieldErrors.durationMinutes) : ' '}
                sx={{ width: 160 }}
              />
            </Stack>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
              <FormControlLabel
                control={
                  <Switch
                    checked={form.published}
                    onChange={(event) => {
                      update('published', event.target.checked);
                    }}
                  />
                }
                label={
                  <Box>
                    <Typography variant="body2">{t('admin.contest.published')}</Typography>
                    <Typography variant="caption" sx={{ color: 'text.disabled' }}>
                      {t('admin.contest.publishedHelp')}
                    </Typography>
                  </Box>
                }
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={form.rated}
                    disabled={sealed}
                    onChange={(event) => {
                      update('rated', event.target.checked);
                    }}
                  />
                }
                label={
                  <Box>
                    <Typography variant="body2">{t('admin.contest.ratedField')}</Typography>
                    <Typography variant="caption" sx={{ color: 'text.disabled' }}>
                      {t('admin.contest.ratedHelp')}
                    </Typography>
                  </Box>
                }
              />
            </Stack>
          </FormSection>

          <FormSection title={t('admin.contest.questions')} description={t('admin.contest.questionsHelp')}>
            {fieldErrors.problems ? <Alert severity="error">{message(fieldErrors.problems)}</Alert> : null}
            <ContestProblemPicker
              questions={form.questions}
              onChange={(questions) => {
                setForm((current) => ({ ...current, questions }));
                setSaved(false);
                // The inputs are indexed paths, so touching the list has to clear
                // every message under it rather than one.
                clearMatching((field) => field.startsWith('problems'));
              }}
              errorFor={(index) => {
                const code = fieldErrors[`problems[${index}].problemId`];
                return code === undefined ? undefined : message(code);
              }}
              disabled={sealed}
            />
          </FormSection>

          <Stack direction="row" spacing={2}>
            <Button onClick={save} disabled={saving}>
              {t(saving ? 'admin.contest.saving' : contestId === null ? 'admin.contest.create' : 'admin.contest.save')}
            </Button>
            {contest && contest.participantCount === 0 ? (
              <Button
                color="error"
                variant="text"
                onClick={() => {
                  setConfirm('delete');
                }}
              >
                {t('admin.contest.delete')}
              </Button>
            ) : null}
          </Stack>

          {/* ── Settling and repairing ─────────────────────────────────────
              Deliberately below the save button and visually separate. Each of
              these reaches every competitor, and two of them reach every rating
              since — they are not edits, and should not look like one. */}
          {contest && ended ? (
            <FormSection title={t('admin.contest.rejudgeTitle')} description={t('admin.contest.rejudgeHint')}>
              {rejudging ? (
                <Box>
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    {t('admin.contest.rejudgeRunning', {
                      done: contest.rejudgeDone ?? 0,
                      total: contest.rejudgeTotal ?? 0,
                    })}
                  </Typography>
                  <LinearProgress
                    variant={contest.rejudgeTotal ? 'determinate' : 'indeterminate'}
                    value={
                      contest.rejudgeTotal
                        ? ((contest.rejudgeDone ?? 0) / contest.rejudgeTotal) * 100
                        : undefined
                    }
                  />
                </Box>
              ) : (
                <Stack spacing={1.5}>
                  {contest.rejudgeState === 'FAILED' ? (
                    <Alert severity="error">
                      <AlertTitle>{t('admin.contest.rejudgeFailed')}</AlertTitle>
                      {contest.rejudgeError}
                    </Alert>
                  ) : contest.rejudgeState === 'COMPLETED' && contest.rejudgeFinishedAt ? (
                    <Alert severity="success">
                      {t('admin.contest.rejudgeDone')} ·{' '}
                      {new Date(contest.rejudgeFinishedAt).toLocaleString(i18n.language)}
                    </Alert>
                  ) : null}

                  <Box>
                    <Button
                      variant="outlined"
                      startIcon={<ReplayRounded />}
                      disabled={busy}
                      onClick={() => {
                        setConfirm('rejudge');
                      }}
                    >
                      {t('admin.contest.rejudge')}
                    </Button>
                  </Box>
                </Stack>
              )}

              <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }} useFlexGap>
                {contest.ratingsAppliedAt === undefined && contest.rated ? (
                  <Button
                    disabled={busy || rejudging}
                    onClick={() => {
                      act(adminContestsApi.finalize(contest.id));
                    }}
                  >
                    {t(busy ? 'admin.contest.settling' : 'admin.contest.settle')}
                  </Button>
                ) : null}

                {contest.rated ? (
                  <Button
                    color="error"
                    variant="outlined"
                    disabled={busy || rejudging}
                    onClick={() => {
                      setConfirm('unrate');
                    }}
                  >
                    {t('admin.contest.makeUnrated')}
                  </Button>
                ) : (
                  <Button
                    variant="outlined"
                    disabled={busy || rejudging}
                    onClick={() => {
                      act(adminContestsApi.setRated(contest.id, true));
                    }}
                  >
                    {t('admin.contest.makeRated')}
                  </Button>
                )}
              </Stack>

              {contest.ratingsAppliedAt === undefined && contest.rated ? (
                <Typography variant="caption" sx={{ color: 'text.disabled' }}>
                  {t('admin.contest.settleHint')}
                </Typography>
              ) : null}
            </FormSection>
          ) : null}
        </Stack>
      </Container>

      <Dialog open={confirm !== null} onClose={() => { setConfirm(null); }} maxWidth="sm" fullWidth>
        <DialogTitle>
          {confirm === 'unrate'
            ? t('admin.contest.unratedConfirm')
            : confirm === 'rejudge'
              ? t('admin.contest.rejudgeConfirm')
              : t('admin.contest.deleteConfirm')}
        </DialogTitle>
        <DialogContent>
          {confirm === 'unrate' ? (
            <Stack spacing={2}>
              <DialogContentText>{t('admin.contest.unratedConfirmBody')}</DialogContentText>
              <TextField
                label={t('admin.contest.unratedReason')}
                helperText={t('admin.contest.unratedReasonHelp')}
                value={unratedReason}
                onChange={(event) => {
                  setUnratedReason(event.target.value);
                }}
                multiline
                minRows={2}
                fullWidth
              />
            </Stack>
          ) : confirm === 'rejudge' ? (
            <DialogContentText>{t('admin.contest.rejudgeConfirmBody')}</DialogContentText>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button variant="text" onClick={() => { setConfirm(null); }}>
            {t('common.cancel')}
          </Button>
          <Button
            color={confirm === 'delete' ? 'error' : 'primary'}
            onClick={() => {
              if (contest === null) {
                return;
              }
              if (confirm === 'unrate') {
                act(adminContestsApi.setRated(contest.id, false, unratedReason));
              } else if (confirm === 'rejudge') {
                act(adminContestsApi.rejudge(contest.id));
              } else {
                setConfirm(null);
                adminContestsApi
                  .remove(contest.id)
                  .then(() => navigate(paths.adminContests))
                  .catch(capture);
              }
            }}
          >
            {t('common.confirm')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
