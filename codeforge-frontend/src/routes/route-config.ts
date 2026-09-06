import { type ParseKeys } from 'i18next';
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';
import { paths } from './paths';

/** Who may see a route. Enforced centrally by the router, not by each page. */
export type RouteAccess =
  /** Anyone, signed in or not. */
  | 'public'
  /** Signed-in callers only; others are sent to the login page. */
  | 'authenticated'
  /** Signed-out callers only — login and register, which a signed-in user should skip. */
  | 'guestOnly'
  /** Administrators only. The server enforces this too; the guard only spares everyone else a wall of 403s. */
  | 'admin';

export type AppRoute = {
  path: string;
  component: LazyExoticComponent<ComponentType>;
  /** `pageTitle.*` key used for the browser tab. */
  titleKey: ParseKeys;
  access: RouteAccess;
};

/**
 * The route table.
 *
 * <p>Every page is code-split: the components are only fetched when their route
 * is first visited, so the initial download does not carry screens the user may
 * never open. Pages use named exports, hence the small `.then` remap that
 * `lazy` needs.
 *
 * <p>Adding a page means adding one entry here — path, component, tab title and
 * who may see it — with nothing to remember to wire up elsewhere.
 */
export const routes: AppRoute[] = [
  {
    path: paths.login,
    component: lazy(() => import('../pages/Login').then((module) => ({ default: module.LoginPage }))),
    titleKey: 'pageTitle.login',
    access: 'guestOnly',
  },
  {
    path: paths.register,
    component: lazy(() => import('../pages/Register').then((module) => ({ default: module.RegisterPage }))),
    titleKey: 'pageTitle.register',
    access: 'guestOnly',
  },
  {
    path: paths.home,
    component: lazy(() => import('../pages/Home').then((module) => ({ default: module.HomePage }))),
    titleKey: 'pageTitle.home',
    access: 'authenticated',
  },
  {
    path: paths.profile,
    component: lazy(() => import('../pages/Profile').then((module) => ({ default: module.ProfilePage }))),
    titleKey: 'pageTitle.profile',
    access: 'authenticated',
  },
  {
    path: paths.problems,
    component: lazy(() => import('../pages/Problems').then((module) => ({ default: module.ProblemsPage }))),
    titleKey: 'pageTitle.problems',
    access: 'authenticated',
  },
  {
    path: paths.solve,
    component: lazy(() => import('../pages/Solve').then((module) => ({ default: module.SolvePage }))),
    titleKey: 'pageTitle.solve',
    access: 'authenticated',
  },
  {
    path: paths.interviews,
    component: lazy(() => import('../pages/Interview').then((module) => ({ default: module.InterviewPage }))),
    titleKey: 'pageTitle.interviews',
    access: 'authenticated',
  },
  {
    // Declared before the report route only for readability — the router matches
    // on specificity, not on order.
    path: paths.interviewSession,
    component: lazy(() =>
      import('../pages/InterviewSession').then((module) => ({ default: module.InterviewSessionPage })),
    ),
    titleKey: 'pageTitle.interviewSession',
    access: 'authenticated',
  },
  {
    path: paths.interviewReport,
    component: lazy(() =>
      import('../pages/InterviewReport').then((module) => ({ default: module.InterviewReportPage })),
    ),
    titleKey: 'pageTitle.interviewReport',
    access: 'authenticated',
  },
  {
    // Declared before the edit route only for readability — the router matches
    // on specificity, and "/admin/problems/new" is more specific than the
    // template it would otherwise fall into.
    path: paths.adminProblems,
    component: lazy(() => import('../pages/AdminProblems').then((module) => ({ default: module.AdminProblemsPage }))),
    titleKey: 'pageTitle.adminProblems',
    access: 'admin',
  },
  {
    path: paths.adminProblemNew,
    component: lazy(() =>
      import('../pages/AdminProblemEditor').then((module) => ({ default: module.AdminProblemEditorPage })),
    ),
    titleKey: 'pageTitle.adminProblemNew',
    access: 'admin',
  },
  {
    path: paths.adminProblemEdit,
    component: lazy(() =>
      import('../pages/AdminProblemEditor').then((module) => ({ default: module.AdminProblemEditorPage })),
    ),
    titleKey: 'pageTitle.adminProblemEdit',
    access: 'admin',
  },
  {
    path: paths.themePreview,
    component: lazy(() => import('../pages/ThemePreview').then((module) => ({ default: module.ThemePreviewPage }))),
    titleKey: 'pageTitle.themePreview',
    access: 'public',
  },
];
