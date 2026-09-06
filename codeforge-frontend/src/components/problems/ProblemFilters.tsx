import CasinoRounded from '@mui/icons-material/CasinoRounded';
import ClearRounded from '@mui/icons-material/ClearRounded';
import SearchRounded from '@mui/icons-material/SearchRounded';
import { Button, InputAdornment, MenuItem, Stack, TextField } from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type ProblemStatusFilter, type Tag } from '../../api/types';
import { type ProblemQueryState } from './problem-query';
import { DIFFICULTIES, DIFFICULTY_LABEL_KEY } from './difficulty';

/** Literal keys, so a renamed translation is a compile error. */
const STATUS_LABEL_KEY = {
  TODO: 'problems.statusTodo',
  ATTEMPTED: 'problems.statusAttempted',
  SOLVED: 'problems.statusSolved',
} as const satisfies Record<ProblemStatusFilter, string>;

const STATUSES = Object.keys(STATUS_LABEL_KEY) as ProblemStatusFilter[];

type ProblemFiltersProps = {
  query: ProblemQueryState;
  tags: Tag[];
  onChange: (patch: Partial<ProblemQueryState>) => void;
  onClear: () => void;
  onPickRandom: () => void;
  picking: boolean;
  filtered: boolean;
};

/**
 * The catalogue's toolbar.
 *
 * <p>The search box keeps its own state and reports upward on a pause. The URL
 * is the source of truth for every other control, but a text field driven
 * straight from it fights the typist: each keystroke would rewrite history and
 * re-render the page under the cursor.
 */
export const ProblemFilters = ({
  query,
  tags,
  onChange,
  onClear,
  onPickRandom,
  picking,
  filtered,
}: ProblemFiltersProps) => {
  const { t } = useTranslation();
  const [search, setSearch] = useState(query.search);

  // Follows the URL when it changes from elsewhere — a cleared filter, the back
  // button — without interfering while the field itself is being typed into.
  useEffect(() => {
    setSearch(query.search);
  }, [query.search]);

  useEffect(() => {
    if (search === query.search) {
      return;
    }
    const timer = setTimeout(() => {
      onChange({ search });
    }, 300);

    return () => {
      clearTimeout(timer);
    };
  }, [search, query.search, onChange]);

  return (
    <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap', alignItems: 'center' }}>
      <TextField
        placeholder={t('problems.searchPlaceholder')}
        value={search}
        onChange={(event) => {
          setSearch(event.target.value);
        }}
        sx={{ flex: '1 1 220px', minWidth: 180 }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchRounded fontSize="small" />
              </InputAdornment>
            ),
            endAdornment: search ? (
              <InputAdornment position="end">
                <ClearRounded
                  fontSize="small"
                  role="button"
                  aria-label={t('problems.clearSearch')}
                  tabIndex={0}
                  onClick={() => {
                    setSearch('');
                  }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      setSearch('');
                    }
                  }}
                  sx={{ cursor: 'pointer', color: 'text.disabled' }}
                />
              </InputAdornment>
            ) : null,
          },
        }}
      />

      <TextField
        select
        label={t('problems.status')}
        value={query.status}
        onChange={(event) => {
          onChange({ status: event.target.value as ProblemQueryState['status'] });
        }}
        sx={{ minWidth: 140 }}
      >
        <MenuItem value="">{t('problems.anyStatus')}</MenuItem>
        {STATUSES.map((status) => (
          <MenuItem key={status} value={status}>
            {t(STATUS_LABEL_KEY[status])}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        select
        label={t('problems.difficulty')}
        value={query.difficulty}
        onChange={(event) => {
          onChange({ difficulty: event.target.value as ProblemQueryState['difficulty'] });
        }}
        sx={{ minWidth: 140 }}
      >
        <MenuItem value="">{t('problems.anyDifficulty')}</MenuItem>
        {DIFFICULTIES.map((difficulty) => (
          <MenuItem key={difficulty} value={difficulty}>
            {t(DIFFICULTY_LABEL_KEY[difficulty])}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        select
        label={t('problems.topic')}
        value={query.tag}
        onChange={(event) => {
          onChange({ tag: event.target.value });
        }}
        sx={{ minWidth: 160 }}
      >
        <MenuItem value="">{t('problems.anyTopic')}</MenuItem>
        {tags.map((tag) => (
          <MenuItem key={tag.slug} value={tag.slug}>
            {tag.name}
          </MenuItem>
        ))}
      </TextField>

      {filtered ? (
        <Button variant="text" startIcon={<ClearRounded />} onClick={onClear}>
          {t('problems.clearFilters')}
        </Button>
      ) : null}

      <Button
        variant="soft"
        startIcon={<CasinoRounded />}
        onClick={onPickRandom}
        disabled={picking}
        sx={{ ml: { sm: 'auto' } }}
      >
        {t('problems.pickRandom')}
      </Button>
    </Stack>
  );
};
