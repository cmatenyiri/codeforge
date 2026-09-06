import LightbulbOutlined from '@mui/icons-material/LightbulbOutlined';
import LockRounded from '@mui/icons-material/LockRounded';
import { Alert, Box, Button, Chip, Paper, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type InterviewProblemDetail } from '../../api/types';
import { DifficultyChip } from '../problems/DifficultyChip';
import { MarkdownBody } from '../solve/MarkdownBody';

type InterviewProblemPanelProps = {
  problem: InterviewProblemDetail;
  onRevealHint: () => void;
  revealing: boolean;
  /** False once the round is over, when hints are moot. */
  canRevealHints: boolean;
};

/**
 * The left half of the interview workspace.
 *
 * <p>Compare it with the solving page's panel and the differences are the
 * feature: no tabs, because there is no editorial and no submission history to
 * open; no acceptance rate, which is a hint about how hard the problem is; no
 * topic tags, which for a good many problems are the answer; and hints behind a
 * button that costs something rather than an accordion that does not.
 */
export const InterviewProblemPanel = ({
  problem,
  onRevealHint,
  revealing,
  canRevealHints,
}: InterviewProblemPanelProps) => {
  const { t } = useTranslation();
  const remainingHints = problem.hintCount - problem.hints.length;

  return (
    <Stack spacing={3} sx={{ p: 3, height: '100%', overflow: 'auto' }}>
      <Stack direction="row" spacing={1.5} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <Typography variant="h3">{problem.title}</Typography>
        <DifficultyChip difficulty={problem.difficulty} />
        {problem.warmUp ? <Chip label={t('interview.warmUp')} variant="outlined" /> : null}
        {problem.solved ? (
          <Chip
            label={t('interview.resultSolved')}
            sx={{ color: 'verdict.accepted', backgroundColor: 'verdict.acceptedBg' }}
          />
        ) : problem.skipped ? (
          <Chip label={t('interview.resultSkipped')} variant="outlined" />
        ) : null}
      </Stack>

      {problem.hiddenTestCaseCount > 0 ? (
        <Tooltip title={t('solve.hiddenCasesHint')}>
          {/* `alignSelf` keeps this to the width of its own text. Stretched to
              the column's full width — the default for a flex child — the
              tooltip would anchor to the centre of the panel rather than to the
              label it belongs to. */}
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ alignItems: 'center', alignSelf: 'flex-start', color: 'text.disabled' }}
          >
            <LockRounded sx={{ fontSize: 14 }} />
            <Typography variant="body2">
              {t('solve.hiddenCases', { count: problem.hiddenTestCaseCount })}
            </Typography>
          </Stack>
        </Tooltip>
      ) : null}

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

      {/* Hints revealed so far stay readable, but the offer to buy another one
          disappears with the problem: a greyed-out button and a note about what
          it would cost are noise on a problem that can no longer be worked on. */}
      {problem.hints.length > 0 || (problem.hintCount > 0 && canRevealHints) ? (
        <Box>
          <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block', mb: 1 }}>
            {t('solve.hints')}
          </Typography>

          <Stack spacing={1}>
            {problem.hints.map((hint, index) => (
              <Paper key={index} variant="sunken" sx={{ p: 1.5 }}>
                <Typography variant="overline" sx={{ color: 'text.disabled', display: 'block' }}>
                  {t('solve.hint', { index: index + 1 })}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {hint}
                </Typography>
              </Paper>
            ))}

            {remainingHints > 0 && canRevealHints ? (
              <Box>
                <Button
                  size="small"
                  variant="soft"
                  startIcon={<LightbulbOutlined />}
                  onClick={onRevealHint}
                  disabled={revealing}
                >
                  {t('interview.revealHint', { count: remainingHints })}
                </Button>
                {/* Said plainly rather than hidden behind the click: a hint you
                    did not know was being counted is not a choice. */}
                <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block', mt: 0.75 }}>
                  {t('interview.hintCost')}
                </Typography>
              </Box>
            ) : null}
          </Stack>
        </Box>
      ) : null}

      <Alert severity="info" variant="outlined" icon={<LockRounded fontSize="small" />}>
        {t('interview.editorialLocked')}
      </Alert>
    </Stack>
  );
};
