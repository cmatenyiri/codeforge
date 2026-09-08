import { apiClient } from './client';
import { type DailyCalendarMonth, type DailyChallenge } from './types';

/**
 * The problem of the day.
 *
 * <p>One problem, the same one for everybody, turning over at midnight UTC.
 * Solving it on the day extends a streak; solving it later still solves the
 * problem but does not extend anything — which is the whole of what makes a
 * streak worth keeping.
 */
export const dailyApi = {
  async today(): Promise<DailyChallenge> {
    const { data } = await apiClient.get<DailyChallenge>('/api/daily');
    return data;
  },

  /** A month of the picker. Future days come back without a problem. */
  async calendar(year: number, month: number): Promise<DailyCalendarMonth> {
    const { data } = await apiClient.get<DailyCalendarMonth>('/api/daily/calendar', {
      params: { year, month },
    });
    return data;
  },

  /** Pins a problem to a future date, taking it out of the rotation. Admin only. */
  async pin(date: string, problemId: number): Promise<void> {
    await apiClient.put(`/api/admin/daily/${date}`, { problemId });
  },

  async unpin(date: string): Promise<void> {
    await apiClient.delete(`/api/admin/daily/${date}`);
  },
};
