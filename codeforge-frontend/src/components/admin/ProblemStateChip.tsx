import { Chip, Tooltip, type ChipProps } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ContestHold, type ProblemState } from '../../api/types';

/** Literal keys, so a renamed translation is a compile error. */
const STATE_LABEL_KEY = {
  DRAFT: 'admin.state.draft',
  IN_CONTEST: 'admin.state.inContest',
  PUBLISHED: 'admin.state.published',
  ARCHIVED: 'admin.state.archived',
} as const satisfies Record<ProblemState, string>;

const STATE_COLOR = {
  DRAFT: 'warning',
  IN_CONTEST: 'info',
  PUBLISHED: 'success',
  ARCHIVED: 'default',
} as const satisfies Record<ProblemState, ChipProps['color']>;

/**
 * Where a problem stands, as a badge. The only status an author reads at a glance.
 *
 * <p>A problem held by a contest names the contest on hover, since "in contest"
 * alone does not say which one will publish it.
 */
export const ProblemStateChip = ({
  state,
  heldBy,
  ...props
}: { state: ProblemState; heldBy?: ContestHold } & ChipProps) => {
  const { t } = useTranslation();

  const chip = <Chip {...props} color={STATE_COLOR[state]} label={t(STATE_LABEL_KEY[state])} />;

  return state === 'IN_CONTEST' && heldBy ? (
    <Tooltip title={t('admin.state.inContestHint', { title: heldBy.title })}>{chip}</Tooltip>
  ) : (
    chip
  );
};
