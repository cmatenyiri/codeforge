import CloseRounded from '@mui/icons-material/CloseRounded';
import {
  Alert,
  Box,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
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
import { toApiError } from '../../api/api-error';
import { submissionsApi } from '../../api/submissions-api';
import { type SubmissionDetail, type SubmissionSummary } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { VerdictChip } from './VerdictChip';
import { LANGUAGE_LABEL } from './verdict';

/** Absolute rather than relative ("3 minutes ago"): a history is read for what happened when. */
const useTimestamp = () => {
  const { i18n } = useTranslation();

  return (iso: string) =>
    new Intl.DateTimeFormat(i18n.resolvedLanguage, { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(iso),
    );
};

const SourceDialog = ({ submission, onClose }: { submission: SubmissionDetail | null; onClose: () => void }) => {
  const { t } = useTranslation();
  const timestamp = useTimestamp();

  return (
    <Dialog open={submission !== null} onClose={onClose} maxWidth="md" fullWidth>
      {submission ? (
        <>
          <DialogTitle sx={{ pr: 6 }}>
            <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
              <VerdictChip status={submission.status} />
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {LANGUAGE_LABEL[submission.language]}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.disabled' }}>
                {timestamp(submission.createdAt)}
              </Typography>
            </Stack>
            <IconButton
              onClick={onClose}
              aria-label={t('common.close')}
              sx={{ position: 'absolute', right: 12, top: 12 }}
            >
              <CloseRounded fontSize="small" />
            </IconButton>
          </DialogTitle>

          <DialogContent>
            <Stack spacing={2}>
              {submission.failureMessage ? (
                <Alert severity="warning" sx={{ '& .MuiAlert-message': { overflow: 'hidden' } }}>
                  <Typography variant="code" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {submission.failureMessage}
                  </Typography>
                </Alert>
              ) : null}

              <Paper variant="sunken" sx={{ p: 1.5, overflow: 'auto' }}>
                <Typography component="pre" variant="code" sx={{ m: 0, whiteSpace: 'pre' }}>
                  {submission.sourceCode}
                </Typography>
              </Paper>
            </Stack>
          </DialogContent>
        </>
      ) : null}
    </Dialog>
  );
};

/**
 * The caller's attempts at this problem, newest first.
 *
 * <p>`refreshKey` changes whenever a submission lands, which is what reloads the
 * list without this component knowing anything about the editor beside it.
 */
export const SubmissionsPanel = ({ slug, refreshKey }: { slug: string; refreshKey: number }) => {
  const { t } = useTranslation();
  const message = useMessages();
  const timestamp = useTimestamp();

  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
  const [selected, setSelected] = useState<SubmissionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    submissionsApi
      .listForProblem(slug)
      .then((page) => {
        if (!cancelled) {
          setSubmissions(page.content);
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
  }, [slug, refreshKey, message]);

  const open = (id: number) => {
    submissionsApi
      .get(id)
      .then(setSelected)
      .catch((caught: unknown) => {
        const apiError = toApiError(caught);
        setError(message(apiError.code, apiError.message));
      });
  };

  if (loading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', p: 6 }}>
        <CircularProgress size={22} />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2 }}>
      {error ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      ) : null}

      {submissions.length === 0 ? (
        <Stack spacing={0.5} sx={{ alignItems: 'center', py: 6 }}>
          <Typography variant="subtitle1">{t('solve.noSubmissions')}</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('solve.noSubmissionsBody')}
          </Typography>
        </Stack>
      ) : (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>{t('solve.status')}</TableCell>
              <TableCell>{t('solve.programmingLanguage')}</TableCell>
              <TableCell align="right">{t('solve.runtimeColumn')}</TableCell>
              <TableCell align="right">{t('solve.submittedAt')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {submissions.map((submission) => (
              <TableRow
                key={submission.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => {
                  open(submission.id);
                }}
              >
                <TableCell>
                  <Stack spacing={0.5}>
                    <VerdictChip status={submission.status} size="small" />
                    {submission.passedTests === undefined || submission.totalTests === undefined ? null : (
                      <Typography variant="caption" sx={{ color: 'text.disabled' }}>
                        {t('solve.casesPassed', {
                          passed: submission.passedTests,
                          total: submission.totalTests,
                        })}
                      </Typography>
                    )}
                  </Stack>
                </TableCell>
                <TableCell>
                  <Typography variant="body2">{LANGUAGE_LABEL[submission.language]}</Typography>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                    {submission.runtimeMs === undefined ? '—' : t('solve.runtime', { ms: submission.runtimeMs })}
                  </Typography>
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

      <SourceDialog
        submission={selected}
        onClose={() => {
          setSelected(null);
        }}
      />
    </Box>
  );
};
