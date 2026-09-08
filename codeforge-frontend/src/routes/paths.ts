/**
 * Every route path in the application, in one place.
 *
 * <p>Navigation and route declarations both read from here, so a path is
 * written once and a rename is a compile error at every call site rather than a
 * dead link discovered by clicking.
 */
export const paths = {
  home: '/',
  login: '/login',
  register: '/register',
  profile: '/profile',
  problems: '/problems',
  solve: '/problems/:slug',
  contests: '/contest',
  contest: '/contest/:slug',
  contestProblem: '/contest/:slug/problems/:position',
  contestRanking: '/contest/:slug/ranking',
  leaderboard: '/ranking',
  publicProfile: '/u/:username',
  interviews: '/interview',
  interviewSession: '/interview/:id',
  interviewReport: '/interview/:id/report',
  adminContests: '/admin/contests',
  adminContestNew: '/admin/contests/new',
  adminContestEdit: '/admin/contests/:id/edit',
  adminDaily: '/admin/daily',
  adminProblems: '/admin/problems',
  adminProblemNew: '/admin/problems/new',
  adminProblemEdit: '/admin/problems/:id/edit',
  /** Design-system reference; delete along with `src/components/preview`. */
  themePreview: '/theme-preview',
} as const;

export type AppPath = (typeof paths)[keyof typeof paths];

/** Builds the solving-page URL for a problem. */
export const problemPath = (slug: string) => `${paths.problems}/${slug}`;

export const interviewSessionPath = (id: number) => `${paths.interviews}/${id}`;

export const interviewReportPath = (id: number) => `${paths.interviews}/${id}/report`;

/** A contest's own page, keyed by slug — the URL people share. */
export const contestPath = (slug: string) => `${paths.contests}/${slug}`;

/**
 * One question inside a contest, addressed by its position rather than its slug.
 *
 * <p>The slug would be the obvious choice and is the wrong one: before a contest
 * starts nobody is told it, and an author renaming the problem afterwards would
 * break every link into a contest that has already been sat. A question's place
 * in the contest is the one identifier that cannot move.
 */
export const contestProblemPath = (slug: string, position: number) =>
  `${paths.contests}/${slug}/problems/${position}`;

export const contestRankingPath = (slug: string) => `${paths.contests}/${slug}/ranking`;

/** Somebody's public profile. */
export const profilePath = (username: string) => `/u/${username}`;

/** The authoring form for one contest. */
export const adminContestEditPath = (id: number) => `${paths.adminContests}/${id}/edit`;

/** The authoring form for one problem. */
export const adminProblemEditPath = (id: number) => `${paths.adminProblems}/${id}/edit`;
