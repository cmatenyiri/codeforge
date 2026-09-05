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
  /** Design-system reference; delete along with `src/components/preview`. */
  themePreview: '/theme-preview',
} as const;

export type AppPath = (typeof paths)[keyof typeof paths];

/** Builds the solving-page URL for a problem. */
export const problemPath = (slug: string) => `${paths.problems}/${slug}`;
