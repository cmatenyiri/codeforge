import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import RadioButtonUncheckedRounded from '@mui/icons-material/RadioButtonUncheckedRounded';
import ReplayRounded from '@mui/icons-material/ReplayRounded';
import SkipNextRounded from '@mui/icons-material/SkipNextRounded';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Divider,
  FormControlLabel,
  Link as MuiLink,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { interviewsApi } from '../api/interviews-api';
import { type InterviewProblemResult, type InterviewReport } from '../api/types';
import {
  FORMAT_LABEL_KEY,
  INSIGHT_IS_PRAISE,
  INSIGHT_LABEL_KEY,
  OUTCOME_BLURB_KEY,
  OUTCOME_LABEL_KEY,
  OUTCOME_TOKEN,
  formatClock,
} from '../components/interview/interview';
import { AppHeader } from '../components/layout/AppHeader';
import { DifficultyChip } from '../components/problems/DifficultyChip';
import { useMessages } from '../i18n/use-messages';
import { paths, problemPath } from '../routes/paths';

const StatTile = ({ label, value, hint }: { label: string; value: string; hint?: string }) => (
  <Paper variant="outlined" sx={{ p: 2, flex: '1 1 150px' }}>
    <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
      {label}
    </Typography>
    <Typography variant="metric">{value}</Typography>
    {hint === undefined ? null : (
      <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block' }}>
        {hint}
      </Typography>
    )}
  </Paper>
);

/** Solved beats skipped: coming back to a problem and getting it is a solve. */
const ResultChip = ({ result }: { result: InterviewProblemResult }) => {
  const { t } = useTranslation();

  if (result.solved) {
    return (
      <Chip
        icon={<CheckCircleRounded />}
        label={t('interview.resultSolved')}
        sx={{
          color: 'verdict.accepted',
          backgroundColor: 'verdict.acceptedBg',
          '& .MuiChip-icon': { color: 'inherit' },
        }}
      />
    );
  }

  return (
    <Chip
      icon={result.skipped ? <SkipNextRounded /> : <RadioButtonUncheckedRounded />}
      label={result.skipped ? t('interview.resultSkipped') : t('interview.resultUnsolved')}
      sx={{
        color: 'text.secondary',
        backgroundColor: 'surface.hover',
        '& .MuiChip-icon': { color: 'inherit' },
      }}
    />
  );
};

/**
 * The debrief.
 *
 * <p>Shaped like the one a real mock ends with: a band, the round broken down
 * problem by problem, and a few notes on how the time went — rather than a
 * percentage to optimise. None of it is comparable with anybody else's, which is
 * the point; the numbers are here to be acted on, not ranked.
 *
 * <p>This is also where everything the round withheld comes back: each problem
 * links to its catalogue page, editorial and all.
 */
export const InterviewReportPage = () => {
  const { t, i18n } = useTranslation();
  const message = useMessages();
  const { id } = useParams<{ id: string }>();
  const interviewId = Number(id);

  const [report, setReport] = useState<InterviewReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(interviewId)) {
      return;
    }

    let cancelled = false;
    interviewsApi
      .report(interviewId)
      .then((data) => {
        if (!cancelled) {
          setReport(data);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          const apiError = toApiError(caught);
          setError(
            apiError.status === 404
              ? t('interview.sessionNotFound')
              : message(apiError.code, apiError.message),
          );
        }
      });

    return () => {
      cancelled = true;
    };
  }, [interviewId, message, t]);

  const handleSelfReport = useCallback(
    (usedOutsideHelp: boolean) => {
      setSaving(true);
      interviewsApi
        .selfReport(interviewId, usedOutsideHelp)
        .then(setReport)
        .catch((caught: unknown) => {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
        })
        .finally(() => {
          setSaving(false);
        });
    },
    [interviewId, message],
  );

  if (error !== null) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Container maxWidth="md" sx={{ py: 5 }}>
          <Alert severity="error">{error}</Alert>
        </Container>
      </Box>
    );
  }

  if (report === null) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }

  const abandoned = report.status === 'ABANDONED';
  const running = report.status === 'IN_PROGRESS';
  const token = report.outcome === undefined ? 'pending' : OUTCOME_TOKEN[report.outcome];
  const started = new Intl.DateTimeFormat(i18n.resolvedLanguage, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(report.startedAt));

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
              <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
                {`${t(FORMAT_LABEL_KEY[report.format])} · ${started}`}
              </Typography>
              <Typography variant="h1">{t('interview.reportTitle')}</Typography>
            </Box>

            <Button
              component={Link}
              to={paths.interviews}
              variant="contained"
              endIcon={<ReplayRounded />}
              sx={{ flexShrink: 0 }}
            >
              {t('interview.startAnother')}
            </Button>
          </Stack>

          {/* An abandoned round gets no band. Quitting five minutes in is not a
              failed interview, and scoring it as one makes the history a lie. */}
          <Paper
            variant="outlined"
            sx={{
              p: 3,
              borderColor: abandoned || running ? 'border.default' : `verdict.${token}`,
              backgroundColor: abandoned || running ? 'surface.paper' : `verdict.${token}Bg`,
            }}
          >
            {abandoned ? (
              <Stack spacing={0.5}>
                <Typography variant="h3">{t('interview.abandonedTitle')}</Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('interview.abandonedBody')}
                </Typography>
              </Stack>
            ) : running ? (
              <Stack spacing={0.5}>
                <Typography variant="h3">{t('interview.stillRunningTitle')}</Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('interview.stillRunningBody')}
                </Typography>
              </Stack>
            ) : report.outcome === undefined ? null : (
              <Stack spacing={0.5}>
                <Typography variant="h3" sx={{ color: `verdict.${token}` }}>
                  {t(OUTCOME_LABEL_KEY[report.outcome])}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t(OUTCOME_BLURB_KEY[report.outcome])}
                </Typography>
              </Stack>
            )}
          </Paper>

          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
            <StatTile
              label={t('interview.solved')}
              value={`${report.solved} / ${report.total}`}
            />
            <StatTile
              label={t('interview.timeUsed')}
              value={formatClock(report.elapsedSeconds)}
              hint={t('interview.ofBudget', { budget: formatClock(report.durationMinutes * 60) })}
            />
            <StatTile label={t('interview.attempts')} value={`${report.attempts}`} />
            <StatTile label={t('interview.hints')} value={`${report.hintsRevealed}`} />
          </Stack>

          {report.insights.length === 0 ? null : (
            <Paper variant="outlined" sx={{ p: 2.5 }}>
              <Typography variant="subtitle1" sx={{ mb: 1.5 }}>
                {t('interview.notes')}
              </Typography>
              <Stack spacing={1.25}>
                {report.insights.map((insight) => (
                  <Stack key={insight} direction="row" spacing={1.25} sx={{ alignItems: 'flex-start' }}>
                    {/* The dot is centred against the first line by giving its
                        box that line's exact height, rather than by guessing a
                        top margin — which leaves it a pixel or two high and
                        drifts again the moment the type scale changes. */}
                    <Box
                      sx={(theme) => ({
                        flexShrink: 0,
                        display: 'flex',
                        alignItems: 'center',
                        height: `calc(${theme.typography.body2.fontSize} * ${theme.typography.body2.lineHeight})`,
                      })}
                    >
                      <Box
                        sx={{
                          width: 6,
                          height: 6,
                          borderRadius: '50%',
                          backgroundColor: INSIGHT_IS_PRAISE[insight]
                            ? 'verdict.accepted'
                            : 'verdict.timeLimit',
                        }}
                      />
                    </Box>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {t(INSIGHT_LABEL_KEY[insight])}
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            </Paper>
          )}

          <Paper variant="outlined">
            <Typography variant="subtitle1" sx={{ px: 2, py: 1.5 }}>
              {t('interview.breakdown')}
            </Typography>
            <Divider />
            <Box sx={{ overflowX: 'auto' }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>{t('problems.problem')}</TableCell>
                    <TableCell width={110}>{t('problems.difficulty')}</TableCell>
                    <TableCell width={140}>{t('solve.status')}</TableCell>
                    <TableCell width={110} align="right">
                      {t('interview.timeToSolve')}
                    </TableCell>
                    <TableCell width={90} align="right">
                      {t('interview.attempts')}
                    </TableCell>
                    <TableCell width={80} align="right">
                      {t('interview.hints')}
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {report.problems.map((result) => (
                    <TableRow key={result.position} hover>
                      <TableCell>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          {/* Everything the round withheld is one click away now. */}
                          <MuiLink
                            component={Link}
                            to={problemPath(result.slug)}
                            underline="hover"
                            sx={{ typography: 'body2', fontWeight: 550, color: 'text.primary' }}
                          >
                            {result.title}
                          </MuiLink>
                          {result.warmUp ? (
                            <Chip size="small" label={t('interview.warmUp')} variant="outlined" />
                          ) : null}
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <DifficultyChip difficulty={result.difficulty} />
                      </TableCell>
                      <TableCell>
                        <ResultChip result={result} />
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                          {result.timeToSolveSeconds === undefined
                            ? '—'
                            : formatClock(result.timeToSolveSeconds)}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                          {result.attempts}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                          {result.hintsRevealed}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </Paper>

          {/* Nothing enforces this and nothing is deducted for it. A self-guided
              mock has no stakes to cheat for, so the only thing worth building is
              a way to keep the record honest for the one person who reads it. */}
          <Paper variant="outlined" sx={{ p: 2.5 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={report.usedOutsideHelp === true}
                  disabled={saving}
                  onChange={(event) => {
                    handleSelfReport(event.target.checked);
                  }}
                />
              }
              label={<Typography variant="body2">{t('interview.usedOutsideHelp')}</Typography>}
              // The theme strips MuiSwitch's own padding, which leaves the label
              // touching the track at this size.
              sx={{ gap: 1.25, ml: 0 }}
            />
            <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block', mt: 0.5 }}>
              {t('interview.usedOutsideHelpHint')}
            </Typography>
          </Paper>

          <Box>
            <Button component={Link} to={paths.problems} variant="text" endIcon={<ArrowForwardRounded />}>
              {t('interview.reviewInCatalogue')}
            </Button>
          </Box>
        </Stack>
      </Container>
    </Box>
  );
};
