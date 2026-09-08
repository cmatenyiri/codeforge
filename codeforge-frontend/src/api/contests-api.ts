import { apiClient } from './client';
import {
  type ContestDetail,
  type ContestProblemDetail,
  type ContestRegistrationState,
  type ContestStandings,
  type ContestSummary,
  type PageResponse,
  type RunPayload,
  type RunResult,
  type SubmissionResult,
  type SubmissionSummary,
  type SubmitPayload,
} from './types';

/**
 * The same judging timeouts the practice editor uses: a cold Java compile alone
 * outlasts the client's default, and a submission runs every hidden case.
 */
const RUN_TIMEOUT_MS = 90_000;
const SUBMIT_TIMEOUT_MS = 180_000;

/**
 * Contests.
 *
 * <p>Note the shape of what is offered. There is no call that returns a problem
 * before the contest starts, none that edits a submission after it lands, and
 * none that reads anybody else's code. A contest is a measurement, and each of
 * those would be a way around it.
 */
export const contestsApi = {
  /** Announced contests, newest first. Past ones carry the caller's own result. */
  async list(page = 0, size = 20): Promise<PageResponse<ContestSummary>> {
    const { data } = await apiClient.get<PageResponse<ContestSummary>>('/api/contests', {
      params: { page, size },
    });
    return data;
  },

  /** What is running now, then what is next — one call, because the lobby wants both. */
  async upcoming(): Promise<ContestSummary[]> {
    const { data } = await apiClient.get<ContestSummary[]>('/api/contests/upcoming');
    return data;
  },

  /**
   * One contest's page.
   *
   * <p>Also the call that freezes the problems when the start time passes, so
   * everybody loading the page at 10:00:00 races to be the one that seals it and
   * exactly one of them wins.
   */
  async detail(slug: string): Promise<ContestDetail> {
    const { data } = await apiClient.get<ContestDetail>(`/api/contests/${slug}`);
    return data;
  },

  async register(slug: string): Promise<ContestRegistrationState> {
    const { data } = await apiClient.post<ContestRegistrationState>(`/api/contests/${slug}/register`);
    return data;
  },

  /** Refused once the contest is under way — an opt-out after seeing the result is not one. */
  async unregister(slug: string): Promise<ContestRegistrationState> {
    const { data } = await apiClient.delete<ContestRegistrationState>(`/api/contests/${slug}/register`);
    return data;
  },

  /** One question, from the frozen copy. A 409 before the contest starts. */
  async problem(slug: string, position: number): Promise<ContestProblemDetail> {
    const { data } = await apiClient.get<ContestProblemDetail>(
      `/api/contests/${slug}/problems/${position}`,
    );
    return data;
  },

  async run(slug: string, position: number, payload: RunPayload): Promise<RunResult> {
    const { data } = await apiClient.post<RunResult>(
      `/api/contests/${slug}/problems/${position}/run`,
      payload,
      { timeout: RUN_TIMEOUT_MS },
    );
    return data;
  },

  /**
   * Judges every case and scores it.
   *
   * <p>Whether it counts was decided server-side before the sandbox ran: a
   * submission sent with ten seconds left counts even though the verdict arrives
   * a minute after the buzzer.
   */
  async submit(slug: string, position: number, payload: SubmitPayload): Promise<SubmissionResult> {
    const { data } = await apiClient.post<SubmissionResult>(
      `/api/contests/${slug}/problems/${position}/submit`,
      payload,
      { timeout: SUBMIT_TIMEOUT_MS },
    );
    return data;
  },

  async submissions(
    slug: string,
    position: number,
    page = 0,
    size = 20,
  ): Promise<PageResponse<SubmissionSummary>> {
    const { data } = await apiClient.get<PageResponse<SubmissionSummary>>(
      `/api/contests/${slug}/problems/${position}/submissions`,
      { params: { page, size } },
    );
    return data;
  },

  /** The scoreboard. The caller's own row travels with every page. */
  async standings(slug: string, page = 0, size = 25): Promise<ContestStandings> {
    const { data } = await apiClient.get<ContestStandings>(`/api/contests/${slug}/standings`, {
      params: { page, size },
    });
    return data;
  },
};
