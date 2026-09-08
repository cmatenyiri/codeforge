import { apiClient } from './client';
import { type AdminContestDetail, type AdminContestSummary, type ContestUpsertPayload, type PageResponse } from './types';

/**
 * Contest authoring.
 *
 * <p>The bottom three are the interesting ones, and they exist because a
 * contest is the one thing here that cannot simply be edited afterwards: once it
 * has started its questions are frozen, and once it has been rated the result is
 * inside everybody's rating. So repairing one is not an edit but a deliberate
 * act — settle it, re-run it, or withdraw it.
 */
export const adminContestsApi = {
  async list(search: string | undefined, page = 0, size = 20): Promise<PageResponse<AdminContestSummary>> {
    const { data } = await apiClient.get<PageResponse<AdminContestSummary>>('/api/admin/contests', {
      params: { search: search === '' ? undefined : search, page, size },
    });
    return data;
  },

  async get(id: number): Promise<AdminContestDetail> {
    const { data } = await apiClient.get<AdminContestDetail>(`/api/admin/contests/${id}`);
    return data;
  },

  async create(payload: ContestUpsertPayload): Promise<AdminContestDetail> {
    const { data } = await apiClient.post<AdminContestDetail>('/api/admin/contests', payload);
    return data;
  },

  async update(id: number, payload: ContestUpsertPayload): Promise<AdminContestDetail> {
    const { data } = await apiClient.put<AdminContestDetail>(`/api/admin/contests/${id}`, payload);
    return data;
  },

  async setPublished(id: number, value: boolean): Promise<AdminContestDetail> {
    const { data } = await apiClient.patch<AdminContestDetail>(`/api/admin/contests/${id}/published`, {
      value,
    });
    return data;
  },

  async remove(id: number): Promise<void> {
    await apiClient.delete(`/api/admin/contests/${id}`);
  },

  /** Freezes the places and applies the ratings. A decision, not a timer. */
  async finalize(id: number): Promise<AdminContestDetail> {
    const { data } = await apiClient.post<AdminContestDetail>(`/api/admin/contests/${id}/finalize`);
    return data;
  },

  /**
   * Withdraws the contest's effect on everybody's rating, or restores it.
   *
   * <p>Both directions replay the rating ledger from this contest forward: a
   * rating is a running total, so every contest since was computed on top of it.
   */
  async setRated(id: number, rated: boolean, reason?: string): Promise<AdminContestDetail> {
    const { data } = await apiClient.patch<AdminContestDetail>(`/api/admin/contests/${id}/rated`, {
      rated,
      reason,
    });
    return data;
  },

  /**
   * Re-runs every submission against the corrected problems.
   *
   * <p>Returns immediately with the contest marked as rejudging; the work
   * happens on a background worker and can take minutes. Poll `get` for progress.
   */
  async rejudge(id: number): Promise<AdminContestDetail> {
    const { data } = await apiClient.post<AdminContestDetail>(`/api/admin/contests/${id}/rejudge`);
    return data;
  },
};
