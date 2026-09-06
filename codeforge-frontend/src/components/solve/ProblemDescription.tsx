import CheckCircleRounded from '@mui/icons-material/CheckCircleRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Chip,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ProblemDetail } from '../../api/types';
import { DifficultyChip } from '../problems/DifficultyChip';
import LockRounded from '@mui/icons-material/LockRounded';
import { MarkdownBody } from './MarkdownBody';

export const ProblemDescription = ({ problem }: { problem: ProblemDetail }) => {
  const { t } = useTranslation();

  return (
    <Stack spacing={3} sx={{ p: 3 }}>
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="h3">{problem.title}</Typography>
          <DifficultyChip difficulty={problem.difficulty} />
          {problem.solved ? (
            <Chip
              icon={<CheckCircleRounded />}
              label={t('problems.solved')}
              sx={{
                color: 'verdict.accepted',
                backgroundColor: 'verdict.acceptedBg',
                '& .MuiChip-icon': { color: 'inherit' },
              }}
            />
          ) : null}
        </Stack>

        <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
          <Typography variant="body2" sx={{ color: 'text.disabled' }}>
            {t('solve.acceptanceStat', {
              rate:
                problem.acceptanceRate === undefined ? '—' : `${(problem.acceptanceRate * 100).toFixed(1)}%`,
              submissions: problem.totalSubmissions,
            })}
          </Typography>

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

        {problem.hints.length > 0 ? (
          <Box>
            <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 1 }}>
              {t('solve.hints')}
            </Typography>
            {problem.hints.map((hint, index) => (
              <Accordion key={index}>
                <AccordionSummary expandIcon={<ExpandMoreRounded fontSize="small" />}>
                  {t('solve.hint', { index: index + 1 })}
                </AccordionSummary>
                <AccordionDetails>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    {hint}
                  </Typography>
                </AccordionDetails>
              </Accordion>
            ))}
          </Box>
        ) : null}

        <Box>
          <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 1 }}>
            {t('solve.topics')}
          </Typography>
          <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap' }}>
            {problem.tags.map((tag) => (
              <Chip key={tag.slug} label={tag.name} variant="outlined" />
            ))}
          </Stack>
      </Box>
    </Stack>
  );
};
