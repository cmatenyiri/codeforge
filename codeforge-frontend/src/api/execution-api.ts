import { apiClient } from './client';
import { type RunPayload, type RunResult, type SubmissionResult, type SubmitPayload } from './types';

/**
 * The default client timeout is far too short for either of these: the judge has
 * to compile and then run once per case, and a cold Java compile alone can
 * outlast it.
 */
const RUN_TIMEOUT_MS = 90_000;

/**
 * A submission runs every case, hidden ones included, and some of those are
 * sized so that an inefficient solution burns the whole CPU limit before it is
 * killed. The client has to be willing to wait longer than the judge will.
 */
const SUBMIT_TIMEOUT_MS = 180_000;

export const executionApi = {
  /** Runs code against a problem's sample cases. Nothing is persisted. */
  async run(slug: string, payload: RunPayload): Promise<RunResult> {
    const { data } = await apiClient.post<RunResult>(`/api/problems/${slug}/run`, payload, {
      timeout: RUN_TIMEOUT_MS,
    });
    return data;
  },

  /**
   * Judges code against every case and records the verdict.
   *
   * <p>Only this marks a problem solved: passing the visible samples is not the
   * bar, and hidden cases come back carrying a verdict and nothing else.
   */
  async submit(slug: string, payload: SubmitPayload): Promise<SubmissionResult> {
    const { data } = await apiClient.post<SubmissionResult>(`/api/problems/${slug}/submit`, payload, {
      timeout: SUBMIT_TIMEOUT_MS,
    });
    return data;
  },
};
