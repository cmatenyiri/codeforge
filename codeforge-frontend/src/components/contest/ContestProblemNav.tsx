import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import LockRounded from '@mui/icons-material/LockRounded';
import { Box, Stack, Tab, Tabs, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ContestProblemSummary } from '../../api/types';

/**
 * The question strip: Q1 … Q4, with what each is worth and whether it is done.
 *
 * <p>Unlike the mock interview's, nothing here is locked once the contest
 * starts. A contest is not a sequence — deciding which of four problems to spend
 * the last twenty minutes on is most of the skill it measures, so all of them
 * are always one click away.
 */
export const ContestProblemNav = ({
  problems,
  active,
  onSelect,
  started,
}: {
  problems: ContestProblemSummary[];
  active: number;
  onSelect: (position: number) => void;
  started: boolean;
}) => {
  const { t } = useTranslation();

  return (
    <Tabs
      value={active}
      onChange={(_, value: number) => {
        onSelect(value);
      }}
      variant="scrollable"
      scrollButtons="auto"
      sx={{ borderBottom: 1, borderColor: 'border.default', px: 1 }}
    >
      {problems.map((problem) => (
        <Tab
          key={problem.position}
          value={problem.position}
          disabled={!started}
          label={
            <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center' }}>
              {problem.solved ? (
                <CheckCircleRounded sx={{ fontSize: 15, color: 'verdict.accepted' }} />
              ) : started ? null : (
                <LockRounded sx={{ fontSize: 14, color: 'text.disabled' }} />
              )}
              <Box component="span">{problem.label}</Box>
              <Tooltip title={t('contest.points', { count: problem.points })}>
                <Typography component="span" variant="caption" sx={{ color: 'text.disabled' }}>
                  {problem.points}
                </Typography>
              </Tooltip>
            </Stack>
          }
        />
      ))}
    </Tabs>
  );
};
