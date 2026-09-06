import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import PendingRounded from '@mui/icons-material/PendingRounded';
import {
  Chip,
  Link as MuiLink,
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  Tooltip,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type ProblemSort, type ProblemSummary, type SortOrder } from '../../api/types';
import { problemPath } from '../../routes/paths';
import { DifficultyChip } from './DifficultyChip';

type ProblemsTableProps = {
  problems: ProblemSummary[];
  loading: boolean;
  rows: number;
  sort: ProblemSort;
  order: SortOrder;
  onSort: (sort: ProblemSort) => void;
};

/** The columns a user can order by, and how wide each one sits. */
const SORTABLE = [
  { key: 'id', labelKey: 'problems.number', width: 68, align: 'left' },
  { key: 'title', labelKey: 'problems.problem', width: undefined, align: 'left' },
  { key: 'acceptance', labelKey: 'problems.acceptance', width: 118, align: 'right' },
  { key: 'difficulty', labelKey: 'problems.difficulty', width: 122, align: 'left' },
] as const satisfies readonly { key: ProblemSort; labelKey: string; width?: number; align: 'left' | 'right' }[];

/** As many placeholder rows as the page will hold, so the layout does not jump. */
const SkeletonRows = ({ rows }: { rows: number }) => (
  <>
    {Array.from({ length: rows }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={6}>
          <Skeleton height={24} />
        </TableCell>
      </TableRow>
    ))}
  </>
);

const EmptyState = () => {
  const { t } = useTranslation();

  return (
    <TableRow sx={{ '&:hover': { backgroundColor: 'transparent' } }}>
      <TableCell colSpan={6}>
        <Stack spacing={0.5} sx={{ alignItems: 'center', py: 7 }}>
          <Typography variant="subtitle1">{t('problems.emptyTitle')}</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('problems.emptyBody')}
          </Typography>
        </Stack>
      </TableCell>
    </TableRow>
  );
};

/** Solved beats attempted; neither is drawn for a problem never opened. */
const StatusIcon = ({ problem }: { problem: ProblemSummary }) => {
  const { t } = useTranslation();

  if (problem.solved) {
    return (
      <Tooltip title={t('problems.solved')}>
        <CheckCircleRounded sx={{ fontSize: 17, color: 'verdict.accepted', display: 'block' }} />
      </Tooltip>
    );
  }
  if (problem.attempted) {
    return (
      <Tooltip title={t('problems.attempted')}>
        <PendingRounded sx={{ fontSize: 17, color: 'verdict.timeLimit', display: 'block' }} />
      </Tooltip>
    );
  }
  return null;
};

/**
 * The catalogue itself.
 *
 * <p>Each title is a real link rather than a row click handler: middle-click,
 * ⌘-click and "copy link address" all work, and a keyboard reaches it in tab
 * order without the row having to fake being a button.
 */
export const ProblemsTable = ({ problems, loading, rows, sort, order, onSort }: ProblemsTableProps) => {
  const { t } = useTranslation();

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell width={44} aria-label={t('problems.status')} />
            {SORTABLE.map((column) => (
              <TableCell key={column.key} width={column.width} align={column.align}>
                <TableSortLabel
                  active={sort === column.key}
                  direction={sort === column.key ? order : 'asc'}
                  onClick={() => {
                    onSort(column.key);
                  }}
                >
                  {t(column.labelKey)}
                </TableSortLabel>
              </TableCell>
            ))}
            <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>{t('problems.topics')}</TableCell>
          </TableRow>
        </TableHead>

        <TableBody>
          {loading ? (
            <SkeletonRows rows={rows} />
          ) : problems.length === 0 ? (
            <EmptyState />
          ) : (
            problems.map((problem) => (
              <TableRow key={problem.id} hover>
                <TableCell>
                  <StatusIcon problem={problem} />
                </TableCell>

                <TableCell>
                  <Typography variant="mono" sx={{ color: 'text.disabled' }}>
                    {problem.id}
                  </Typography>
                </TableCell>

                <TableCell>
                  <MuiLink
                    component={Link}
                    to={problemPath(problem.slug)}
                    underline="hover"
                    sx={{ typography: 'body2', fontWeight: 550, color: 'text.primary' }}
                  >
                    {problem.title}
                  </MuiLink>
                </TableCell>

                <TableCell align="right">
                  <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                    {problem.acceptanceRate === undefined
                      ? '—'
                      : `${(problem.acceptanceRate * 100).toFixed(1)}%`}
                  </Typography>
                </TableCell>

                <TableCell>
                  <DifficultyChip difficulty={problem.difficulty} />
                </TableCell>

                <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>
                  <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {problem.tags.map((tag) => (
                      <Chip key={tag.slug} label={tag.name} size="small" variant="outlined" />
                    ))}
                  </Stack>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
