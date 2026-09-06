import { apiClient } from './client';
import {
  type InterviewFormat,
  type InterviewFormatOption,
  type InterviewHints,
  type InterviewProblemDetail,
  type InterviewReport,
  type InterviewSession,
  type InterviewSummary,
  type PageResponse,
  type RunPayload,
  type RunResult,
  type SubmissionResult,
  type SubmitPayload,
} from './types';

/**
 * The same judging timeouts the practice editor uses: a cold Java compile alone
 * outlasts the client's default, and a submission runs every hidden case.
 */
const RUN_TIMEOUT_MS = 90_000;
const SUBMIT_TIMEOUT_MS = 180_000;

/**
 * The mock interview.
 *
 * <p>Note the shape of what is offered. A caller may pick a format and nothing
 * else — not the problems, not the order, not the length — and there is no route
 * to an editorial anywhere in here. That is the difference between a mock and
 * practice with a timer on it.
 */
export const interviewsApi = {
  /** The formats on offer, with the shape of each. */
  async formats(): Promise<InterviewFormatOption[]> {
    const { data } = await apiClient.get<InterviewFormatOption[]>('/api/interviews/formats');
    return data;
  },

  /** Starts a round. 409 when one is already running. */
  async start(format: InterviewFormat): Promise<InterviewSession> {
    const { data } = await apiClient.post<InterviewSession>('/api/interviews', { format });
    return data;
  },

  /** The caller's running round, or 404. */
  async current(): Promise<InterviewSession> {
    const { data } = await apiClient.get<InterviewSession>('/api/interviews/current');
    return data;
  },

  /**
   * The session state.
   *
   * <p>Also the clock: `remainingSeconds` is recomputed server-side on every
   * call, so this is what the countdown resynchronises against.
   */
  async session(id: number): Promise<InterviewSession> {
    const { data } = await apiClient.get<InterviewSession>(`/api/interviews/${id}`);
    return data;
  },

  async report(id: number): Promise<InterviewReport> {
    const { data } = await apiClient.get<InterviewReport>(`/api/interviews/${id}/report`);
    return data;
  },

  async history(page = 0, size = 10): Promise<PageResponse<InterviewSummary>> {
    const { data } = await apiClient.get<PageResponse<InterviewSummary>>('/api/interviews', {
      params: { page, size },
    });
    return data;
  },

  /** Ends the round early and returns its debrief. */
  async finish(id: number): Promise<InterviewReport> {
    const { data } = await apiClient.post<InterviewReport>(`/api/interviews/${id}/finish`);
    return data;
  },

  /** Throws the round away — not the same as finishing it. */
  async abandon(id: number): Promise<InterviewSummary> {
    const { data } = await apiClient.post<InterviewSummary>(`/api/interviews/${id}/abandon`);
    return data;
  },

  /** The honesty toggle on the report. Changes no score and gates nothing. */
  async selfReport(id: number, usedOutsideHelp: boolean | null): Promise<InterviewReport> {
    const { data } = await apiClient.patch<InterviewReport>(`/api/interviews/${id}/self-report`, {
      usedOutsideHelp,
    });
    return data;
  },

  /**
   * One problem of the set.
   *
   * <p>Fetching it is what starts that problem's clock, which is why the session
   * screen asks for a slot only when it is actually put on screen.
   */
  async problem(id: number, position: number): Promise<InterviewProblemDetail> {
    const { data } = await apiClient.get<InterviewProblemDetail>(
      `/api/interviews/${id}/problems/${position}`,
    );
    return data;
  },

  /** Opens the next hint. The server counts it; nothing here is trusted to. */
  async revealHint(id: number, position: number): Promise<InterviewHints> {
    const { data } = await apiClient.post<InterviewHints>(
      `/api/interviews/${id}/problems/${position}/hints`,
    );
    return data;
  },

  async skip(id: number, position: number): Promise<InterviewSession> {
    const { data } = await apiClient.post<InterviewSession>(
      `/api/interviews/${id}/problems/${position}/skip`,
    );
    return data;
  },

  async run(id: number, position: number, payload: RunPayload): Promise<RunResult> {
    const { data } = await apiClient.post<RunResult>(
      `/api/interviews/${id}/problems/${position}/run`,
      payload,
      { timeout: RUN_TIMEOUT_MS },
    );
    return data;
  },

  /**
   * Judges every case and attributes the verdict to the slot.
   *
   * <p>The attempt also lands in the ordinary submission history: an interview is
   * a different way to be handed a problem, not a different kind of solving.
   */
  async submit(id: number, position: number, payload: SubmitPayload): Promise<SubmissionResult> {
    const { data } = await apiClient.post<SubmissionResult>(
      `/api/interviews/${id}/problems/${position}/submit`,
      payload,
      { timeout: SUBMIT_TIMEOUT_MS },
    );
    return data;
  },
};
