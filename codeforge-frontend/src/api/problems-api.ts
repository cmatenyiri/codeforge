import { apiClient } from './client';
import { type PageResponse, type ProblemDetail, type ProblemSummary, type Tag } from './types';

export type ProblemQuery = {
  search?: string;
  difficulty?: string;
  tag?: string;
  page?: number;
  size?: number;
};

export const problemsApi = {
  async list(query: ProblemQuery): Promise<PageResponse<ProblemSummary>> {
    const { data } = await apiClient.get<PageResponse<ProblemSummary>>('/api/problems', {
      // Blank filters are dropped rather than sent empty, so the URL stays readable.
      params: Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== '')),
    });
    return data;
  },

  async getBySlug(slug: string): Promise<ProblemDetail> {
    const { data } = await apiClient.get<ProblemDetail>(`/api/problems/${slug}`);
    return data;
  },

  async tags(): Promise<Tag[]> {
    const { data } = await apiClient.get<Tag[]>('/api/tags');
    return data;
  },
};
