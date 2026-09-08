import LocalFireDepartmentRounded from '@mui/icons-material/LocalFireDepartmentRounded';
import MilitaryTechRounded from '@mui/icons-material/MilitaryTechRounded';
import { Box, Paper, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type Badge } from '../../api/types';

/**
 * The badges the daily challenge awards.
 *
 * <p>Earned ones are drawn in full colour; the month and year still in progress
 * are drawn faded with their real percentage. Showing the unfinished ones is the
 * point rather than clutter — a badge nobody can see themselves approaching is
 * a badge nobody chases, and the monthly one has to be visible on the third of
 * the month to do its job.
 */
export const BadgeList = ({ badges }: { badges: Badge[] }) => {
  const { t } = useTranslation();

  if (badges.length === 0) {
    return (
      <Box>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('publicProfile.badgesNone')}
        </Typography>
        <Typography variant="caption" sx={{ color: 'text.disabled' }}>
          {t('publicProfile.badgesNoneBody')}
        </Typography>
      </Box>
    );
  }

  return (
    <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: 'wrap' }}>
      {badges.map((badge) => {
        const monthly = badge.kind === 'MONTHLY';
        const Icon = monthly ? LocalFireDepartmentRounded : MilitaryTechRounded;

        return (
          <Tooltip
            key={`${badge.kind}-${badge.name}`}
            title={
              badge.earned
                ? badge.name
                : `${badge.name} · ${t('publicProfile.badgeLocked', { percent: Math.round(badge.progress * 100) })}`
            }
          >
            <Paper
              variant="outlined"
              sx={{
                px: 1.5,
                py: 1.25,
                minWidth: 96,
                textAlign: 'center',
                opacity: badge.earned ? 1 : 0.45,
                borderColor: badge.earned ? 'border.strong' : 'border.subtle',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <Icon
                sx={{
                  fontSize: 26,
                  color: badge.earned ? (monthly ? 'brand.ember' : 'primary.main') : 'text.disabled',
                }}
              />
              <Typography variant="caption" sx={{ display: 'block', fontWeight: 600 }}>
                {badge.name}
              </Typography>

              {/* A fill line along the bottom of the unfinished ones, so the
                  distance left is readable without opening the tooltip. */}
              {badge.earned ? null : (
                <Box
                  sx={{
                    position: 'absolute',
                    left: 0,
                    bottom: 0,
                    height: 3,
                    width: `${Math.round(badge.progress * 100)}%`,
                    backgroundColor: 'primary.main',
                  }}
                />
              )}
            </Paper>
          </Tooltip>
        );
      })}
    </Stack>
  );
};
