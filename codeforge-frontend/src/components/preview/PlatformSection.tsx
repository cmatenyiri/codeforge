import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import SkipNextRounded from '@mui/icons-material/SkipNextRounded';
import TimerRounded from '@mui/icons-material/TimerRounded';
import {
  Box,
  Button,
  LinearProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { Block, Section } from './Section';
import { StatTiles } from './StatTiles';
import { DifficultyChip, VerdictChip, type Verdict } from './StatusChips';
import { Workspace } from './Workspace';

type Submission = {
  when: string;
  problem: string;
  verdict: Verdict;
  language: string;
  runtime: string;
  memory: string;
};

const submissions: Submission[] = [
  { when: '2 min ago', problem: 'Two Sum', verdict: 'accepted', language: 'Java', runtime: '42 ms', memory: '17.4 MB' },
  { when: '9 min ago', problem: 'Two Sum', verdict: 'wrongAnswer', language: 'Java', runtime: '—', memory: '—' },
  {
    when: '1 h ago',
    problem: 'Course Schedule',
    verdict: 'timeLimit',
    language: 'Python',
    runtime: '2000 ms',
    memory: '—',
  },
  { when: '3 h ago', problem: 'Word Ladder', verdict: 'runtimeError', language: 'Python', runtime: '—', memory: '—' },
  {
    when: 'Yesterday',
    problem: 'LRU Cache',
    verdict: 'compileError',
    language: 'TypeScript',
    runtime: '—',
    memory: '—',
  },
];

const InterviewBar = () => (
  <Paper variant="outlined" sx={{ p: 2 }}>
    <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: { md: 'center' } }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', color: 'warning.main' }}>
        <TimerRounded fontSize="small" />
        <Typography variant="metric" sx={{ fontSize: '1.5rem' }}>
          18:42
        </Typography>
      </Stack>
      <Box sx={{ flex: 1, minWidth: 200 }}>
        <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.75 }}>
          <Typography variant="overline" sx={{ color: 'text.disabled' }}>
            Mock interview · problem 2 of 3
          </Typography>
          <Typography variant="mono" sx={{ color: 'text.secondary' }}>
            21 / 40 min
          </Typography>
        </Stack>
        <LinearProgress variant="determinate" value={53} color="warning" />
      </Box>
      <Stack direction="row" spacing={1}>
        <DifficultyChip level="medium" />
        <Button size="small" variant="outlined" startIcon={<SkipNextRounded />}>
          Skip
        </Button>
        <Button size="small" startIcon={<PlayArrowRounded />}>
          Resume
        </Button>
      </Stack>
    </Stack>
  </Paper>
);

const SubmissionHistory = () => (
  <TableContainer component={Paper} variant="outlined">
    <Table>
      <TableHead>
        <TableRow>
          <TableCell width={120}>When</TableCell>
          <TableCell>Problem</TableCell>
          <TableCell width={190}>Status</TableCell>
          <TableCell width={110}>Language</TableCell>
          <TableCell width={100} align="right">
            Runtime
          </TableCell>
          <TableCell width={100} align="right">
            Memory
          </TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {submissions.map((row) => (
          <TableRow key={`${row.when}-${row.problem}`} hover>
            <TableCell>
              <Typography variant="mono" sx={{ color: 'text.disabled' }}>
                {row.when}
              </Typography>
            </TableCell>
            <TableCell>
              <Typography variant="body2" sx={{ fontWeight: 550 }}>
                {row.problem}
              </Typography>
            </TableCell>
            <TableCell>
              <VerdictChip verdict={row.verdict} />
            </TableCell>
            <TableCell>
              <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                {row.language}
              </Typography>
            </TableCell>
            <TableCell align="right">
              <Typography variant="mono">{row.runtime}</Typography>
            </TableCell>
            <TableCell align="right">
              <Typography variant="mono">{row.memory}</Typography>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </TableContainer>
);

export const PlatformSection = () => (
  <Section
    id="platform"
    eyebrow="In context"
    title="Product surfaces"
    description="The same tokens assembled into the screens CodeForge will actually ship: the solving workspace,
      a dashboard, the interview timer and submission history. Nothing here is wired up — it exists to prove the
      design system holds together at real density."
  >
    <Block label="Solving workspace" hint="problem left, editor right, console below">
      <Workspace />
    </Block>
    <Block label="Interview timer">
      <InterviewBar />
    </Block>
    <Block label="Dashboard tiles">
      <StatTiles />
    </Block>
    <Block label="Submission history">
      <SubmissionHistory />
    </Block>
  </Section>
);
