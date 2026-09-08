import LockRounded from '@mui/icons-material/LockRounded';
import { Box, Chip, Paper, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ContestProblemDetail } from '../../api/types';
import { DifficultyChip } from '../problems/DifficultyChip';
import { MarkdownBody } from '../solve/MarkdownBody';

/**
 * The left-hand half of the contest workspace.
 *
 * <p>Compared with the practice page: no topic tags, no editorial tab, no hints,
 * and nothing about whether this problem has been solved before. None of that is
 * hidden by this component — the server does not send it, because in a contest
 * each of those is a shortcut past the thing being measured.
 */
export const ContestProblemPanel = ({ problem }: { problem: ContestProblemDetail }) => {
  const { t } = useTranslation();

  return (
    <Box sx={{ height: '100%', overflowY: 'auto' }}>
      <Stack spacing={3} sx={{ p: 3 }}>
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="overline" sx={{ color: 'text.disabled' }}>
            {problem.label}
          </Typography>
          <Typography variant="h3">{problem.title}</Typography>
          <DifficultyChip difficulty={problem.difficulty} />
          <Chip label={t('contest.points', { count: problem.points })} variant="outlined" />
        </Stack>

        <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
          {problem.attempts > 0 ? (
            <Typography variant="body2" sx={{ color: 'text.disabled' }}>
              {t('contest.attempts', { count: problem.attempts })}
              {problem.wrongAttempts > 0 && !problem.solved
                ? ` · ${t('contest.wrongAttempts', { count: problem.wrongAttempts })}`
                : ''}
            </Typography>
          ) : null}

          {problem.hiddenTestCaseCount > 0 ? (
            <Tooltip title={t('solve.hiddenCasesHint')}>
              <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', color: 'text.disabled' }}>
                <LockRounded sx={{ fontSize: 14 }} />
                <Typography variant="body2">
                  {t('solve.hiddenCases', { count: problem.hiddenTestCaseCount })}
                </Typography>
              </Stack>
            </Tooltip>
          ) : null}
        </Stack>

        <MarkdownBody>{problem.description}</MarkdownBody>

        {problem.examples.length > 0 ? (
          <Stack spacing={1.5}>
            {problem.examples.map((example, index) => (
              <Paper key={index} variant="sunken" sx={{ p: 1.75 }}>
                <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 1 }}>
                  {t('solve.example', { index: index + 1 })}
                </Typography>
                <Stack spacing={0.75}>
                  <Typography variant="code" sx={{ display: 'block', whiteSpace: 'pre-wrap' }}>
                    <Box component="span" sx={{ color: 'text.disabled' }}>
                      {t('solve.input')}:{' '}
                    </Box>
                    {example.input}
                  </Typography>
                  <Typography variant="code" sx={{ display: 'block', whiteSpace: 'pre-wrap' }}>
                    <Box component="span" sx={{ color: 'text.disabled' }}>
                      {t('solve.output')}:{' '}
                    </Box>
                    {example.output}
                  </Typography>
                  {example.explanation ? (
                    <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
                      {example.explanation}
                    </Typography>
                  ) : null}
                </Stack>
              </Paper>
            ))}
          </Stack>
        ) : null}

        {problem.constraintsMarkdown ? (
          <Box>
            <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 1 }}>
              {t('solve.constraints')}
            </Typography>
            <MarkdownBody>{problem.constraintsMarkdown}</MarkdownBody>
          </Box>
        ) : null}
      </Stack>
    </Box>
  );
};
