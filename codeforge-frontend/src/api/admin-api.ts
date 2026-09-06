import { apiClient } from './client';
import {
  type AdminProblemDetail,
  type AdminProblemSummary,
  type Difficulty,
  type Language,
  type PageResponse,
  type ProblemState,
  type ProblemUpsertPayload,
  type RunPayload,
  type RunResult,
  type SignaturePreviewPayload,
  type Tag,
} from './types';

/** Judging a reference solution runs every case, hidden ones included — a cold Java compile alone outlasts the default. */
const VALIDATE_TIMEOUT_MS = 180_000;

export type AdminProblemQuery = {
  search?: string;
  difficulty?: Difficulty | '';
  tag?: string;
  state?: ProblemState | '';
  sort?: AdminProblemSort;
  order?: 'asc' | 'desc';
  page?: number;
  size?: number;
};

/** Sort keys the authoring catalogue accepts. */
export type AdminProblemSort = 'id' | 'title' | 'difficulty' | 'updated';

const params = (query: AdminProblemQuery) =>
  Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== ''));

/**
 * Authoring the catalogue.
 *
 * <p>Everything here is behind `hasRole('ADMIN')` on the server; the client-side
 * route guard only spares an admin-less user a screen full of 403s.
 *
 * <p>Every write returns the saved problem rather than a bare 204 — the form has
 * to adopt the ids of the rows it just created, or the next save would insert
 * them all over again.
 */
export const adminApi = {
  async list(query: AdminProblemQuery): Promise<PageResponse<AdminProblemSummary>> {
    const { data } = await apiClient.get<PageResponse<AdminProblemSummary>>('/api/admin/problems', {
      params: params(query),
    });
    return data;
  },

  /** Keyed by id, not slug: the slug is one of the things being edited. */
  async get(id: number): Promise<AdminProblemDetail> {
    const { data } = await apiClient.get<AdminProblemDetail>(`/api/admin/problems/${id}`);
    return data;
  },

  async create(payload: ProblemUpsertPayload): Promise<AdminProblemDetail> {
    const { data } = await apiClient.post<AdminProblemDetail>('/api/admin/problems', payload);
    return data;
  },

  async update(id: number, payload: ProblemUpsertPayload): Promise<AdminProblemDetail> {
    const { data } = await apiClient.put<AdminProblemDetail>(`/api/admin/problems/${id}`, payload);
    return data;
  },

  /** Copies a problem as a draft — how most new problems start. */
  async duplicate(id: number): Promise<AdminProblemDetail> {
    const { data } = await apiClient.post<AdminProblemDetail>(`/api/admin/problems/${id}/duplicate`);
    return data;
  },

  /** 400 with field errors when the problem is not complete enough to publish. */
  async setPublished(id: number, value: boolean): Promise<AdminProblemDetail> {
    const { data } = await apiClient.patch<AdminProblemDetail>(`/api/admin/problems/${id}/published`, { value });
    return data;
  },

  async setArchived(id: number, value: boolean): Promise<AdminProblemDetail> {
    const { data } = await apiClient.patch<AdminProblemDetail>(`/api/admin/problems/${id}/archived`, { value });
    return data;
  },

  /** 409 once the problem has submissions or has been asked in an interview. */
  async remove(id: number): Promise<void> {
    await apiClient.delete(`/api/admin/problems/${id}`);
  },

  /**
   * Judges a reference solution against every stored case.
   *
   * <p>Runs against what is saved, not what is on screen: it is the check that
   * the expected outputs in the database are the ones a correct solution
   * actually produces.
   */
  async validate(id: number, payload: RunPayload): Promise<RunResult> {
    const { data } = await apiClient.post<RunResult>(`/api/admin/problems/${id}/validate`, payload, {
      timeout: VALIDATE_TIMEOUT_MS,
    });
    return data;
  },

  /** The stubs a signature would generate, before it is saved. */
  async starterCode(payload: SignaturePreviewPayload): Promise<Partial<Record<Language, string>>> {
    const { data } = await apiClient.post<Partial<Record<Language, string>>>(
      '/api/admin/problems/starter-code',
      payload,
    );
    return data;
  },

  async createTag(name: string): Promise<Tag> {
    const { data } = await apiClient.post<Tag>('/api/admin/tags', { name });
    return data;
  },
};
