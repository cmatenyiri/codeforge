import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import {
  Box,
  Chip,
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { type ProblemSummary } from '../../api/types';
import { problemPath } from '../../routes/paths';
import { DifficultyChip } from './DifficultyChip';

type ProblemsTableProps = { problems: ProblemSummary[]; loading: boolean };

const SkeletonRows = () => (
  <>
    {Array.from({ length: 5 }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={5}>
          <Skeleton height={26} />
        </TableCell>
      </TableRow>
    ))}
  </>
);

const EmptyState = () => {
  const { t } = useTranslation();
  return (
    <TableRow>
      <TableCell colSpan={5}>
        <Stack spacing={0.5} sx={{ alignItems: 'center', py: 6 }}>
          <Typography variant="subtitle1">{t('problems.emptyTitle')}</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('problems.emptyBody')}
          </Typography>
        </Stack>
      </TableCell>
    </TableRow>
  );
};

export const ProblemsTable = ({ problems, loading }: ProblemsTableProps) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell width={44} aria-label={t('problems.status')} />
            <TableCell>{t('problems.title')}</TableCell>
            <TableCell>{t('problems.topics')}</TableCell>
            <TableCell width={120}>{t('problems.difficulty')}</TableCell>
            <TableCell width={52} />
          </TableRow>
        </TableHead>
        <TableBody>
          {loading ? (
            <SkeletonRows />
          ) : problems.length === 0 ? (
            <EmptyState />
          ) : (
            problems.map((problem) => (
              <TableRow
                key={problem.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => {
                  void navigate(problemPath(problem.slug));
                }}
              >
                <TableCell>
                  {problem.solved ? (
                    <Tooltip title={t('problems.solved')}>
                      <CheckCircleRounded sx={{ fontSize: 17, color: 'verdict.accepted', display: 'block' }} />
                    </Tooltip>
                  ) : null}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 550 }}>
                    {problem.title}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {problem.tags.map((tag) => (
                      <Chip key={tag.slug} label={tag.name} size="small" variant="outlined" />
                    ))}
                  </Stack>
                </TableCell>
                <TableCell>
                  <DifficultyChip difficulty={problem.difficulty} />
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', color: 'text.disabled' }}>
                    <ChevronRightRounded fontSize="small" />
                  </Box>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
