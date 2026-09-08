import { Box, Stack, Tooltip, Typography } from '@mui/material';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { type RatingPoint } from '../../api/types';
import { formatRating } from '../contest/contest';

const HEIGHT = 160;

/**
 * Inset around the plot, in viewBox units.
 *
 * <p>The viewBox is 100 units wide and stretched to the panel, so a unit here is
 * a percent of the width — which is why `left` is 2 and not 40. It once was 40,
 * reserving room for y-axis labels that ended up rendered outside the SVG
 * instead (see below), and the reservation quietly ate two-fifths of the panel.
 * Top and bottom are real pixels, since the vertical axis is not scaled.
 */
const PADDING = { top: 12, right: 2, bottom: 20, left: 2 };

/**
 * The rating over time, drawn inside the Contest Rating panel.
 *
 * <p>No card of its own: the graph and the three numbers above it are one
 * statement about one thing, and splitting them into two panels made the reader
 * join them up by eye. It renders nothing at all when there is no history —
 * the panel handles that case, because "no rating yet" is a sentence about the
 * account rather than about the chart.
 *
 * <p>Inline SVG rather than a charting library: it is one line over at most a
 * few hundred points, and the whole thing is fewer lines of code than the import
 * would be. It also means the colours come from the theme like everything else,
 * so it follows the light/dark switch without a second palette to maintain.
 */
export const RatingGraph = ({ history }: { history: RatingPoint[] }) => {
  const { i18n } = useTranslation();

  const geometry = useMemo(() => {
    if (history.length === 0) {
      return null;
    }

    // The starting point counts: a first contest is a movement from 1500, and a
    // graph that began at the result would hide the largest jump most people
    // ever make.
    const values = [history[0]!.ratingBefore, ...history.map((point) => point.ratingAfter)];
    const min = Math.min(...values);
    const max = Math.max(...values);
    // A flat history would divide by zero and, worse, draw a line at the very
    // top of the box; padding it keeps a single contest looking like a point
    // rather than a trend.
    const spread = Math.max(max - min, 40);
    const low = min - spread * 0.15;
    const high = max + spread * 0.15;

    const width = 100;
    const x = (index: number) =>
      PADDING.left + (values.length === 1 ? 0 : (index / (values.length - 1)) * (width - PADDING.left - PADDING.right));
    const y = (value: number) =>
      PADDING.top + (1 - (value - low) / (high - low)) * (HEIGHT - PADDING.top - PADDING.bottom);

    return {
      points: values.map((value, index) => ({ x: x(index), y: y(value), value })),
      path: values.map((value, index) => `${index === 0 ? 'M' : 'L'} ${x(index)} ${y(value)}`).join(' '),
      low,
      high,
      y,
    };
  }, [history]);

  if (geometry === null) {
    return null;
  }

  return (
    <Box sx={{ position: 'relative', mt: 2 }}>
      <Box
        component="svg"
        viewBox={`0 0 100 ${HEIGHT}`}
        preserveAspectRatio="none"
        sx={{ width: '100%', height: HEIGHT, display: 'block', overflow: 'visible' }}
      >
        {[geometry.high, (geometry.high + geometry.low) / 2, geometry.low].map((value) => (
          <g key={value}>
            <line
              x1={PADDING.left}
              x2={100 - PADDING.right}
              y1={geometry.y(value)}
              y2={geometry.y(value)}
              stroke="currentColor"
              strokeWidth={0.15}
              opacity={0.25}
            />
            {/* Non-scaling text inside a stretched viewBox would be squashed
                horizontally, so the labels sit outside the SVG below. */}
          </g>
        ))}

        <path
          d={geometry.path}
          fill="none"
          stroke="currentColor"
          strokeWidth={0.4}
          vectorEffect="non-scaling-stroke"
          style={{ color: 'var(--mui-palette-primary-main)' }}
        />
      </Box>

      {/* The points are absolutely positioned rather than drawn as SVG
          circles: a stretched viewBox would turn them into ellipses, and
          they need real hover targets for the tooltip anyway. */}
      {geometry.points.slice(1).map((point, index) => {
        const entry = history[index]!;

        return (
          <Tooltip
            key={entry.contestSlug}
            title={`${entry.contestTitle} · #${entry.rank}/${entry.participantCount} · ${formatRating(entry.ratingAfter)}`}
          >
            <Box
              sx={{
                position: 'absolute',
                left: `${point.x}%`,
                top: point.y,
                width: 8,
                height: 8,
                ml: '-4px',
                mt: '-4px',
                borderRadius: '50%',
                backgroundColor: entry.delta >= 0 ? 'verdict.accepted' : 'verdict.wrongAnswer',
                border: 2,
                borderColor: 'surface.paper',
              }}
            />
          </Tooltip>
        );
      })}

      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', mt: 0.5, color: 'text.disabled' }}
      >
        <Typography variant="caption">
          {new Date(history[0]!.startsAt).toLocaleDateString(i18n.language, { dateStyle: 'medium' })}
        </Typography>
        <Typography variant="caption">
          {formatRating(geometry.low)} – {formatRating(geometry.high)}
        </Typography>
        <Typography variant="caption">
          {new Date(history[history.length - 1]!.startsAt).toLocaleDateString(i18n.language, {
            dateStyle: 'medium',
          })}
        </Typography>
      </Stack>
    </Box>
  );
};
