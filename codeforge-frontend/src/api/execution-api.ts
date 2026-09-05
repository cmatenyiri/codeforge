import { apiClient } from './client';
import { type RunPayload, type RunResult } from './types';

export const executionApi = {
  /**
   * Runs code against a problem's sample cases.
   *
   * <p>The default client timeout is far too short for this one call: the judge
   * has to compile and then run once per case, and a cold Java compile alone can
   * outlast it. Nothing is persisted — running is not submitting.
   */
  async run(slug: string, payload: RunPayload): Promise<RunResult> {
    const { data } = await apiClient.post<RunResult>(`/api/problems/${slug}/run`, payload, {
      timeout: 90_000,
    });
    return data;
  },
};
