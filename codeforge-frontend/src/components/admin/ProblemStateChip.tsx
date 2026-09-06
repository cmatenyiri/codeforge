import { Chip, type ChipProps } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ProblemState } from '../../api/types';

/** Literal keys, so a renamed translation is a compile error. */
const STATE_LABEL_KEY = {
  DRAFT: 'admin.state.draft',
  PUBLISHED: 'admin.state.published',
  ARCHIVED: 'admin.state.archived',
} as const satisfies Record<ProblemState, string>;

const STATE_COLOR = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  ARCHIVED: 'default',
} as const satisfies Record<ProblemState, ChipProps['color']>;

/** Where a problem stands, as a badge. The only status an author reads at a glance. */
export const ProblemStateChip = ({ state, ...props }: { state: ProblemState } & ChipProps) => {
  const { t } = useTranslation();

  return <Chip {...props} color={STATE_COLOR[state]} label={t(STATE_LABEL_KEY[state])} />;
};
