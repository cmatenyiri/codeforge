/**
 * What the server pushes, and where.
 *
 * <p>Mirrors `com.codeforge.realtime`. Every message only says that something
 * changed; the screen re-reads what it shows through the ordinary API, which is
 * where every visibility rule lives.
 */

/** Mirrors `ContestChanged.Change`. */
export type ContestChange = 'STATUS' | 'REGISTRATION' | 'STANDINGS' | 'REJUDGE';

export type ContestChanged = { contestId: number; change: ContestChange };

export type InterviewChanged = { interviewId: number };

export const topics = {
  /** Every announced contest's status and registrations — the lobby. */
  contests: '/topic/contests',
  /** One contest: status, registrations, standings. */
  contest: (contestId: number) => `/topic/contests.${contestId}`,
  /** A rejudge's progress. Authors only. */
  adminContest: (contestId: number) => `/topic/admin.contests.${contestId}`,
  /** One mock interview, for its owner's other tabs. */
  interview: (interviewId: number) => `/topic/interviews.${interviewId}`,
};
