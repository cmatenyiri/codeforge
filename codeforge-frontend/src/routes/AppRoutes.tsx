import { Box, CircularProgress } from '@mui/material';
import { Suspense } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router';
import { useAuth } from '../auth/use-auth';
import { paths } from './paths';
import { routes, type RouteAccess } from './route-config';
import { useRouteDocumentTitle } from './use-document-title';

/** Shown while a lazily-loaded page is being fetched, and while the session is resolving. */
const FullPageSpinner = () => (
  <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', backgroundColor: 'surface.canvas' }}>
    <CircularProgress />
  </Box>
);

/**
 * Applies the route's access rule.
 *
 * <p>While the session cookie is still being checked there is no answer yet, so
 * guarded routes wait rather than guess — redirecting during that window would
 * bounce a signed-in user to the login page on every refresh.
 */
const RouteGuard = ({ access, children }: { access: RouteAccess; children: React.ReactNode }) => {
  const { isAuthenticated, initialising } = useAuth();
  const location = useLocation();

  if (access === 'public') {
    return <>{children}</>;
  }

  if (initialising) {
    return <FullPageSpinner />;
  }

  if (access === 'authenticated' && !isAuthenticated) {
    // Remember the destination so login can send them back to it.
    return <Navigate to={paths.login} replace state={{ from: location.pathname }} />;
  }

  if (access === 'guestOnly' && isAuthenticated) {
    return <Navigate to={paths.home} replace />;
  }

  return <>{children}</>;
};

/** The whole route table, rendered from data. */
export const AppRoutes = () => {
  // Outside the Suspense boundary on purpose — see useRouteDocumentTitle.
  useRouteDocumentTitle();

  return (
    <Suspense fallback={<FullPageSpinner />}>
      <Routes>
        {routes.map((route) => {
          const Page = route.component;
          return (
            <Route
              key={route.path}
              path={route.path}
              element={
                <RouteGuard access={route.access}>
                  <Page />
                </RouteGuard>
              }
            />
          );
        })}
        <Route path="*" element={<Navigate to={paths.home} replace />} />
      </Routes>
    </Suspense>
  );
};
