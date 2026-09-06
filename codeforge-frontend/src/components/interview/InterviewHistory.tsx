import {
  Box,
  Chip,
  Divider,
  Link as MuiLink,
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type InterviewSummary } from '../../api/types';
import { interviewReportPath } from '../../routes/paths';
import { FORMAT_LABEL_KEY, OUTCOME_LABEL_KEY, OUTCOME_TOKEN } from './interview';

/**
 * Past rounds.
 *
 * <p>Abandoned ones are listed rather than hidden, and without a band. A history
 * that quietly dropped the rounds somebody walked out of would flatter them, and
 * how often you walk out is itself worth seeing.
 */
export const InterviewHistory = ({ interviews }: { interviews: InterviewSummary[] | null }) => {
  const { t, i18n } = useTranslation();

  const timestamp = (iso: string) =>
    new Intl.DateTimeFormat(i18n.resolvedLanguage, { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(iso),
    );

  return (
    <Paper variant="outlined">
      <Typography variant="subtitle1" sx={{ px: 2, py: 1.5 }}>
        {t('interview.pastRounds')}
      </Typography>
      <Divider />

      {interviews === null ? (
        <Box sx={{ p: 2 }}>
          <Skeleton height={28} />
          <Skeleton height={28} />
          <Skeleton height={28} />
        </Box>
      ) : interviews.length === 0 ? (
        <Stack spacing={0.5} sx={{ alignItems: 'center', py: 6, px: 2 }}>
          <Typography variant="subtitle1">{t('interview.noHistory')}</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('interview.noHistoryBody')}
          </Typography>
        </Stack>
      ) : (
        <Box sx={{ overflowX: 'auto' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('interview.round')}</TableCell>
                <TableCell width={160}>{t('interview.format.label')}</TableCell>
                <TableCell width={110} align="right">
                  {t('interview.solved')}
                </TableCell>
                <TableCell width={170}>{t('interview.outcome.label')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {interviews.map((interview) => (
                <TableRow key={interview.id} hover>
                  <TableCell>
                    <MuiLink
                      component={Link}
                      to={interviewReportPath(interview.id)}
                      underline="hover"
                      sx={{ typography: 'body2', color: 'text.primary' }}
                    >
                      {timestamp(interview.startedAt)}
                    </MuiLink>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {t(FORMAT_LABEL_KEY[interview.format])}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                      {interview.score === undefined
                        ? '—'
                        : `${interview.score} / ${interview.total}`}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {interview.status === 'ABANDONED' ? (
                      <Chip label={t('interview.statusAbandoned')} variant="outlined" />
                    ) : interview.status === 'IN_PROGRESS' ? (
                      <Chip label={t('interview.statusRunning')} variant="outlined" />
                    ) : interview.outcome === undefined ? null : (
                      <Chip
                        label={t(OUTCOME_LABEL_KEY[interview.outcome])}
                        sx={{
                          color: `verdict.${OUTCOME_TOKEN[interview.outcome]}`,
                          backgroundColor: `verdict.${OUTCOME_TOKEN[interview.outcome]}Bg`,
                          fontWeight: 600,
                        }}
                      />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
    </Paper>
  );
};
