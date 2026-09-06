import AddRounded from '@mui/icons-material/AddRounded';
import ClearRounded from '@mui/icons-material/ClearRounded';
import SearchRounded from '@mui/icons-material/SearchRounded';
import { Button, InputAdornment, MenuItem, Stack, TextField } from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { type ProblemState, type Tag } from '../../api/types';
import { paths } from '../../routes/paths';
import { DIFFICULTIES, DIFFICULTY_LABEL_KEY } from '../problems/difficulty';
import { type AdminQueryState } from './admin-problem-query';

const STATE_LABEL_KEY = {
  DRAFT: 'admin.state.draft',
  PUBLISHED: 'admin.state.published',
  ARCHIVED: 'admin.state.archived',
} as const satisfies Record<ProblemState, string>;

const STATES = Object.keys(STATE_LABEL_KEY) as ProblemState[];

type AdminProblemFiltersProps = {
  query: AdminQueryState;
  tags: Tag[];
  onChange: (patch: Partial<AdminQueryState>) => void;
  onClear: () => void;
  filtered: boolean;
};

/**
 * The authoring catalogue's toolbar.
 *
 * <p>Same debounce as the solver's: the search box keeps its own state and
 * reports upward on a pause, so typing does not rewrite history per keystroke.
 * The search matches slugs as well as titles — an author usually remembers the
 * URL they were last testing against.
 */
export const AdminProblemFilters = ({ query, tags, onChange, onClear, filtered }: AdminProblemFiltersProps) => {
  const { t } = useTranslation();
  const [search, setSearch] = useState(query.search);

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
        placeholder={t('admin.list.searchPlaceholder')}
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
          },
        }}
      />

      <TextField
        select
        label={t('admin.list.state')}
        value={query.state}
        onChange={(event) => {
          onChange({ state: event.target.value as AdminQueryState['state'] });
        }}
        sx={{ minWidth: 150 }}
      >
        <MenuItem value="">{t('admin.list.anyState')}</MenuItem>
        {STATES.map((state) => (
          <MenuItem key={state} value={state}>
            {t(STATE_LABEL_KEY[state])}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        select
        label={t('problems.difficulty')}
        value={query.difficulty}
        onChange={(event) => {
          onChange({ difficulty: event.target.value as AdminQueryState['difficulty'] });
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

      <Button component={Link} to={paths.adminProblemNew} startIcon={<AddRounded />} sx={{ ml: { sm: 'auto' } }}>
        {t('admin.list.newProblem')}
      </Button>
    </Stack>
  );
};
