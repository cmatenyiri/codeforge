import LightbulbRounded from '@mui/icons-material/LightbulbRounded';
import MemoryRounded from '@mui/icons-material/MemoryRounded';
import ScheduleRounded from '@mui/icons-material/ScheduleRounded';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toApiError } from '../../api/api-error';
import { problemsApi } from '../../api/problems-api';
import { type Editorial, type Language } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { EditorialCode } from './EditorialCode';
import { MarkdownBody } from './MarkdownBody';
import { LANGUAGE_LABEL } from './verdict';

/** Matches the editor's default, so the tab opens on the language being written. */
const PREFERRED_LANGUAGE: Language = 'JAVA';

const ComplexityChip = ({ icon, label, value }: { icon: React.ReactElement; label: string; value: string }) => (
  <Chip
    icon={icon}
    label={
      <Box component="span">
        <Box component="span" sx={{ color: 'text.disabled' }}>{`${label} `}</Box>
        <Box component="span" sx={{ typography: 'mono', color: 'text.primary' }}>
          {value}
        </Box>
      </Box>
    }
    variant="outlined"
    sx={{ '& .MuiChip-icon': { color: 'text.disabled' } }}
  />
);

/**
 * The written solution: how to think about the problem, then code that passes.
 *
 * <p>Fetched when the tab is opened rather than with the problem — it is the
 * largest thing a problem owns, and most visits to a problem never read it.
 */
export const EditorialPanel = ({ slug }: { slug: string }) => {
  const { t } = useTranslation();
  const message = useMessages();

  const [editorial, setEditorial] = useState<Editorial | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [missing, setMissing] = useState(false);
  const [language, setLanguage] = useState<Language | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    problemsApi
      .editorial(slug)
      .then((data) => {
        if (!cancelled) {
          setEditorial(data);
          setError(null);
          setMissing(false);
        }
      })
      .catch((caught: unknown) => {
        if (cancelled) {
          return;
        }
        const apiError = toApiError(caught);
        // Nothing written yet is an ordinary state, not a failure to report.
        if (apiError.status === 404) {
          setMissing(true);
          return;
        }
        setError(message(apiError.code, apiError.message));
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [slug, message, t]);

  const languages = useMemo(
    () => (editorial ? (Object.keys(editorial.solutions) as Language[]) : []),
    [editorial],
  );
  const selected = language ?? (languages.includes(PREFERRED_LANGUAGE) ? PREFERRED_LANGUAGE : languages[0]);
  const solution = selected === undefined ? undefined : editorial?.solutions[selected];

  if (loading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', p: 6 }}>
        <CircularProgress size={22} />
      </Box>
    );
  }

  if (missing) {
    return (
      <Stack spacing={0.5} sx={{ alignItems: 'center', py: 7, px: 3 }}>
        <Typography variant="subtitle1">{t('solve.noEditorial')}</Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('solve.noEditorialBody')}
        </Typography>
      </Stack>
    );
  }

  if (error || !editorial) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error ?? t('error.internal')}</Alert>
      </Box>
    );
  }

  return (
    <Stack spacing={3} sx={{ p: 3 }}>
      <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <Chip
          icon={<LightbulbRounded />}
          label={t('solve.editorial')}
          sx={{ color: 'brand.ember', backgroundColor: 'difficulty.mediumBg', '& .MuiChip-icon': { color: 'inherit' } }}
        />
        {editorial.timeComplexity ? (
          <ComplexityChip
            icon={<ScheduleRounded fontSize="small" />}
            label={t('solve.time')}
            value={editorial.timeComplexity}
          />
        ) : null}
        {editorial.spaceComplexity ? (
          <ComplexityChip
            icon={<MemoryRounded fontSize="small" />}
            label={t('solve.space')}
            value={editorial.spaceComplexity}
          />
        ) : null}
      </Stack>

      <MarkdownBody>{editorial.contentMarkdown}</MarkdownBody>

      {solution && selected ? (
        <Stack spacing={1.5}>
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="overline" sx={{ color: 'text.disabled' }}>
              {t('solve.referenceSolution')}
            </Typography>
            <Select
              size="small"
              value={selected}
              onChange={(event) => {
                setLanguage(event.target.value as Language);
              }}
              inputProps={{ 'aria-label': t('solve.programmingLanguage') }}
              sx={{ width: 150 }}
            >
              {languages.map((option) => (
                <MenuItem key={option} value={option}>
                  {LANGUAGE_LABEL[option]}
                </MenuItem>
              ))}
            </Select>
          </Stack>

          <EditorialCode code={solution} language={selected} />
        </Stack>
      ) : null}
    </Stack>
  );
};
