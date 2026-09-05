import AccountTreeRounded from '@mui/icons-material/AccountTreeRounded';
import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import LocalFireDepartmentRounded from '@mui/icons-material/LocalFireDepartmentRounded';
import NotificationsRounded from '@mui/icons-material/NotificationsRounded';
import TagRounded from '@mui/icons-material/TagRounded';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Avatar,
  AvatarGroup,
  Badge,
  Chip,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
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
import { DifficultyChip, VerdictChip } from './StatusChips';
import { Block, Row, Section } from './Section';

/** The preview page is deliberately inert; interactive slots get an explicit no-op. */
const noop = () => {
  // no behaviour on the theme preview
};

type ProblemRow = {
  id: number;
  title: string;
  difficulty: 'easy' | 'medium' | 'hard';
  acceptance: string;
  tags: string[];
  solved: boolean;
};

const problems: ProblemRow[] = [
  { id: 1, title: 'Two Sum', difficulty: 'easy', acceptance: '52.4%', tags: ['Arrays', 'Hash Table'], solved: true },
  {
    id: 42,
    title: 'Trapping Rain Water',
    difficulty: 'hard',
    acceptance: '61.8%',
    tags: ['Two Pointers'],
    solved: false,
  },
  {
    id: 207,
    title: 'Course Schedule',
    difficulty: 'medium',
    acceptance: '47.1%',
    tags: ['Graphs', 'DFS'],
    solved: true,
  },
  {
    id: 300,
    title: 'Longest Increasing Subsequence',
    difficulty: 'medium',
    acceptance: '54.9%',
    tags: ['DP'],
    solved: false,
  },
  {
    id: 124,
    title: 'Binary Tree Maximum Path Sum',
    difficulty: 'hard',
    acceptance: '40.3%',
    tags: ['Trees'],
    solved: false,
  },
];

const ProblemsTable = () => (
  <TableContainer component={Paper} variant="outlined">
    <Table>
      <TableHead>
        <TableRow>
          <TableCell width={44} />
          <TableCell width={64}>#</TableCell>
          <TableCell>Title</TableCell>
          <TableCell>Tags</TableCell>
          <TableCell width={120}>Difficulty</TableCell>
          <TableCell width={110} align="right">
            Acceptance
          </TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {problems.map((problem) => (
          <TableRow key={problem.id} hover>
            <TableCell>
              {problem.solved ? (
                <CheckCircleRounded sx={{ fontSize: 17, color: 'verdict.accepted', display: 'block' }} />
              ) : null}
            </TableCell>
            <TableCell>
              <Typography variant="mono" sx={{ color: 'text.disabled' }}>
                {problem.id}
              </Typography>
            </TableCell>
            <TableCell>
              <Typography variant="body2" sx={{ fontWeight: 550 }}>
                {problem.title}
              </Typography>
            </TableCell>
            <TableCell>
              <Stack direction="row" spacing={0.75}>
                {problem.tags.map((tag) => (
                  <Chip key={tag} label={tag} size="small" variant="outlined" />
                ))}
              </Stack>
            </TableCell>
            <TableCell>
              <DifficultyChip level={problem.difficulty} />
            </TableCell>
            <TableCell align="right">
              <Typography variant="mono">{problem.acceptance}</Typography>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </TableContainer>
);

export const DataDisplaySection = () => (
  <Section
    id="data"
    eyebrow="Components"
    title="Data display"
    description="Tables are the backbone of a problem set, so they run dense with monospaced numerals and a
      monospaced small-caps header. Chips are square-ish and monospaced — they read as metadata, not decoration."
  >
    <Block label="Problem table">
      <ProblemsTable />
    </Block>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Chips">
        <Row>
          <DifficultyChip level="easy" />
          <DifficultyChip level="medium" />
          <DifficultyChip level="hard" />
        </Row>
        <Row>
          <VerdictChip verdict="accepted" />
          <VerdictChip verdict="wrongAnswer" />
          <VerdictChip verdict="timeLimit" />
          <VerdictChip verdict="runtimeError" />
          <VerdictChip verdict="compileError" />
          <VerdictChip verdict="pending" />
        </Row>
        <Row>
          <Chip label="Arrays" variant="outlined" icon={<TagRounded />} />
          <Chip label="Dynamic Programming" variant="outlined" />
          <Chip label="Graphs" variant="outlined" icon={<AccountTreeRounded />} />
          <Chip label="Premium" color="primary" />
          <Chip label="Removable" onDelete={noop} />
          <Chip label="Small" size="small" />
        </Row>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Avatars, badges & lists">
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
          <Stack spacing={2}>
            <Row>
              <Avatar>CS</Avatar>
              <Avatar sx={{ bgcolor: 'primary.main', color: 'primary.contrastText' }}>AK</Avatar>
              <Avatar sx={{ bgcolor: 'brand.ember', color: 'common.white' }}>
                <LocalFireDepartmentRounded fontSize="small" />
              </Avatar>
              <AvatarGroup max={4}>
                <Avatar>A</Avatar>
                <Avatar>B</Avatar>
                <Avatar>C</Avatar>
                <Avatar>D</Avatar>
                <Avatar>E</Avatar>
              </AvatarGroup>
            </Row>
            <Row>
              <Badge badgeContent={12} color="primary">
                <IconButton>
                  <NotificationsRounded fontSize="small" />
                </IconButton>
              </Badge>
              <Badge variant="dot" color="success">
                <Avatar sx={{ width: 30, height: 30 }}>ON</Avatar>
              </Badge>
              <Badge badgeContent="99+" color="error">
                <Chip label="Failed" variant="outlined" />
              </Badge>
            </Row>
          </Stack>
          <Paper variant="outlined" sx={{ minWidth: 240 }}>
            <List>
              <ListSubheader>Study plan</ListSubheader>
              <ListItemButton selected>
                <ListItemIcon>
                  <CheckCircleRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary="Arrays & Hashing" secondary="9 of 9 solved" />
              </ListItemButton>
              <ListItemButton>
                <ListItemIcon>
                  <AccountTreeRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary="Graphs" secondary="3 of 11 solved" />
              </ListItemButton>
              <ListItemButton>
                <ListItemIcon>
                  <TagRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary="Dynamic Programming" secondary="0 of 14 solved" />
              </ListItemButton>
            </List>
          </Paper>
        </Stack>
      </Block>
    </Paper>

    <Block label="Accordion">
      <div>
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreRounded fontSize="small" />}>Hint 1</AccordionSummary>
          <AccordionDetails>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              A brute force approach is O(n²). Can you trade memory for time?
            </Typography>
          </AccordionDetails>
        </Accordion>
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreRounded fontSize="small" />}>Hint 2</AccordionSummary>
          <AccordionDetails>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              Store each value you have seen in a hash map keyed by its complement.
            </Typography>
          </AccordionDetails>
        </Accordion>
      </div>
    </Block>
  </Section>
);
