import { Chip, type ChipProps } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ExecutionStatus } from '../../api/types';
import { VERDICT_LABEL_KEY, VERDICT_TOKEN } from './verdict';

type VerdictChipProps = { status: ExecutionStatus; size?: ChipProps['size'] };

/** A judge verdict, in the palette's colour for it. */
export const VerdictChip = ({ status, size }: VerdictChipProps) => {
  const { t } = useTranslation();
  const token = VERDICT_TOKEN[status];

  return (
    <Chip
      size={size}
      label={t(VERDICT_LABEL_KEY[status])}
      sx={{
        color: `verdict.${token}`,
        backgroundColor: `verdict.${token}Bg`,
        fontWeight: 600,
      }}
    />
  );
};
