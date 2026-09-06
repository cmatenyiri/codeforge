import ArrowDownwardRounded from '@mui/icons-material/ArrowDownwardRounded';
import ArrowUpwardRounded from '@mui/icons-material/ArrowUpwardRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import { Box, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type ReactNode } from 'react';

type RepeatableRowProps = {
  label: string;
  /** Rendered next to the label — a "hidden" switch, a verdict chip. */
  badge?: ReactNode;
  index: number;
  count: number;
  onMove: (delta: number) => void;
  onRemove: () => void;
  children: ReactNode;
};

/**
 * The chrome around one row of a repeatable list.
 *
 * <p>Order is meaningful in every list this wraps — examples are numbered in the
 * statement, hints are revealed in sequence, a test case's position is the case
 * number a solver sees fail — so every row can be moved, and the arrows are
 * disabled rather than hidden at the ends.
 */
export const RepeatableRow = ({ label, badge, index, count, onMove, onRemove, children }: RepeatableRowProps) => {
  const { t } = useTranslation();

  return (
    <Box sx={{ border: 1, borderColor: 'border.subtle', borderRadius: 1.5, p: 2, backgroundColor: 'surface.sunken' }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1.5 }}>
        <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
          {label}
        </Typography>
        {badge}
        <Box sx={{ flex: 1 }} />
        <Tooltip title={t('admin.form.moveUp')}>
          <span>
            <IconButton
              size="small"
              aria-label={t('admin.form.moveUp')}
              disabled={index === 0}
              onClick={() => {
                onMove(-1);
              }}
            >
              <ArrowUpwardRounded fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title={t('admin.form.moveDown')}>
          <span>
            <IconButton
              size="small"
              aria-label={t('admin.form.moveDown')}
              disabled={index === count - 1}
              onClick={() => {
                onMove(1);
              }}
            >
              <ArrowDownwardRounded fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title={t('admin.form.remove')}>
          <IconButton size="small" color="error" aria-label={t('admin.form.remove')} onClick={onRemove}>
            <DeleteOutlineRounded fontSize="small" />
          </IconButton>
        </Tooltip>
      </Stack>
      {children}
    </Box>
  );
};
