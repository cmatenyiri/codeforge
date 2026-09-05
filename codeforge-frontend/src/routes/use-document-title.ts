import { useLayoutEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { matchPath, useLocation } from 'react-router';
import { routes } from './route-config';

/** Separates the page name from the product name in the tab title. */
const SEPARATOR = ' · ';

/**
 * Keeps the tab title in step with the current location.
 *
 * <p>Resolved from the location rather than from inside the rendered page, and
 * called from outside the {@code Suspense} boundary, so the title changes the
 * moment navigation happens rather than when the page's lazy chunk finishes
 * downloading. That ordering matters in production: the title is navigation
 * feedback, it is what screen readers announce on a route change, and it is what
 * gets recorded in browser history — none of which should wait on a network
 * round trip. The cost is that a route which immediately redirects titles the
 * tab for one tick before the redirect settles it, which is the cheaper trade.
 *
 * <p>Titles resolve through i18n, and because `t` changes identity on a language
 * change, switching language retitles the open tab.
 */
export const useRouteDocumentTitle = () => {
  const { t } = useTranslation();
  const location = useLocation();

  const titleKey = routes.find((route) => matchPath(route.path, location.pathname))?.titleKey;

  // Layout effect, not a passive one: the title is committed before paint, so the
  // tab never shows the previous page's name — not even for a frame.
  useLayoutEffect(() => {
    document.title = titleKey ? `${t(titleKey)}${SEPARATOR}${t('common.appName')}` : t('common.appName');
  }, [t, titleKey]);
};
