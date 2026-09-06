import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import ScheduleRounded from '@mui/icons-material/ScheduleRounded';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { toApiError } from '../api/api-error';
import { interviewsApi } from '../api/interviews-api';
import {
  type InterviewFormat,
  type InterviewFormatOption,
  type InterviewSession,
  type InterviewSummary,
} from '../api/types';
import { InterviewHistory } from '../components/interview/InterviewHistory';
import {
  FORMAT_BLURB_KEY,
  FORMAT_LABEL_KEY,
  formatClock,
} from '../components/interview/interview';
import { AppHeader } from '../components/layout/AppHeader';
import { DIFFICULTY_LABEL_KEY } from '../components/problems/difficulty';
import { useMessages } from '../i18n/use-messages';
import { interviewSessionPath } from '../routes/paths';

const HISTORY_SIZE = 10;

/** What a candidate is agreeing to, said before they start rather than after. */
const RULES = [
  'interview.rulesClock',
  'interview.rulesSequence',
  'interview.rulesEditorial',
  'interview.rulesHints',
  'interview.rulesUnseen',
  'interview.rulesPrivate',
] as const;

/**
 * The lobby: pick a format, or pick up the round you left running.
 *
 * <p>The one thing it does not offer is a choice of problems. Not knowing what
 * is coming is most of what separates a mock from practice, so the only decision
 * here is how long a round and how hard.
 */
export const InterviewPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();

  const [formats, setFormats] = useState<InterviewFormatOption[] | null>(null);
  const [active, setActive] = useState<InterviewSession | null>(null);
  const [history, setHistory] = useState<InterviewSummary[] | null>(null);
  const [starting, setStarting] = useState<InterviewFormat | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    interviewsApi
      .formats()
      .then((data) => {
        if (!cancelled) {
          setFormats(data);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setFormats([]);
        }
      });

    // A 404 here is the ordinary case — most visits have no round running — so
    // it is not an error state, just an absent one.
    interviewsApi
      .current()
      .then((session) => {
        if (!cancelled) {
          setActive(session);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setActive(null);
        }
      });

    interviewsApi
      .history(0, HISTORY_SIZE)
      .then((page) => {
        if (!cancelled) {
          setHistory(page.content);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setHistory([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleStart = useCallback(
    (format: InterviewFormat) => {
      setStarting(format);
      setError(null);

      interviewsApi
        .start(format)
        .then((session) => {
          navigate(interviewSessionPath(session.id));
        })
        .catch((caught: unknown) => {
          const apiError = toApiError(caught);
          setError(message(apiError.code, apiError.message));
          setStarting(null);
        });
    },
    [navigate, message],
  );

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h1">{t('interview.title')}</Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mt: 1 }}>
              {t('interview.subtitle')}
            </Typography>
          </Box>

          {error === null ? null : <Alert severity="error">{error}</Alert>}

          {active === null ? null : (
            <Paper variant="outlined" sx={{ p: 2.5, borderColor: 'primary.main' }}>
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
              >
                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                  <ScheduleRounded sx={{ color: 'primary.main' }} />
                  <Box>
                    <Typography variant="subtitle1">{t('interview.resumeTitle')}</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {t('interview.resumeBody', {
                        format: t(FORMAT_LABEL_KEY[active.format]),
                        time: formatClock(active.remainingSeconds),
                      })}
                    </Typography>
                  </Box>
                </Stack>

                <Button
                  component={Link}
                  to={interviewSessionPath(active.id)}
                  variant="contained"
                  endIcon={<ArrowForwardRounded />}
                  sx={{ flexShrink: 0 }}
                >
                  {t('interview.resume')}
                </Button>
              </Stack>
            </Paper>
          )}

          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
            {formats === null
              ? null
              : formats.map((option) => (
                  <Paper
                    key={option.format}
                    variant="outlined"
                    sx={{ p: 2.5, flex: '1 1 260px', display: 'flex', flexDirection: 'column' }}
                  >
                    <Stack spacing={1.5} sx={{ flex: 1 }}>
                      <Box>
                        <Typography variant="h3">{t(FORMAT_LABEL_KEY[option.format])}</Typography>
                        <Typography variant="body2" sx={{ color: 'text.disabled', mt: 0.25 }}>
                          {t('interview.formatShape', {
                            minutes: option.durationMinutes,
                            count: option.problemCount,
                          })}
                        </Typography>
                      </Box>

                      {/* The difficulties, in order, so "warm-up first" is
                          something the card shows rather than claims. */}
                      <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
                        {option.slots.map((difficulty, index) => (
                          <Chip
                            key={index}
                            size="small"
                            variant="outlined"
                            label={t(DIFFICULTY_LABEL_KEY[difficulty])}
                          />
                        ))}
                      </Stack>

                      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                        {t(FORMAT_BLURB_KEY[option.format])}
                      </Typography>
                    </Stack>

                    <Button
                      variant={option.format === 'STANDARD' ? 'contained' : 'soft'}
                      startIcon={
                        starting === option.format ? (
                          <CircularProgress size={14} color="inherit" />
                        ) : (
                          <PlayArrowRounded />
                        )
                      }
                      onClick={() => {
                        handleStart(option.format);
                      }}
                      disabled={starting !== null || active !== null}
                      sx={{ mt: 2 }}
                    >
                      {t('interview.start')}
                    </Button>
                  </Paper>
                ))}
          </Stack>

          <Paper variant="outlined" sx={{ p: 2.5 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              {t('interview.rulesTitle')}
            </Typography>
            <Stack component="ul" spacing={0.75} sx={{ m: 0, pl: 2.5 }}>
              {/* Literal keys, like everywhere else: a renamed translation should
                  be a compile error, not a line that renders as its own key. */}
              {RULES.map((rule) => (
                <Typography key={rule} component="li" variant="body2" sx={{ color: 'text.secondary' }}>
                  {t(rule)}
                </Typography>
              ))}
            </Stack>
          </Paper>

          <InterviewHistory interviews={history} />
        </Stack>
      </Container>
    </Box>
  );
};
