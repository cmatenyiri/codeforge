import HelpOutlineRounded from '@mui/icons-material/HelpOutlineRounded';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

const RULES = ['rulesClock', 'rulesAccess', 'rulesScore', 'rulesPenalty', 'rulesFrozen', 'rulesRating'] as const;

/**
 * "How contests work", one click from wherever the question comes up.
 *
 * <p>A button in the header rather than a box at the foot of the page: below a
 * list of past contests the rules were exactly as hard to find as there were
 * contests, which is backwards — the more there have been, the more people
 * there are who have never read them.
 */
export const ContestRulesButton = () => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button
        variant="text"
        startIcon={<HelpOutlineRounded />}
        onClick={() => {
          setOpen(true);
        }}
        sx={{ whiteSpace: 'nowrap' }}
      >
        {t('contest.rulesButton')}
      </Button>

      <Dialog
        open={open}
        onClose={() => {
          setOpen(false);
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>{t('contest.rulesTitle')}</DialogTitle>
        <DialogContent>
          <Stack component="ul" spacing={1.25} sx={{ m: 0, pl: 2.5, color: 'text.secondary' }}>
            {RULES.map((key) => (
              <Typography key={key} component="li" variant="body2">
                {t(`contest.${key}`)}
              </Typography>
            ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setOpen(false);
            }}
          >
            {t('common.close')}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
