import { apiClient } from './client';
import { type PageResponse, type SubmissionDetail, type SubmissionSummary } from './types';

/**
 * The signed-in user's own submission history.
 *
 * <p>No endpoint here takes a user id: the backend scopes every query to the
 * caller, so there is nothing to get wrong about whose code is being read.
 */
export const submissionsApi = {
  async listMine(page = 0, size = 20): Promise<PageResponse<SubmissionSummary>> {
    const { data } = await apiClient.get<PageResponse<SubmissionSummary>>('/api/submissions', {
      params: { page, size },
    });
    return data;
  },

  async listForProblem(slug: string, page = 0, size = 20): Promise<PageResponse<SubmissionSummary>> {
    const { data } = await apiClient.get<PageResponse<SubmissionSummary>>(`/api/problems/${slug}/submissions`, {
      params: { page, size },
    });
    return data;
  },

  async get(id: number): Promise<SubmissionDetail> {
    const { data } = await apiClient.get<SubmissionDetail>(`/api/submissions/${id}`);
    return data;
  },
};
