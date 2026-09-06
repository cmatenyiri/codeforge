import ArchiveRounded from '@mui/icons-material/ArchiveRounded';
import ContentCopyRounded from '@mui/icons-material/ContentCopyRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import EditRounded from '@mui/icons-material/EditRounded';
import MoreVertRounded from '@mui/icons-material/MoreVertRounded';
import OpenInNewRounded from '@mui/icons-material/OpenInNewRounded';
import PublishRounded from '@mui/icons-material/PublishRounded';
import UnarchiveRounded from '@mui/icons-material/UnarchiveRounded';
import UnpublishedRounded from '@mui/icons-material/UnpublishedRounded';
import WarningAmberRounded from '@mui/icons-material/WarningAmberRounded';
import {
  Chip,
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type AdminProblemSort } from '../../api/admin-api';
import { type AdminProblemSummary, type SortOrder } from '../../api/types';
import { adminProblemEditPath, problemPath } from '../../routes/paths';
import { DifficultyChip } from '../problems/DifficultyChip';
import { ProblemStateChip } from './ProblemStateChip';

/** Columns the authoring catalogue can be ordered by. */
const SORTABLE = [
  { key: 'id', labelKey: 'problems.number', width: 68 },
  { key: 'title', labelKey: 'problems.problem', width: undefined },
  { key: 'difficulty', labelKey: 'problems.difficulty', width: 122 },
] as const satisfies readonly { key: AdminProblemSort; labelKey: string; width?: number }[];

const COLUMN_COUNT = 8;

export type AdminProblemAction = 'edit' | 'duplicate' | 'publish' | 'unpublish' | 'archive' | 'restore' | 'delete';

type AdminProblemsTableProps = {
  problems: AdminProblemSummary[];
  loading: boolean;
  rows: number;
  sort: AdminProblemSort;
  order: SortOrder;
  onSort: (sort: AdminProblemSort) => void;
  onAction: (action: AdminProblemAction, problem: AdminProblemSummary) => void;
  /** The row a request is in flight for, so its menu can be disabled. */
  busyId: number | null;
};

const SkeletonRows = ({ rows }: { rows: number }) => (
  <>
    {Array.from({ length: rows }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={COLUMN_COUNT}>
          <Skeleton height={24} />
        </TableCell>
      </TableRow>
    ))}
  </>
);

/** The row menu. Which entries appear depends on where the problem stands. */
const RowMenu = ({
  problem,
  disabled,
  onAction,
}: {
  problem: AdminProblemSummary;
  disabled: boolean;
  onAction: (action: AdminProblemAction, problem: AdminProblemSummary) => void;
}) => {
  const { t } = useTranslation();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const close = () => {
    setAnchorEl(null);
  };

  const choose = (action: AdminProblemAction) => () => {
    close();
    onAction(action, problem);
  };

  return (
    <>
      <IconButton
        size="small"
        disabled={disabled}
        aria-label={t('admin.list.actions')}
        aria-haspopup="menu"
        onClick={(event) => {
          setAnchorEl(event.currentTarget);
        }}
      >
        <MoreVertRounded fontSize="small" />
      </IconButton>

      <Menu anchorEl={anchorEl} open={anchorEl !== null} onClose={close} slotProps={{ paper: { sx: { minWidth: 214 } } }}>
        <MenuItem onClick={choose('edit')}>
          <ListItemIcon>
            <EditRounded fontSize="small" />
          </ListItemIcon>
          <ListItemText>{t('admin.list.edit')}</ListItemText>
        </MenuItem>

        <MenuItem component={Link} to={problemPath(problem.slug)} onClick={close}>
          <ListItemIcon>
            <OpenInNewRounded fontSize="small" />
          </ListItemIcon>
          <ListItemText>{t('admin.list.preview')}</ListItemText>
        </MenuItem>

        <MenuItem onClick={choose('duplicate')}>
          <ListItemIcon>
            <ContentCopyRounded fontSize="small" />
          </ListItemIcon>
          <ListItemText>{t('admin.list.duplicate')}</ListItemText>
        </MenuItem>

        {problem.state === 'PUBLISHED' ? (
          <MenuItem onClick={choose('unpublish')}>
            <ListItemIcon>
              <UnpublishedRounded fontSize="small" />
            </ListItemIcon>
            <ListItemText>{t('admin.list.unpublish')}</ListItemText>
          </MenuItem>
        ) : (
          <MenuItem onClick={choose('publish')} disabled={problem.state === 'ARCHIVED'}>
            <ListItemIcon>
              <PublishRounded fontSize="small" />
            </ListItemIcon>
            <ListItemText>{t('admin.list.publish')}</ListItemText>
          </MenuItem>
        )}

        {problem.state === 'ARCHIVED' ? (
          <MenuItem onClick={choose('restore')}>
            <ListItemIcon>
              <UnarchiveRounded fontSize="small" />
            </ListItemIcon>
            <ListItemText>{t('admin.list.restore')}</ListItemText>
          </MenuItem>
        ) : (
          <MenuItem onClick={choose('archive')}>
            <ListItemIcon>
              <ArchiveRounded fontSize="small" />
            </ListItemIcon>
            <ListItemText>{t('admin.list.archive')}</ListItemText>
          </MenuItem>
        )}

        <MenuItem onClick={choose('delete')} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <DeleteOutlineRounded fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>{t('admin.list.delete')}</ListItemText>
        </MenuItem>
      </Menu>
    </>
  );
};

/**
 * The authoring catalogue.
 *
 * <p>Shows what an author decides by rather than what a solver does: where each
 * problem stands, whether it is complete enough to be solved at all, and how much
 * history it has — which is what makes an edit to its test cases consequential.
 */
export const AdminProblemsTable = ({
  problems,
  loading,
  rows,
  sort,
  order,
  onSort,
  onAction,
  busyId,
}: AdminProblemsTableProps) => {
  const { t, i18n } = useTranslation();
  const dates = new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' });

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            {SORTABLE.map((column) => (
              <TableCell key={column.key} width={column.width}>
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
            <TableCell width={120}>{t('admin.list.state')}</TableCell>
            <TableCell width={168}>{t('admin.list.cases')}</TableCell>
            <TableCell width={110} align="right">
              {t('admin.list.submissions')}
            </TableCell>
            <TableCell width={130}>
              <TableSortLabel
                active={sort === 'updated'}
                direction={sort === 'updated' ? order : 'desc'}
                onClick={() => {
                  onSort('updated');
                }}
              >
                {t('admin.list.updated')}
              </TableSortLabel>
            </TableCell>
            <TableCell width={56} aria-label={t('admin.list.actions')} />
          </TableRow>
        </TableHead>

        <TableBody>
          {loading ? (
            <SkeletonRows rows={rows} />
          ) : problems.length === 0 ? (
            <TableRow sx={{ '&:hover': { backgroundColor: 'transparent' } }}>
              <TableCell colSpan={COLUMN_COUNT}>
                <Stack spacing={0.5} sx={{ alignItems: 'center', py: 7 }}>
                  <Typography variant="subtitle1">{t('admin.list.emptyTitle')}</Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    {t('admin.list.emptyBody')}
                  </Typography>
                </Stack>
              </TableCell>
            </TableRow>
          ) : (
            problems.map((problem) => (
              <TableRow key={problem.id}>
                <TableCell sx={{ typography: 'mono', color: 'text.disabled' }}>{problem.id}</TableCell>

                <TableCell>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <MuiLink component={Link} to={adminProblemEditPath(problem.id)} sx={{ fontWeight: 600 }}>
                      {problem.title}
                    </MuiLink>
                    {problem.solvable ? null : (
                      <Tooltip title={t('admin.list.notSolvable')}>
                        <WarningAmberRounded sx={{ fontSize: 16, color: 'warning.main', display: 'block' }} />
                      </Tooltip>
                    )}
                  </Stack>
                  <Stack direction="row" spacing={0.75} sx={{ mt: 0.5, flexWrap: 'wrap' }} useFlexGap>
                    <Typography variant="caption" sx={{ typography: 'mono', color: 'text.disabled' }}>
                      /{problem.slug}
                    </Typography>
                    {problem.tags.map((tag) => (
                      <Chip key={tag.id} size="small" label={tag.name} />
                    ))}
                  </Stack>
                </TableCell>

                <TableCell>
                  <DifficultyChip difficulty={problem.difficulty} size="small" />
                </TableCell>

                <TableCell>
                  <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
                    <ProblemStateChip state={problem.state} size="small" />
                    {problem.hasEditorial ? (
                      <Tooltip title={t('admin.list.hasEditorial')}>
                        <Chip size="small" label={t('admin.list.editorialShort')} />
                      </Tooltip>
                    ) : null}
                  </Stack>
                </TableCell>

                <TableCell sx={{ typography: 'mono', color: 'text.secondary', whiteSpace: 'nowrap' }}>
                  {t('admin.list.caseCounts', {
                    samples: problem.sampleTestCaseCount,
                    hidden: problem.testCaseCount - problem.sampleTestCaseCount,
                  })}
                </TableCell>

                <TableCell align="right" sx={{ typography: 'mono', color: 'text.secondary' }}>
                  {problem.totalSubmissions}
                </TableCell>

                <TableCell sx={{ color: 'text.disabled' }}>{dates.format(new Date(problem.updatedAt))}</TableCell>

                <TableCell align="right">
                  <RowMenu problem={problem} disabled={busyId === problem.id} onAction={onAction} />
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
