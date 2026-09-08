import { apiClient } from './client';
import { type ActivityCalendar, type LeaderboardRow, type PageResponse, type PublicProfile } from './types';

/**
 * Public profiles and the two global tables.
 *
 * <p>Keyed by username rather than id: a profile URL is something people type
 * and share, and `/u/ada` is the only form of it worth having.
 */
export const profilesApi = {
  async get(username: string, year?: number): Promise<PublicProfile> {
    const { data } = await apiClient.get<PublicProfile>(`/api/profiles/${username}`, {
      params: year === undefined ? undefined : { year },
    });
    return data;
  },

  /**
   * One window of the activity calendar.
   *
   * <p>Its own call so changing the window re-fetches a grid of squares rather
   * than the whole profile — the rating graph and the contest history do not
   * change when you look at 2024.
   *
   * @param year omit for the rolling twelve months
   */
  async calendar(username: string, year?: number): Promise<ActivityCalendar> {
    const { data } = await apiClient.get<ActivityCalendar>(`/api/profiles/${username}/calendar`, {
      params: year === undefined ? undefined : { year },
    });
    return data;
  },

  /** The rating table — only accounts that have actually competed appear. */
  async ratingLeaderboard(page = 0, size = 25): Promise<PageResponse<LeaderboardRow>> {
    const { data } = await apiClient.get<PageResponse<LeaderboardRow>>('/api/leaderboard/rating', {
      params: { page, size },
    });
    return data;
  },

  /** The solved table, weighted by difficulty so hard problems count for more. */
  async solvedLeaderboard(page = 0, size = 25): Promise<PageResponse<LeaderboardRow>> {
    const { data } = await apiClient.get<PageResponse<LeaderboardRow>>('/api/leaderboard/solved', {
      params: { page, size },
    });
    return data;
  },
};
