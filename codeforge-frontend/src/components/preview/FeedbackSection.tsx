import CloseRounded from '@mui/icons-material/CloseRounded';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  LinearProgress,
  Paper,
  Skeleton,
  SnackbarContent,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Block, Row, Section } from './Section';

const severities = ['success', 'info', 'warning', 'error'] as const;

const messages: Record<(typeof severities)[number], [string, string]> = {
  success: ['Accepted', 'All 57 hidden test cases passed in 42 ms.'],
  info: ['Sample tests only', 'Run executes the 3 visible cases. Submit runs the full hidden suite.'],
  warning: ['Approaching the time limit', 'Your last submission used 1.8 s of a 2.0 s budget.'],
  error: ['Wrong answer on test 12', 'Expected [0,1] but your solution returned [1,0].'],
};

/**
 * A real Dialog behind a trigger rather than one pinned open: an always-mounted
 * Modal locks body scroll and marks the rest of the page `aria-hidden`.
 */
const DialogSpecimen = () => {
  const [open, setOpen] = useState(false);
  const close = () => {
    setOpen(false);
  };

  return (
    <>
      <Button
        variant="outlined"
        onClick={() => {
          setOpen(true);
        }}
      >
        Open dialog
      </Button>
      <Dialog open={open} onClose={close} slotProps={{ paper: { sx: { maxWidth: 420 } } }}>
        <DialogTitle>End interview early?</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ typography: 'body2' }}>
            You have 18 minutes left and one unsolved problem. Submitting now locks in your current score.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button variant="text" onClick={close}>
            Keep going
          </Button>
          <Button color="error" onClick={close}>
            End &amp; score
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export const FeedbackSection = () => (
  <Section
    id="feedback"
    eyebrow="Components"
    title="Feedback"
    description="Alerts carry a tinted fill and a solid left rule, so severity is readable in peripheral vision
      while you are looking at the editor. Progress bars are thin pills; nothing spins unless work is happening."
  >
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Alerts">
        <Stack spacing={1.5}>
          {severities.map((severity) => (
            <Alert key={severity} severity={severity}>
              <AlertTitle>{messages[severity][0]}</AlertTitle>
              {messages[severity][1]}
            </Alert>
          ))}
          <Alert
            severity="info"
            action={
              <IconButton size="small">
                <CloseRounded fontSize="small" />
              </IconButton>
            }
          >
            Dismissible, single-line — no title.
          </Alert>
          <Alert severity="success" variant="outlined">
            Outlined variant, for use inside already-tinted panels.
          </Alert>
          <Alert severity="error" variant="filled">
            Filled variant, reserved for blocking failures.
          </Alert>
        </Stack>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Progress & placeholders">
        <Stack spacing={2.5}>
          <Stack spacing={1}>
            <Typography variant="caption" sx={{ color: 'text.secondary' }}>
              Running hidden tests · 34 of 57
            </Typography>
            <LinearProgress variant="determinate" value={60} />
            <LinearProgress color="success" variant="determinate" value={88} />
            <LinearProgress />
          </Stack>
          <Row>
            <CircularProgress size={28} />
            <CircularProgress size={28} color="success" variant="determinate" value={72} />
            <CircularProgress size={28} color="warning" variant="determinate" value={38} />
            <Box sx={{ width: 16 }} />
            <Stack spacing={0.75} sx={{ width: 280 }}>
              <Skeleton variant="text" width="70%" />
              <Skeleton variant="text" width="90%" />
              <Skeleton variant="rounded" height={44} />
            </Stack>
            <Skeleton variant="circular" width={36} height={36} />
          </Row>
        </Stack>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Overlays" hint="tooltip pinned open for inspection">
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={3} sx={{ alignItems: 'flex-start' }}>
          <DialogSpecimen />
          <Stack spacing={2}>
            <SnackbarContent
              message="Solution saved to your submissions."
              action={
                <Button size="small" variant="text">
                  View
                </Button>
              }
            />
            <Row>
              <Tooltip title="Time complexity: O(n)" open arrow placement="right">
                <Button variant="outlined">Hovered tooltip</Button>
              </Tooltip>
            </Row>
          </Stack>
        </Stack>
      </Block>
    </Paper>
  </Section>
);
