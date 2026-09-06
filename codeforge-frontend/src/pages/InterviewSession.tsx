import { Alert, Box, CircularProgress, Divider, Stack } from '@mui/material';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';
import { toApiError } from '../api/api-error';
import { interviewsApi } from '../api/interviews-api';
import { type InterviewProblemDetail, type InterviewSession } from '../api/types';
import { InterviewEditorPanel } from '../components/interview/InterviewEditorPanel';
import { InterviewProblemPanel } from '../components/interview/InterviewProblemPanel';
import { InterviewToolbar } from '../components/interview/InterviewToolbar';
import { RESYNC_INTERVAL_MS, useInterviewClock } from '../components/interview/use-interview-clock';
import { AppHeader } from '../components/layout/AppHeader';
import { useMessages } from '../i18n/use-messages';
import { interviewReportPath, paths } from '../routes/paths';

/**
 * The interview workspace: clock on top, problem left, editor right.
 *
 * <p>Two things make it different from the solving page, and both are about the
 * clock rather than the code. The countdown is resynchronised against the server
 * on a timer and after every action, because the server's answer is the only one
 * that decides anything. And the moment the round is over — the buzzer, the
 * finish button, or another tab having ended it — the page leaves for the
 * report rather than sitting on a workspace that can no longer submit.
 */
export const InterviewSessionPage = () => {
  const { t } = useTranslation();
  const message = useMessages();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const interviewId = Number(id);

  const [session, setSession] = useState<InterviewSession | null>(null);
  const [problem, setProblem] = useState<InterviewProblemDetail | null>(null);
  // -1 until the first session read says which problem the round is on, so a
  // resumed round opens where it was left rather than always on problem 1.
  const [position, setPosition] = useState(-1);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [judging, setJudging] = useState(false);
  const [loading, setLoading] = useState(true);

  const remaining = useInterviewClock(session);
  const running = session?.status === 'IN_PROGRESS';
  const sessionLoaded = session !== null;

  // Guards the one-shot re-check when the local clock reaches zero, so a stalled
  // navigation cannot turn into a request per second.
  const expiryChecked = useRef(false);

  /**
   * Adopts a server answer as the truth about the round.
   *
   * <p>Also re-arms the expiry check: if the browser's clock was simply running
   * fast, the server hands back time that is still on the round, and the local
   * countdown has to be allowed to reach zero again.
   */
  const applySession = useCallback((next: InterviewSession) => {
    expiryChecked.current = next.remainingSeconds <= 0;
    setSession(next);
    // Never leave the screen pointing at a problem the round has not reached:
    // fetching one is refused, so it would render an error rather than a
    // workspace. The last problem is the fallback once the set is finished.
    setPosition((current) => {
      const reachable = next.problems.filter((slot) => !slot.locked);
      if (reachable.some((slot) => slot.position === current)) {
        return current;
      }
      return next.activePosition ?? reachable.at(-1)?.position ?? 0;
    });
    return next;
  }, []);

  const refreshSession = useCallback(
    async () => applySession(await interviewsApi.session(interviewId)),
    [applySession, interviewId],
  );

  const report = useCallback(() => {
    navigate(interviewReportPath(interviewId), { replace: true });
  }, [navigate, interviewId]);

  const fail = useCallback(
    (caught: unknown) => {
      const apiError = toApiError(caught);
      setError(
        apiError.status === 404 ? t('interview.sessionNotFound') : message(apiError.code, apiError.message),
      );
    },
    [message, t],
  );

  // Initial load, then a slow poll. The poll is a correction, not the clock:
  // between ticks the countdown runs locally, and this is what stops it drifting.
  useEffect(() => {
    if (!Number.isFinite(interviewId)) {
      navigate(paths.interviews, { replace: true });
      return;
    }

    let cancelled = false;

    const load = () => {
      interviewsApi
        .session(interviewId)
        .then((next) => {
          if (!cancelled) {
            applySession(next);
            setError(null);
          }
        })
        .catch((caught: unknown) => {
          if (!cancelled) {
            fail(caught);
          }
        })
        .finally(() => {
          if (!cancelled) {
            setLoading(false);
          }
        });
    };

    load();
    const timer = setInterval(load, RESYNC_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [interviewId, navigate, fail, applySession]);

  // The round is over, however it ended — go and read about it. Not while the
  // judge still has a submission, though: one started with ten seconds left is
  // still counted, and walking away mid-verdict would show a debrief that is one
  // submission out of date.
  useEffect(() => {
    if (session !== null && session.status !== 'IN_PROGRESS' && !judging) {
      report();
    }
  }, [session, judging, report]);

  // The local clock reached zero. Ask the server rather than assuming: it is the
  // one that decides, and it is the call that closes the round out.
  useEffect(() => {
    if (!running || remaining > 0 || expiryChecked.current) {
      return;
    }
    expiryChecked.current = true;
    refreshSession().catch(fail);
  }, [running, remaining, refreshSession, fail]);

  // The problem is fetched only when it is actually put on screen, because that
  // fetch is what starts its clock.
  useEffect(() => {
    if (!sessionLoaded || position < 0) {
      return;
    }

    let cancelled = false;
    setProblem(null);

    interviewsApi
      .problem(interviewId, position)
      .then((next) => {
        if (!cancelled) {
          setProblem(next);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          fail(caught);
        }
      });

    return () => {
      cancelled = true;
    };
    // Keyed on whether a session has loaded rather than on the session itself:
    // a poll every twenty seconds must not re-fetch the problem and blank the
    // panel under the candidate.
  }, [interviewId, position, sessionLoaded, fail]);

  const act = useCallback(
    (action: () => Promise<unknown>) => {
      setBusy(true);
      setError(null);
      action()
        .catch(fail)
        .finally(() => {
          setBusy(false);
        });
    },
    [fail],
  );

  const handleSubmitted = useCallback(() => {
    // The header's solved count and the tab ticks come off the session, so it is
    // re-read after every verdict.
    refreshSession()
      .then((next) => {
        if (next.status === 'IN_PROGRESS') {
          return interviewsApi.problem(interviewId, position).then(setProblem);
        }
        return undefined;
      })
      .catch(fail);
  }, [refreshSession, interviewId, position, fail]);

  const handleRevealHint = useCallback(() => {
    act(async () => {
      const revealed = await interviewsApi.revealHint(interviewId, position);
      setProblem((current) =>
        current === null ? current : { ...current, hints: revealed.hints, hintCount: revealed.hintCount },
      );
      await refreshSession();
    });
  }, [act, interviewId, position, refreshSession]);

  const handleSkip = useCallback(() => {
    act(async () => {
      const next = await interviewsApi.skip(interviewId, position);
      applySession(next);
      // Skipping is only ever "move me on", so go there rather than leaving the
      // candidate looking at the problem they just gave up.
      if (next.activePosition !== undefined) {
        setPosition(next.activePosition);
      }
    });
  }, [act, applySession, interviewId, position]);

  const handleFinish = useCallback(() => {
    act(async () => {
      await interviewsApi.finish(interviewId);
      report();
    });
  }, [act, interviewId, report]);

  const handleAbandon = useCallback(() => {
    act(async () => {
      await interviewsApi.abandon(interviewId);
      navigate(paths.interviews, { replace: true });
    });
  }, [act, interviewId, navigate]);

  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (session === null) {
    return (
      <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
        <AppHeader />
        <Box sx={{ p: 3 }}>
          <Alert severity="error">{error ?? t('interview.sessionNotFound')}</Alert>
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <InterviewToolbar
        session={session}
        remainingSeconds={remaining}
        activePosition={position}
        onSelectPosition={setPosition}
        onSkip={handleSkip}
        onFinish={handleFinish}
        onAbandon={handleAbandon}
        busy={busy}
      />

      {error === null ? null : (
        <Alert severity="error" sx={{ mx: 2, mt: 1.5 }}>
          {error}
        </Alert>
      )}

      {problem === null || position < 0 ? (
        <Box sx={{ flex: 1, display: 'grid', placeItems: 'center' }}>
          <CircularProgress />
        </Box>
      ) : (
        <Stack
          direction={{ xs: 'column', lg: 'row' }}
          divider={<Divider orientation="vertical" flexItem />}
          sx={{ flex: 1, minHeight: 0, p: 2, gap: { xs: 2, lg: 0 } }}
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
            <InterviewProblemPanel
              problem={problem}
              onRevealHint={handleRevealHint}
              revealing={busy}
              canRevealHints={running === true && problem.editable}
            />
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
            <InterviewEditorPanel
              interviewId={interviewId}
              problem={problem}
              onSubmitted={handleSubmitted}
              onJudgingChange={setJudging}
              editable={running === true && problem.editable}
            />
          </Box>
        </Stack>
      )}
    </Box>
  );
};
