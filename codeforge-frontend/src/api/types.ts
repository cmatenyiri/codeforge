/** Mirrors `com.codeforge.web.dto.error.ApiFieldError`. */
export type ApiFieldError = {
  /** Matches the form field name exactly. */
  field: string;
  /** i18n key, e.g. `validation.username.taken`. */
  code: string;
  /** English fallback, used only when the code has no translation. */
  message: string;
};

/** Mirrors `com.codeforge.web.dto.error.ApiError`. */
export type ApiErrorBody = {
  status: number;
  code: string;
  message: string;
  path: string;
  timestamp: string;
  /** Present only for form validation failures. */
  fieldErrors?: ApiFieldError[];
};

export type Role = 'USER' | 'ADMIN';

export type UserResponse = {
  id: number;
  username: string;
  email: string;
  role: Role;
  /** One of the catalogue ids in `components/user/avatars.ts`; always set. */
  avatar: string;
  createdAt: string;
};

/**
 * Note there is no token here: it arrives as an httpOnly cookie the browser
 * stores and replays on its own, and that this code cannot read.
 */
export type AuthResponse = {
  user: UserResponse;
  expiresInSeconds: number;
};

export type RegisterPayload = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  avatar: string;
};

export type ChangeAvatarPayload = { avatar: string };

export type ChangeUsernamePayload = { username: string };

export type ChangePasswordPayload = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export type LoginPayload = {
  username: string;
  password: string;
};

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type Tag = { id: number; name: string; slug: string };

export type ProblemSummary = {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  tags: Tag[];
  /** Whether the signed-in user has an accepted submission for it. */
  solved: boolean;
};

export type ProblemExample = { input: string; output: string; explanation?: string };

export type TestCase = { id: number; input: string; expectedOutput: string };

/** Mirrors `com.codeforge.domain.Language`. */
export type Language = 'JAVA' | 'PYTHON' | 'JAVASCRIPT' | 'TYPESCRIPT';

/** Mirrors `com.codeforge.domain.SubmissionStatus`. */
export type ExecutionStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'COMPILE_ERROR'
  | 'INTERNAL_ERROR';

export type ProblemDetail = {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  /** Markdown. */
  description: string;
  /** Markdown; absent when the author left it blank. */
  constraintsMarkdown?: string;
  tags: Tag[];
  examples: ProblemExample[];
  hints: string[];
  /** Sample cases only — hidden ones never leave the server. */
  sampleTestCases: TestCase[];
  /**
   * Editable stub per language, generated from the problem's signature.
   *
   * Its keys are the languages this problem can be solved in; an empty object
   * means no signature has been authored, so the editor cannot run anything.
   */
  starterCode: Partial<Record<Language, string>>;
  solved: boolean;
};

export type RunPayload = { language: Language; sourceCode: string };

/** One sample case's outcome. Mirrors `CaseResultResponse`. */
export type CaseResult = {
  testCaseId: number;
  status: ExecutionStatus;
  input: string;
  expectedOutput: string;
  /** The answer line: the last non-blank line the program printed. */
  actualOutput: string;
  /** Everything printed, so a solver's own debug output survives. */
  stdout: string;
  stderr?: string;
  runtimeMs?: number;
  memoryKb?: number;
};

/** Mirrors `RunResponse`. */
export type RunResult = {
  /** The first case that did not pass decides this; ACCEPTED only if all did. */
  status: ExecutionStatus;
  /** Set only when nothing ran, and then the only thing worth showing. */
  compileOutput?: string;
  passed: number;
  total: number;
  /** The slowest case, and the peak across cases. */
  runtimeMs?: number;
  memoryKb?: number;
  results: CaseResult[];
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
