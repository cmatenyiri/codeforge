import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { type AdminProblemSort } from '../../api/admin-api';
import { type Difficulty, type ProblemState, type SortOrder } from '../../api/types';

/** Everything that narrows or orders the authoring catalogue. */
export type AdminQueryState = {
  search: string;
  difficulty: '' | Difficulty;
  tag: string;
  state: '' | ProblemState;
  sort: AdminProblemSort;
  order: SortOrder;
  /** 1-based, matching what the pagination control shows. */
  page: number;
  size: number;
};

export const PAGE_SIZES = [20, 50, 100] as const;

/**
 * Newest edit first, unlike the solver's catalogue.
 *
 * <p>An author almost always wants the problem they were last working on, not
 * the first one ever written.
 */
const DEFAULTS: AdminQueryState = {
  search: '',
  difficulty: '',
  tag: '',
  state: '',
  sort: 'updated',
  order: 'desc',
  page: 1,
  size: 20,
};

const SORTS: AdminProblemSort[] = ['id', 'title', 'difficulty', 'updated'];

const asSort = (value: string | null): AdminProblemSort =>
  SORTS.includes(value as AdminProblemSort) ? (value as AdminProblemSort) : DEFAULTS.sort;

const asPositiveInt = (value: string | null, fallback: number): number => {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

/**
 * Reads the authoring catalogue's state out of the URL and writes it back.
 *
 * <p>The same reasoning as the solver's catalogue: a filtered view survives a
 * refresh, works with the back button after an edit, and can be sent to whoever
 * else is writing problems.
 */
export const useAdminProblemQuery = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const query = useMemo<AdminQueryState>(() => {
    const size = asPositiveInt(searchParams.get('size'), DEFAULTS.size);

    return {
      search: searchParams.get('search') ?? DEFAULTS.search,
      difficulty: (searchParams.get('difficulty') ?? DEFAULTS.difficulty) as AdminQueryState['difficulty'],
      tag: searchParams.get('tag') ?? DEFAULTS.tag,
      state: (searchParams.get('state') ?? DEFAULTS.state) as AdminQueryState['state'],
      sort: asSort(searchParams.get('sort')),
      order: searchParams.get('order') === 'asc' ? 'asc' : 'desc',
      page: asPositiveInt(searchParams.get('page'), DEFAULTS.page),
      size: PAGE_SIZES.includes(size as (typeof PAGE_SIZES)[number]) ? size : DEFAULTS.size,
    };
  }, [searchParams]);

  const update = useCallback(
    (patch: Partial<AdminQueryState>) => {
      const next: AdminQueryState = { ...query, ...patch, page: patch.page ?? 1 };
      const params = new URLSearchParams();

      for (const [key, value] of Object.entries(next)) {
        if (String(value) !== String(DEFAULTS[key as keyof AdminQueryState])) {
          params.set(key, String(value));
        }
      }

      setSearchParams(params, { replace: patch.search !== undefined });
    },
    [query, setSearchParams],
  );

  const reset = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true });
  }, [setSearchParams]);

  const filtered = query.search !== '' || query.difficulty !== '' || query.tag !== '' || query.state !== '';

  return { query, update, reset, filtered };
};
