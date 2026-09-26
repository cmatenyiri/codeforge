import LinkOffRounded from '@mui/icons-material/LinkOffRounded';
import LinkRounded from '@mui/icons-material/LinkRounded';
import { Alert, InputAdornment, MenuItem, Stack, TextField, Tooltip, IconButton } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ContestHold, type ProblemState, type Tag } from '../../api/types';
import { useMessages } from '../../i18n/use-messages';
import { DIFFICULTIES, DIFFICULTY_LABEL_KEY } from '../problems/difficulty';
import { FormSection } from './FormSection';
import { TagPicker } from './TagPicker';
import { slugify, type ProblemFormState } from './problem-form';

const STATE_LABEL_KEY = {
  DRAFT: 'admin.state.draft',
  IN_CONTEST: 'admin.state.inContest',
  PUBLISHED: 'admin.state.published',
  ARCHIVED: 'admin.state.archived',
} as const satisfies Record<ProblemState, string>;

const STATE_HELP_KEY = {
  DRAFT: 'admin.form.stateHelpDraft',
  IN_CONTEST: 'admin.form.stateHelpInContest',
  PUBLISHED: 'admin.form.stateHelpPublished',
  ARCHIVED: 'admin.form.stateHelpArchived',
} as const satisfies Record<ProblemState, string>;

/** What an author can choose. `IN_CONTEST` is not a choice: announcing a contest puts a problem there. */
const SELECTABLE_STATES: ProblemState[] = ['DRAFT', 'PUBLISHED', 'ARCHIVED'];

type ProblemBasicsSectionProps = {
  form: ProblemFormState;
  onChange: (patch: Partial<ProblemFormState>) => void;
  errorOf: (field: string) => string | undefined;
  /** Every reason the server gave for refusing to publish, not just the first. */
  publishErrors: string[];
  /** The announced contest holding this problem, which locks the state until it ends. */
  heldBy?: ContestHold;
  tags: Tag[];
  onTagCreated: (tag: Tag) => void;
};

/**
 * Identity and visibility: what the problem is called, where it lives, and who
 * can see it.
 *
 * <p>The three states are one control rather than two switches. "Published" and
 * "archived" are stored separately — un-archiving has to put a problem back
 * exactly where it was — but as a pair of toggles they offer a combination
 * ("archived draft") that means nothing to the person setting it.
 *
 * <p>A problem an announced contest holds shows a fourth state and cannot leave
 * it from here: the contest publishes it when it ends, and until then it has to
 * stay complete enough to publish, which is what the server's messages say.
 */
export const ProblemBasicsSection = ({
  form,
  onChange,
  errorOf,
  publishErrors,
  heldBy,
  tags,
  onTagCreated,
}: ProblemBasicsSectionProps) => {
  const { t } = useTranslation();
  const message = useMessages();

  const state: ProblemState = heldBy
    ? 'IN_CONTEST'
    : form.archived
      ? 'ARCHIVED'
      : form.published
        ? 'PUBLISHED'
        : 'DRAFT';

  const changeState = (next: ProblemState) => {
    if (next === 'ARCHIVED') {
      // `published` is left as it was, so restoring an archived problem returns
      // it to the catalogue rather than silently demoting it to a draft.
      onChange({ archived: true });
      return;
    }
    onChange({ archived: false, published: next === 'PUBLISHED' });
  };

  return (
    <FormSection title={t('admin.form.basics')} description={t('admin.form.basicsHelp')}>
      <TextField
        label={t('admin.form.title')}
        value={form.title}
        onChange={(event) => {
          const title = event.target.value;
          // The slug follows the title only until the author takes it over.
          onChange(form.slugLocked ? { title } : { title, slug: slugify(title) });
        }}
        error={Boolean(errorOf('title'))}
        helperText={errorOf('title') ?? ' '}
        fullWidth
      />

      <TextField
        label={t('admin.form.slug')}
        value={form.slug}
        onChange={(event) => {
          onChange({ slug: event.target.value, slugLocked: true });
        }}
        error={Boolean(errorOf('slug'))}
        helperText={errorOf('slug') ?? t('admin.form.slugHelp', { slug: form.slug || '…' })}
        fullWidth
        slotProps={{
          input: {
            sx: { typography: 'mono' },
            startAdornment: <InputAdornment position="start">/problems/</InputAdornment>,
            endAdornment: (
              <InputAdornment position="end">
                <Tooltip title={form.slugLocked ? t('admin.form.slugUnlock') : t('admin.form.slugLock')}>
                  <IconButton
                    size="small"
                    aria-label={form.slugLocked ? t('admin.form.slugUnlock') : t('admin.form.slugLock')}
                    onClick={() => {
                      onChange(
                        form.slugLocked ? { slugLocked: false, slug: slugify(form.title) } : { slugLocked: true },
                      );
                    }}
                  >
                    {form.slugLocked ? <LinkOffRounded fontSize="small" /> : <LinkRounded fontSize="small" />}
                  </IconButton>
                </Tooltip>
              </InputAdornment>
            ),
          },
        }}
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          select
          label={t('admin.form.difficulty')}
          value={form.difficulty}
          onChange={(event) => {
            onChange({ difficulty: event.target.value as ProblemFormState['difficulty'] });
          }}
          error={Boolean(errorOf('difficulty'))}
          helperText={errorOf('difficulty') ?? ' '}
          sx={{ minWidth: 180 }}
        >
          {DIFFICULTIES.map((difficulty) => (
            <MenuItem key={difficulty} value={difficulty}>
              {t(DIFFICULTY_LABEL_KEY[difficulty])}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          label={t('admin.form.state')}
          value={state}
          onChange={(event) => {
            changeState(event.target.value as ProblemState);
          }}
          disabled={heldBy !== undefined}
          helperText={t(STATE_HELP_KEY[state], { title: heldBy?.title })}
          sx={{ minWidth: 200 }}
        >
          {(heldBy ? (['IN_CONTEST'] as ProblemState[]) : SELECTABLE_STATES).map((candidate) => (
            <MenuItem key={candidate} value={candidate}>
              {t(STATE_LABEL_KEY[candidate])}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {publishErrors.length > 0 ? (
        <Alert severity="warning">
          {heldBy ? t('admin.form.heldBlocked') : t('admin.form.publishBlocked')}
          <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
            {publishErrors.map((code) => (
              <li key={code}>{message(code)}</li>
            ))}
          </ul>
        </Alert>
      ) : null}

      <TagPicker
        tags={tags}
        value={form.tagIds}
        onChange={(tagIds) => {
          onChange({ tagIds });
        }}
        onTagCreated={onTagCreated}
        error={errorOf('tagIds')}
      />
    </FormSection>
  );
};
