import { type ContestStatus, type ContestType } from '../../api/types';

/**
 * Literal keys rather than an interpolated `contest.${...}`, so a renamed or
 * missing translation is a compile error instead of a string that renders as
 * itself. Mirrors the approach in `components/interview/interview.ts`.
 */
export const CONTEST_TYPE_LABEL_KEY = {
  WEEKLY: 'contest.type.weekly',
  BIWEEKLY: 'contest.type.biweekly',
  SPECIAL: 'contest.type.special',
} as const satisfies Record<ContestType, string>;

export const CONTEST_STATUS_LABEL_KEY = {
  DRAFT: 'contest.status.draft',
  SCHEDULED: 'contest.status.scheduled',
  RUNNING: 'contest.status.running',
  ENDED: 'contest.status.ended',
  FINALIZED: 'contest.status.finalized',
} as const satisfies Record<ContestStatus, string>;

/**
 * Palette token suffix for `palette.verdict.*`.
 *
 * <p>Reuses the judge's colours rather than introducing a second scale: green
 * already reads as "live and going well" everywhere else in the app.
 */
export const CONTEST_STATUS_TOKEN = {
  DRAFT: 'pending',
  SCHEDULED: 'compileError',
  RUNNING: 'accepted',
  ENDED: 'timeLimit',
  FINALIZED: 'pending',
} as const satisfies Record<ContestStatus, string>;

/** `mm:ss`, or `h:mm:ss` past an hour — the same clock format the interview uses. */
export const formatClock = (totalSeconds: number): string => {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  const pad = (value: number) => value.toString().padStart(2, '0');

  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`;
};

/**
 * A duration with days, for a countdown to a contest that is still a week away.
 *
 * <p>`4d 06:12:33` rather than `102:12:33`: nobody reads a hundred hours as
 * "next Sunday", which is the only thing the number is being asked.
 */
export const formatCountdown = (totalSeconds: number): string => {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const days = Math.floor(seconds / 86_400);

  return days > 0 ? `${days}d ${formatClock(seconds % 86_400)}` : formatClock(seconds);
};

/**
 * A finish time as it appears on the standings: `1:12:33` into the contest.
 *
 * <p>Always with hours, because a contest runs for ninety minutes and a bare
 * `12:33` would be read as twelve minutes when it might be an hour past that.
 */
export const formatContestTime = (totalSeconds: number): string => {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const pad = (value: number) => value.toString().padStart(2, '0');

  return `${Math.floor(seconds / 3600)}:${pad(Math.floor((seconds % 3600) / 60))}:${pad(seconds % 60)}`;
};

/** A rating for display: whole points, because the fractions are bookkeeping. */
export const formatRating = (rating: number): string => Math.round(rating).toString();

/** A delta with its sign, which is the whole point of showing it. */
export const formatDelta = (delta: number): string => {
  const rounded = Math.round(delta);
  return rounded > 0 ? `+${rounded}` : rounded.toString();
};

/** How urgent the clock looks — the same thresholds the mock interview uses. */
export const clockTone = (remainingSeconds: number): 'normal' | 'warning' | 'critical' =>
  remainingSeconds <= 60 ? 'critical' : remainingSeconds <= 300 ? 'warning' : 'normal';
