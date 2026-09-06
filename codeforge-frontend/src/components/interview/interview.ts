import { type InterviewFormat, type InterviewInsight, type InterviewOutcome } from '../../api/types';

/**
 * Literal keys rather than an interpolated `interview.${...}`, so a renamed or
 * missing translation is a compile error instead of a string that renders as
 * itself. Mirrors the approach in `components/problems/difficulty.ts`.
 */
export const FORMAT_LABEL_KEY = {
  WARM_UP: 'interview.format.warmUp',
  STANDARD: 'interview.format.standard',
  HARD: 'interview.format.hard',
} as const satisfies Record<InterviewFormat, string>;

export const FORMAT_BLURB_KEY = {
  WARM_UP: 'interview.formatBlurb.warmUp',
  STANDARD: 'interview.formatBlurb.standard',
  HARD: 'interview.formatBlurb.hard',
} as const satisfies Record<InterviewFormat, string>;

export const OUTCOME_LABEL_KEY = {
  NO_SOLVE: 'interview.outcome.noSolve',
  PARTIAL: 'interview.outcome.partial',
  SOLID: 'interview.outcome.solid',
  STRONG: 'interview.outcome.strong',
} as const satisfies Record<InterviewOutcome, string>;

export const OUTCOME_BLURB_KEY = {
  NO_SOLVE: 'interview.outcomeBlurb.noSolve',
  PARTIAL: 'interview.outcomeBlurb.partial',
  SOLID: 'interview.outcomeBlurb.solid',
  STRONG: 'interview.outcomeBlurb.strong',
} as const satisfies Record<InterviewOutcome, string>;

/**
 * Palette token suffix for `palette.verdict.*`.
 *
 * <p>Reuses the judge's colours rather than introducing a second scale: a
 * candidate already reads green as "that went in" everywhere else in the app,
 * and a band is the same kind of statement about the same kind of thing.
 */
export const OUTCOME_TOKEN = {
  NO_SOLVE: 'wrongAnswer',
  PARTIAL: 'timeLimit',
  SOLID: 'accepted',
  STRONG: 'accepted',
} as const satisfies Record<InterviewOutcome, string>;

export const INSIGHT_LABEL_KEY = {
  NO_SUBMISSION: 'interview.insight.noSubmission',
  ALL_SOLVED: 'interview.insight.allSolved',
  CLEAN_RUN: 'interview.insight.cleanRun',
  FINISHED_EARLY: 'interview.insight.finishedEarly',
  RAN_OUT_OF_TIME: 'interview.insight.ranOutOfTime',
  SLOW_WARM_UP: 'interview.insight.slowWarmUp',
  HINTS_USED: 'interview.insight.hintsUsed',
  MANY_ATTEMPTS: 'interview.insight.manyAttempts',
  SKIPPED_PROBLEM: 'interview.insight.skippedProblem',
} as const satisfies Record<InterviewInsight, string>;

/**
 * Whether an insight is something to fix or something to be pleased about.
 *
 * <p>Only affects the icon and its colour. A debrief that painted every line
 * red would be read as a telling-off rather than as notes.
 */
export const INSIGHT_IS_PRAISE = {
  NO_SUBMISSION: false,
  ALL_SOLVED: true,
  CLEAN_RUN: true,
  FINISHED_EARLY: true,
  RAN_OUT_OF_TIME: false,
  SLOW_WARM_UP: false,
  HINTS_USED: false,
  MANY_ATTEMPTS: false,
  SKIPPED_PROBLEM: false,
} as const satisfies Record<InterviewInsight, boolean>;

/** `mm:ss`, or `h:mm:ss` past an hour. The countdown's format, and the report's. */
export const formatClock = (totalSeconds: number): string => {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  const pad = (value: number) => value.toString().padStart(2, '0');

  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`;
};

/**
 * How urgent the clock looks.
 *
 * <p>Five minutes is the point in a real round where an interviewer starts
 * steering you towards something that compiles, and one minute is the point
 * where the countdown should be impossible to ignore.
 */
export const clockTone = (remainingSeconds: number): 'normal' | 'warning' | 'critical' =>
  remainingSeconds <= 60 ? 'critical' : remainingSeconds <= 300 ? 'warning' : 'normal';
