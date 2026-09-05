import ClearRounded from '@mui/icons-material/ClearRounded';
import SearchRounded from '@mui/icons-material/SearchRounded';
import { Button, InputAdornment, MenuItem, Stack, TextField } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type Difficulty, type Tag } from '../../api/types';
import { DIFFICULTIES, DIFFICULTY_LABEL_KEY } from './difficulty';

export type Filters = { search: string; difficulty: '' | Difficulty; tag: string };

type ProblemFiltersProps = {
  filters: Filters;
  tags: Tag[];
  onChange: (next: Filters) => void;
};

export const ProblemFilters = ({ filters, tags, onChange }: ProblemFiltersProps) => {
  const { t } = useTranslation();
  const dirty = filters.search !== '' || filters.difficulty !== '' || filters.tag !== '';

  return (
    <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ alignItems: { md: 'center' } }}>
      <TextField
        placeholder={t('problems.searchPlaceholder')}
        value={filters.search}
        onChange={(event) => {
          onChange({ ...filters, search: event.target.value });
        }}
        sx={{ flex: 1, minWidth: 200 }}
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
        label={t('problems.difficulty')}
        value={filters.difficulty}
        onChange={(event) => {
          onChange({ ...filters, difficulty: event.target.value as Filters['difficulty'] });
        }}
        sx={{ minWidth: 150 }}
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
        value={filters.tag}
        onChange={(event) => {
          onChange({ ...filters, tag: event.target.value });
        }}
        sx={{ minWidth: 170 }}
      >
        <MenuItem value="">{t('problems.anyTopic')}</MenuItem>
        {tags.map((tag) => (
          <MenuItem key={tag.slug} value={tag.slug}>
            {tag.name}
          </MenuItem>
        ))}
      </TextField>

      {dirty ? (
        <Button
          variant="text"
          startIcon={<ClearRounded />}
          onClick={() => {
            onChange({ search: '', difficulty: '', tag: '' });
          }}
        >
          {t('problems.clearFilters')}
        </Button>
      ) : null}
    </Stack>
  );
};
