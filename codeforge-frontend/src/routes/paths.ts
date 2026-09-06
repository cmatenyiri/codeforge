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
  interviews: '/interview',
  interviewSession: '/interview/:id',
  interviewReport: '/interview/:id/report',
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

/** The authoring form for one problem. */
export const adminProblemEditPath = (id: number) => `${paths.adminProblems}/${id}/edit`;
