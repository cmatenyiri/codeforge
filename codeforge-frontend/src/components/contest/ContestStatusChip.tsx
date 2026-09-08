import { Chip, type ChipProps } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ContestStatus } from '../../api/types';
import { CONTEST_STATUS_LABEL_KEY, CONTEST_STATUS_TOKEN } from './contest';

/** Where a contest is in its life, coloured from `palette.verdict.*`. */
export const ContestStatusChip = ({ status, ...props }: { status: ContestStatus } & ChipProps) => {
  const { t } = useTranslation();
  const token = CONTEST_STATUS_TOKEN[status];

  return (
    <Chip
      {...props}
      label={t(CONTEST_STATUS_LABEL_KEY[status])}
      sx={{ color: `verdict.${token}`, backgroundColor: `verdict.${token}Bg`, ...props.sx }}
    />
  );
};
