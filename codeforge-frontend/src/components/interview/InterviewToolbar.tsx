import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import FlagRounded from '@mui/icons-material/FlagRounded';
import LockRounded from '@mui/icons-material/LockRounded';
import SkipNextRounded from '@mui/icons-material/SkipNextRounded';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type InterviewSession } from '../../api/types';
import { InterviewTimer } from './InterviewTimer';
import { FORMAT_LABEL_KEY } from './interview';

type InterviewToolbarProps = {
  session: InterviewSession;
  remainingSeconds: number;
  activePosition: number;
  onSelectPosition: (position: number) => void;
  onSkip: () => void;
  onFinish: () => void;
  onAbandon: () => void;
  busy: boolean;
};

type Confirmation = 'finish' | 'abandon' | 'skip' | null;

/** Literal keys, so a renamed translation is a compile error rather than a stray key on screen. */
const CONFIRM_TITLE = {
  finish: 'interview.finishTitle',
  abandon: 'interview.abandonTitle',
  skip: 'interview.skipTitle',
} as const;

const CONFIRM_LABEL = {
  finish: 'interview.finish',
  abandon: 'interview.abandon',
  skip: 'interview.skipConfirm',
} as const;

/**
 * The bar that runs the round: the clock, which problem is on screen, and the
 * two ways out of it.
 *
 * <p>Finishing and abandoning both ask first. They are the only irreversible
 * things on the screen, and both are one click away from a candidate reaching
 * for the tab strip.
 */
export const InterviewToolbar = ({
  session,
  remainingSeconds,
  activePosition,
  onSelectPosition,
  onSkip,
  onFinish,
  onAbandon,
  busy,
}: InterviewToolbarProps) => {
  const { t } = useTranslation();
  const [confirming, setConfirming] = useState<Confirmation>(null);

  const solved = session.problems.filter((slot) => slot.solved).length;
  const shown = session.problems.find((slot) => slot.position === activePosition);
  const allSolved = solved === session.problems.length;
  // Nothing left to work on: every problem is solved or skipped, so the only
  // move left is to finish.
  const setFinished = session.activePosition === undefined;
  // Skipping is offered only on the problem actually in play.
  const canSkip = shown !== undefined && !shown.locked && !shown.resolved;

  const confirm = () => {
    const action = confirming;
    setConfirming(null);
    if (action === 'finish') {
      onFinish();
    } else if (action === 'abandon') {
      onAbandon();
    } else if (action === 'skip') {
      onSkip();
    }
  };



  return (
    <>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{
          px: 2,
          py: 1.25,
          alignItems: { md: 'center' },
          borderBottom: 1,
          borderColor: 'border.default',
          backgroundColor: 'surface.paper',
          flexShrink: 0,
        }}
      >
        <InterviewTimer remainingSeconds={remainingSeconds} durationMinutes={session.durationMinutes} />

        <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', md: 'block' } }} />

        <Box sx={{ minWidth: 0 }}>
          <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
            {t(FORMAT_LABEL_KEY[session.format])}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('interview.solvedOf', { solved, total: session.problems.length })}
          </Typography>
        </Box>

        <Tabs
          value={activePosition}
          onChange={(_, next: number) => {
            onSelectPosition(next);
          }}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ flex: 1, minWidth: 0 }}
        >
          {session.problems.map((slot) => (
            <Tab
              key={slot.position}
              value={slot.position}
              // A locked problem is shown but not reachable: hiding it entirely
              // would leave the candidate unsure how many questions are coming,
              // which a real interviewer tells you.
              disabled={slot.locked}
              iconPosition="end"
              icon={
                slot.locked ? (
                  <LockRounded sx={{ fontSize: 15 }} />
                ) : slot.solved ? (
                  <CheckCircleRounded sx={{ fontSize: 16, color: 'verdict.accepted' }} />
                ) : undefined
              }
              label={
                <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                  <span>{t('interview.problemNumber', { index: slot.position + 1 })}</span>
                  {slot.warmUp ? (
                    <Chip size="small" label={t('interview.warmUp')} variant="outlined" />
                  ) : null}
                  {slot.skipped && !slot.solved ? (
                    <Chip size="small" label={t('interview.skipped')} variant="outlined" />
                  ) : null}
                </Stack>
              }
            />
          ))}
        </Tabs>

        <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
          {/* Offered only on the problem in play, and only while another one is
              still to come — skipping the last question just ends the round, and
              "Finish" already says that more plainly. */}
          {canSkip && activePosition < session.problems.length - 1 ? (
            <Button
              size="small"
              variant="text"
              startIcon={<SkipNextRounded />}
              onClick={() => {
                setConfirming('skip');
              }}
              disabled={busy}
            >
              {t('interview.skip')}
            </Button>
          ) : null}

          <Button
            size="small"
            variant={allSolved || setFinished ? 'contained' : 'soft'}
            color={allSolved ? 'success' : 'primary'}
            startIcon={<FlagRounded />}
            onClick={() => {
              setConfirming('finish');
            }}
            disabled={busy}
          >
            {t('interview.finish')}
          </Button>

          <Button
            size="small"
            variant="text"
            color="error"
            onClick={() => {
              setConfirming('abandon');
            }}
            disabled={busy}
          >
            {t('interview.abandon')}
          </Button>
        </Stack>
      </Stack>

      <Dialog
        open={confirming !== null}
        onClose={() => {
          setConfirming(null);
        }}
      >
        <DialogTitle>{confirming === null ? '' : t(CONFIRM_TITLE[confirming])}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {confirming === 'abandon'
              ? t('interview.abandonBody')
              : confirming === 'skip'
                ? t('interview.skipBody')
                : allSolved
                  ? t('interview.finishBodySolved')
                  : t('interview.finishBody')}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setConfirming(null);
            }}
          >
            {t('interview.keepGoing')}
          </Button>
          <Button
            variant="contained"
            color={confirming === 'abandon' ? 'error' : 'primary'}
            onClick={confirm}
          >
            {confirming === null ? '' : t(CONFIRM_LABEL[confirming])}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
