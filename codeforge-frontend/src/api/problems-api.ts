import { apiClient } from './client';
import {
  type Editorial,
  type PageResponse,
  type ProblemDetail,
  type ProblemSort,
  type ProblemStatusFilter,
  type ProblemSummary,
  type SortOrder,
  type Tag,
} from './types';

export type ProblemQuery = {
  search?: string;
  difficulty?: string;
  tag?: string;
  status?: ProblemStatusFilter | '';
  sort?: ProblemSort;
  order?: SortOrder;
  page?: number;
  size?: number;
};

/** Blank filters are dropped rather than sent empty, so the URL stays readable. */
const params = (query: ProblemQuery) =>
  Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== ''));

export const problemsApi = {
  async list(query: ProblemQuery): Promise<PageResponse<ProblemSummary>> {
    const { data } = await apiClient.get<PageResponse<ProblemSummary>>('/api/problems', {
      params: params(query),
    });
    return data;
  },

  /**
   * One problem at random from those matching the filters.
   *
   * <p>Honours the filters on purpose: "surprise me, but only among the medium
   * graph problems I have not solved" is the useful version of this button.
   */
  async random(query: ProblemQuery): Promise<ProblemSummary> {
    const { search, difficulty, tag, status } = query;
    const { data } = await apiClient.get<ProblemSummary>('/api/problems/random', {
      params: params({ search, difficulty, tag, status }),
    });
    return data;
  },

  async getBySlug(slug: string): Promise<ProblemDetail> {
    const { data } = await apiClient.get<ProblemDetail>(`/api/problems/${slug}`);
    return data;
  },

  /**
   * The problem's written solution.
   *
   * <p>Its own call rather than part of the detail response: it is the largest
   * thing a problem owns and most visits never open the tab.
   */
  async editorial(slug: string): Promise<Editorial> {
    const { data } = await apiClient.get<Editorial>(`/api/problems/${slug}/editorial`);
    return data;
  },

  async tags(): Promise<Tag[]> {
    const { data } = await apiClient.get<Tag[]>('/api/tags');
    return data;
  },
};
