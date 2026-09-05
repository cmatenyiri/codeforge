import { type ExecutionStatus } from '../../api/types';

/**
 * Literal keys rather than an interpolated `verdict.${...}`, so a renamed or
 * missing translation is a compile error instead of a string that renders as
 * itself. Mirrors the approach in `components/problems/difficulty.ts`.
 */
export const VERDICT_LABEL_KEY = {
  PENDING: 'verdict.pending',
  RUNNING: 'verdict.running',
  ACCEPTED: 'verdict.accepted',
  WRONG_ANSWER: 'verdict.wrongAnswer',
  TIME_LIMIT_EXCEEDED: 'verdict.timeLimitExceeded',
  MEMORY_LIMIT_EXCEEDED: 'verdict.memoryLimitExceeded',
  RUNTIME_ERROR: 'verdict.runtimeError',
  COMPILE_ERROR: 'verdict.compileError',
  INTERNAL_ERROR: 'verdict.internalError',
} as const satisfies Record<ExecutionStatus, string>;

/**
 * Palette token suffix for `palette.verdict.*`.
 *
 * <p>The palette has fewer colours than there are statuses, on purpose: a memory
 * limit reads as a resource failure like a time limit, and an internal error is
 * not something a solver can distinguish from a crash.
 */
export const VERDICT_TOKEN = {
  PENDING: 'pending',
  RUNNING: 'pending',
  ACCEPTED: 'accepted',
  WRONG_ANSWER: 'wrongAnswer',
  TIME_LIMIT_EXCEEDED: 'timeLimit',
  MEMORY_LIMIT_EXCEEDED: 'timeLimit',
  RUNTIME_ERROR: 'runtimeError',
  COMPILE_ERROR: 'compileError',
  INTERNAL_ERROR: 'runtimeError',
} as const satisfies Record<ExecutionStatus, string>;

/** Language names are proper nouns, so they are not translated. */
export const LANGUAGE_LABEL = {
  JAVA: 'Java',
  PYTHON: 'Python',
  JAVASCRIPT: 'JavaScript',
  TYPESCRIPT: 'TypeScript',
} as const;
