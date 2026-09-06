import AddRounded from '@mui/icons-material/AddRounded';
import {
  Autocomplete,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useState, type SubmitEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi } from '../../api/admin-api';
import { toApiError } from '../../api/api-error';
import { type Tag } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';

type TagPickerProps = {
  tags: Tag[];
  value: number[];
  onChange: (tagIds: number[]) => void;
  /** Publishes a tag this picker created, so the page's list stays complete. */
  onTagCreated: (tag: Tag) => void;
  error?: string;
  disabled?: boolean;
};

/**
 * Topics for a problem, plus the one place new ones are minted.
 *
 * <p>Creating a topic is deliberately a second, explicit act rather than a
 * free-text field that invents one on save. The topic list is the catalogue's
 * filter vocabulary, and it is only useful while "Two Pointers", "two pointers"
 * and "Two-Pointer" are not three separate entries in it.
 */
export const TagPicker = ({ tags, value, onChange, onTagCreated, error, disabled = false }: TagPickerProps) => {
  const { t } = useTranslation();
  const message = useMessages();

  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const selected = value.map((id) => tags.find((tag) => tag.id === id)).filter((tag): tag is Tag => tag !== undefined);

  const close = () => {
    setCreating(false);
    setName('');
    setCreateError(null);
  };

  const submit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setCreateError(null);
    try {
      const tag = await adminApi.createTag(name);
      onTagCreated(tag);
      // Selected straight away: a topic is created because this problem needs
      // it, so making the author then find it in the list is a wasted step.
      onChange([...value, tag.id]);
      close();
    } catch (caught) {
      const apiError = toApiError(caught);
      setCreateError(
        apiError.hasFieldErrors
          ? message(apiError.fieldErrors[0].code, apiError.fieldErrors[0].message)
          : message(apiError.code, apiError.message),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
        <Autocomplete
          multiple
          disabled={disabled}
          options={tags}
          value={selected}
          getOptionLabel={(tag) => tag.name}
          isOptionEqualToValue={(option, candidate) => option.id === candidate.id}
          onChange={(_, next) => {
            onChange(next.map((tag) => tag.id));
          }}
          renderValue={(picked, getItemProps) =>
            picked.map((tag, index) => <Chip {...getItemProps({ index })} key={tag.id} label={tag.name} />)
          }
          renderInput={(params) => (
            <TextField
              {...params}
              label={t('admin.form.topics')}
              error={Boolean(error)}
              helperText={error ?? ' '}
              placeholder={t('admin.form.topicsPlaceholder')}
            />
          )}
          sx={{ flex: 1 }}
        />

        <Button
          variant="text"
          startIcon={<AddRounded />}
          disabled={disabled}
          onClick={() => {
            setCreating(true);
          }}
          sx={{ mt: 1 }}
        >
          {t('admin.form.newTopic')}
        </Button>
      </Stack>

      <Dialog open={creating} onClose={close} maxWidth="xs" fullWidth>
        <form onSubmit={submit} noValidate>
          <DialogTitle>{t('admin.form.newTopic')}</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              fullWidth
              label={t('admin.form.topicName')}
              value={name}
              onChange={(event) => {
                setName(event.target.value);
                setCreateError(null);
              }}
              error={Boolean(createError)}
              helperText={createError ?? ' '}
              sx={{ mt: 1 }}
            />
          </DialogContent>
          <DialogActions>
            <Button variant="text" onClick={close}>
              {t('admin.form.cancel')}
            </Button>
            <Button type="submit" disabled={saving || name.trim() === ''}>
              {t('admin.form.create')}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </>
  );
};
