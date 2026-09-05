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
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import Markdown from 'react-markdown';
import { useTranslation } from 'react-i18next';
import { type ProblemDetail } from '../../api/types';
import { DifficultyChip } from '../problems/DifficultyChip';

/**
 * Renders authored Markdown with the app's typography.
 *
 * <p>Only the elements the problem format actually uses are styled; anything
 * else falls through to sensible browser defaults.
 */
const MarkdownBody = ({ children }: { children: string }) => (
  <Box
    sx={{
      typography: 'body1',
      color: 'text.secondary',
      '& p': { m: 0, mb: 1.5 },
      '& p:last-child': { mb: 0 },
      '& strong': { color: 'text.primary', fontWeight: 650 },
      '& ul': { pl: 2.5, m: 0, mb: 1.5 },
      '& li': { mb: 0.5 },
      '& code': {
        typography: 'code',
        px: 0.5,
        py: '0.1em',
        borderRadius: 0.75,
        backgroundColor: 'surface.sunken',
        border: 1,
        borderColor: 'border.subtle',
        color: 'text.primary',
      },
    }}
  >
    <Markdown>{children}</Markdown>
  </Box>
);

export const ProblemDescription = ({ problem }: { problem: ProblemDetail }) => {
  const { t } = useTranslation();

  return (
    <Stack sx={{ height: '100%' }}>
      <Tabs value={0} sx={{ px: 1, borderBottom: 1, borderColor: 'border.subtle', flexShrink: 0 }}>
        <Tab label={t('solve.description')} />
        <Tab label={t('solve.editorial')} disabled />
        <Tab label={t('solve.submissions')} disabled />
      </Tabs>

      <Stack spacing={3} sx={{ p: 3, overflow: 'auto' }}>
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
    </Stack>
  );
};
