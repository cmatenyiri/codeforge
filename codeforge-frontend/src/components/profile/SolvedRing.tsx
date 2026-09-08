import { Box, CircularProgress, Typography } from '@mui/material';

/**
 * The solved count as a ring.
 *
 * <p>Two stacked arcs rather than one: the back track is the whole catalogue and
 * the front is the part done, so the ring reads as a fraction at a glance
 * without anyone parsing "412 / 3600". The numbers stay in the middle because
 * the ring answers "how far", not "how many".
 */
export const SolvedRing = ({
  solved,
  total,
  label,
  size = 128,
}: {
  solved: number;
  total: number;
  label: string;
  size?: number;
}) => {
  const fraction = total === 0 ? 0 : (solved / total) * 100;

  return (
    <Box sx={{ position: 'relative', width: size, height: size, flexShrink: 0 }}>
      <CircularProgress
        variant="determinate"
        value={100}
        size={size}
        thickness={3.5}
        sx={{ color: 'surface.sunken', position: 'absolute', inset: 0 }}
      />
      <CircularProgress
        variant="determinate"
        value={fraction}
        size={size}
        thickness={3.5}
        // The round cap keeps a very small fraction visible as a mark rather
        // than vanishing into the track.
        sx={{ color: 'verdict.accepted', position: 'absolute', inset: 0, '& circle': { strokeLinecap: 'round' } }}
      />
      <Box sx={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center' }}>
        <Box sx={{ textAlign: 'center' }}>
          <Typography variant="metric" sx={{ lineHeight: 1.1 }}>
            {solved}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block' }}>
            / {total}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block' }}>
            {label}
          </Typography>
        </Box>
      </Box>
    </Box>
  );
};
