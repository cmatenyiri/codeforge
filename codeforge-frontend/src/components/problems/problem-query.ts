import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router';
import { type Difficulty, type ProblemSort, type ProblemStatusFilter, type SortOrder } from '../../api/types';

/** Everything that narrows or orders the catalogue, in one shape. */
export type ProblemQueryState = {
  search: string;
  difficulty: '' | Difficulty;
  tag: string;
  status: '' | ProblemStatusFilter;
  sort: ProblemSort;
  order: SortOrder;
  /** 1-based, matching what the pagination control shows. */
  page: number;
  size: number;
};

export const PAGE_SIZES = [20, 50, 100] as const;

const DEFAULTS: ProblemQueryState = {
  search: '',
  difficulty: '',
  tag: '',
  status: '',
  sort: 'id',
  order: 'asc',
  page: 1,
  size: 20,
};

const SORTS: ProblemSort[] = ['id', 'title', 'difficulty', 'acceptance'];

const asSort = (value: string | null): ProblemSort =>
  SORTS.includes(value as ProblemSort) ? (value as ProblemSort) : DEFAULTS.sort;

const asPositiveInt = (value: string | null, fallback: number): number => {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

/**
 * Reads the catalogue's state out of the URL and writes changes back into it.
 *
 * <p>The URL is the state, rather than a copy of it kept in sync: a filtered,
 * sorted, paged view is then shareable, survives a refresh, and works with the
 * browser's back button — none of which is true of a `useState` that happens to
 * be mirrored into the address bar.
 *
 * <p>Only values that differ from the default are written, so the common case
 * stays a bare `/problems` rather than a URL restating every default.
 */
export const useProblemQuery = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const query = useMemo<ProblemQueryState>(
    () => ({
      search: searchParams.get('search') ?? DEFAULTS.search,
      difficulty: (searchParams.get('difficulty') ?? DEFAULTS.difficulty) as ProblemQueryState['difficulty'],
      tag: searchParams.get('tag') ?? DEFAULTS.tag,
      status: (searchParams.get('status') ?? DEFAULTS.status) as ProblemQueryState['status'],
      sort: asSort(searchParams.get('sort')),
      order: searchParams.get('order') === 'desc' ? 'desc' : 'asc',
      page: asPositiveInt(searchParams.get('page'), DEFAULTS.page),
      size: PAGE_SIZES.includes(asPositiveInt(searchParams.get('size'), DEFAULTS.size) as (typeof PAGE_SIZES)[number])
        ? asPositiveInt(searchParams.get('size'), DEFAULTS.size)
        : DEFAULTS.size,
    }),
    [searchParams],
  );

  /**
   * Applies a change.
   *
   * <p>Anything but an explicit page change resets to page 1: staying on page 4
   * of a list that has just been filtered down to two pages shows an empty
   * table, which reads as "no results" rather than "wrong page".
   */
  const update = useCallback(
    (patch: Partial<ProblemQueryState>) => {
      const next: ProblemQueryState = { ...query, ...patch, page: patch.page ?? 1 };
      const params = new URLSearchParams();

      for (const [key, value] of Object.entries(next)) {
        if (String(value) !== String(DEFAULTS[key as keyof ProblemQueryState])) {
          params.set(key, String(value));
        }
      }

      // Typing in the search box replaces history rather than stacking an entry
      // per keystroke; a deliberate page or sort change is worth going back to.
      setSearchParams(params, { replace: patch.search !== undefined });
    },
    [query, setSearchParams],
  );

  const reset = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true });
  }, [setSearchParams]);

  const filtered = query.search !== '' || query.difficulty !== '' || query.tag !== '' || query.status !== '';

  return { query, update, reset, filtered };
};
