import CloudUploadRounded from '@mui/icons-material/CloudUploadRounded';
import MemoryRounded from '@mui/icons-material/MemoryRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import RefreshRounded from '@mui/icons-material/RefreshRounded';
import SettingsRounded from '@mui/icons-material/SettingsRounded';
import TimerRounded from '@mui/icons-material/TimerRounded';
import {
  Box,
  Button,
  Chip,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tab,
  Tabs,
  Tooltip,
  Typography,
} from '@mui/material';
import { CodeBlock, type CodeToken } from './CodeBlock';
import { DifficultyChip, VerdictChip } from './StatusChips';

const solution: CodeToken[][] = [
  [
    ['class', 'keyword'],
    [' ', 'plain'],
    ['Solution', 'type'],
    [' {', 'plain'],
  ],
  [
    ['  public', 'keyword'],
    [' ', 'plain'],
    ['int', 'type'],
    ['[] ', 'plain'],
    ['twoSum', 'func'],
    ['(', 'plain'],
    ['int', 'type'],
    ['[] nums, ', 'plain'],
    ['int', 'type'],
    [' target) {', 'plain'],
  ],
  [
    ['    ', 'plain'],
    ['// value -> index seen so far', 'comment'],
  ],
  [
    ['    Map<Integer, Integer> seen = ', 'plain'],
    ['new', 'keyword'],
    [' ', 'plain'],
    ['HashMap', 'type'],
    ['<>();', 'plain'],
  ],
  [
    ['    ', 'plain'],
    ['for', 'keyword'],
    [' (', 'plain'],
    ['int', 'type'],
    [' i = ', 'plain'],
    ['0', 'number'],
    ['; i ', 'plain'],
    ['<', 'operator'],
    [' nums.length; i', 'plain'],
    ['++', 'operator'],
    [') {', 'plain'],
  ],
  [
    ['      ', 'plain'],
    ['int', 'type'],
    [' need = target ', 'plain'],
    ['-', 'operator'],
    [' nums[i];', 'plain'],
  ],
  [
    ['      ', 'plain'],
    ['if', 'keyword'],
    [' (seen.', 'plain'],
    ['containsKey', 'func'],
    ['(need)) {', 'plain'],
  ],
  [
    ['        ', 'plain'],
    ['return', 'keyword'],
    [' ', 'plain'],
    ['new', 'keyword'],
    [' ', 'plain'],
    ['int', 'type'],
    ['[] { seen.', 'plain'],
    ['get', 'func'],
    ['(need), i };', 'plain'],
  ],
  [['      }', 'plain']],
  [
    ['      seen.', 'plain'],
    ['put', 'func'],
    ['(nums[i], i);', 'plain'],
  ],
  [['    }', 'plain']],
  [
    ['    ', 'plain'],
    ['throw', 'keyword'],
    [' ', 'plain'],
    ['new', 'keyword'],
    [' ', 'plain'],
    ['IllegalArgumentException', 'type'],
    ['(', 'plain'],
    ['"no solution"', 'string'],
    [');', 'plain'],
  ],
  [['  }', 'plain']],
  [['}', 'plain']],
];

const EditorToolbar = () => (
  <Stack
    direction="row"
    spacing={1}
    sx={{ alignItems: 'center', px: 1.5, py: 1, borderBottom: 1, borderColor: 'border.subtle' }}
  >
    <Select value="java" size="small" sx={{ width: 132 }}>
      <MenuItem value="java">Java</MenuItem>
      <MenuItem value="python">Python</MenuItem>
      <MenuItem value="javascript">JavaScript</MenuItem>
      <MenuItem value="typescript">TypeScript</MenuItem>
    </Select>
    <Box sx={{ flex: 1 }} />
    <Tooltip title="Reset to starter code">
      <IconButton size="small">
        <RefreshRounded fontSize="small" />
      </IconButton>
    </Tooltip>
    <Tooltip title="Editor settings">
      <IconButton size="small">
        <SettingsRounded fontSize="small" />
      </IconButton>
    </Tooltip>
    <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
    <Button size="small" variant="soft" startIcon={<PlayArrowRounded />}>
      Run
    </Button>
    <Button size="small" color="success" startIcon={<CloudUploadRounded />}>
      Submit
    </Button>
  </Stack>
);

const ConsolePanel = () => (
  <Box sx={{ borderTop: 1, borderColor: 'border.subtle' }}>
    <Tabs value={0} sx={{ px: 1, borderBottom: 1, borderColor: 'border.subtle', minHeight: 36 }}>
      <Tab label="Testcase" sx={{ minHeight: 36 }} />
      <Tab label="Result" sx={{ minHeight: 36 }} />
    </Tabs>
    <Stack spacing={1.5} sx={{ p: 1.75 }}>
      <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <VerdictChip verdict="accepted" />
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', color: 'text.secondary' }}>
          <TimerRounded sx={{ fontSize: 15 }} />
          <Typography variant="mono">42 ms</Typography>
        </Stack>
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', color: 'text.secondary' }}>
          <MemoryRounded sx={{ fontSize: 15 }} />
          <Typography variant="mono">17.4 MB</Typography>
        </Stack>
        <Typography variant="mono" sx={{ color: 'verdict.accepted' }}>
          beats 94.21%
        </Typography>
      </Stack>
      <Paper variant="sunken" sx={{ p: 1.5 }}>
        <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 0.5 }}>
          Input
        </Typography>
        <Typography variant="code" sx={{ display: 'block' }}>
          nums = [2,7,11,15], target = 9
        </Typography>
        <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mt: 1.5, mb: 0.5 }}>
          Output
        </Typography>
        <Typography variant="code" sx={{ display: 'block', color: 'verdict.accepted' }}>
          [0,1]
        </Typography>
      </Paper>
    </Stack>
  </Box>
);

const codeSx = {
  typography: 'code',
  px: 0.5,
  py: 0.15,
  borderRadius: 0.75,
  backgroundColor: 'surface.sunken',
  border: 1,
  borderColor: 'border.subtle',
  color: 'text.primary',
};

const ProblemPanel = () => (
  <Stack sx={{ height: '100%' }}>
    <Tabs value={0} sx={{ px: 1, borderBottom: 1, borderColor: 'border.subtle' }}>
      <Tab label="Description" />
      <Tab label="Editorial" />
      <Tab label="Submissions" />
    </Tabs>
    <Stack spacing={2} sx={{ p: 2.5, overflow: 'auto' }}>
      <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <Typography variant="h3">1. Two Sum</Typography>
        <DifficultyChip level="easy" />
      </Stack>
      <Typography variant="body1" sx={{ color: 'text.secondary' }}>
        Given an array of integers{' '}
        <Box component="code" sx={codeSx}>
          nums
        </Box>{' '}
        and an integer{' '}
        <Box component="code" sx={codeSx}>
          target
        </Box>
        , return the indices of the two numbers such that they add up to{' '}
        <Box component="code" sx={codeSx}>
          target
        </Box>
        .
      </Typography>
      <Paper variant="sunken" sx={{ p: 1.5 }}>
        <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 0.75 }}>
          Example 1
        </Typography>
        <Typography variant="code" sx={{ display: 'block', color: 'text.secondary' }}>
          Input: nums = [2,7,11,15], target = 9
          <br />
          Output: [0,1]
          <br />
          Explanation: nums[0] + nums[1] == 9
        </Typography>
      </Paper>
      <Box>
        <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 1 }}>
          Topics
        </Typography>
        <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
          <Chip label="Arrays" variant="outlined" />
          <Chip label="Hash Table" variant="outlined" />
        </Stack>
      </Box>
    </Stack>
  </Stack>
);

/** The two-pane solving surface — the screen this product lives or dies on. */
export const Workspace = () => (
  <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
    <Stack direction={{ xs: 'column', lg: 'row' }} divider={<Divider orientation="vertical" flexItem />}>
      <Box sx={{ flex: '1 1 44%', minWidth: 0 }}>
        <ProblemPanel />
      </Box>
      <Stack sx={{ flex: '1 1 56%', minWidth: 0 }}>
        <EditorToolbar />
        <CodeBlock lines={solution} highlight={[7]} />
        <ConsolePanel />
      </Stack>
    </Stack>
  </Paper>
);
