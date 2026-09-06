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
  /** Whether they have submitted at all. Every solved problem is also attempted. */
  attempted: boolean;
  /** Accepted submissions over all submissions, absent until somebody has submitted. */
  acceptanceRate?: number;
  totalSubmissions: number;
};

/** Narrows the catalogue by what the signed-in user has done with each problem. */
export type ProblemStatusFilter = 'SOLVED' | 'ATTEMPTED' | 'TODO';

/** Sort keys the catalogue endpoint accepts. */
export type ProblemSort = 'id' | 'title' | 'difficulty' | 'acceptance';

export type SortOrder = 'asc' | 'desc';

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
  /**
   * How many further cases a submission is judged against.
   *
   * The count is public on purpose — passing the samples is not the bar, and a
   * solver should know that before they submit — while their contents are not.
   */
  hiddenTestCaseCount: number;
  /** Whether a written solution exists — the Editorial tab is disabled without one. */
  hasEditorial: boolean;
  solved: boolean;
  attempted: boolean;
  acceptanceRate?: number;
  totalSubmissions: number;
  acceptedSubmissions: number;
};

/**
 * A problem's written solution. Mirrors `EditorialResponse`.
 *
 * `solutions` is keyed by the languages the editorial was authored in, which is
 * what the panel's language picker offers — the same shape as `starterCode`.
 */
export type Editorial = {
  /** Markdown: the approach, and why the obvious attempt falls short. */
  contentMarkdown: string;
  timeComplexity?: string;
  spaceComplexity?: string;
  solutions: Partial<Record<Language, string>>;
};

export type RunPayload = { language: Language; sourceCode: string };

export type SubmitPayload = RunPayload;

/**
 * One judged case's outcome. Mirrors `CaseResultResponse`.
 *
 * A hidden case arrives with `hidden: true` and nothing but its verdict and
 * timings — everything that could give the case away is stripped server-side, so
 * the optional fields here are genuinely absent rather than merely unread.
 */
export type CaseResult = {
  testCaseId?: number;
  status: ExecutionStatus;
  hidden: boolean;
  input?: string;
  expectedOutput?: string;
  /** The answer line: the last non-blank line the program printed. */
  actualOutput?: string;
  /** Everything printed, so a solver's own debug output survives. */
  stdout?: string;
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

/** Mirrors `SubmissionResultResponse` — the verdict on a submission. */
export type SubmissionResult = {
  submissionId: number;
  status: ExecutionStatus;
  compileOutput?: string;
  failureMessage?: string;
  passed: number;
  total: number;
  /** How many of `total` were hidden. */
  hiddenTotal: number;
  runtimeMs?: number;
  memoryKb?: number;
  /** True only for the submission that first solved the problem. */
  firstAccepted: boolean;
  results: CaseResult[];
};

/** One row in a submission history. Mirrors `SubmissionSummaryResponse`. */
export type SubmissionSummary = {
  id: number;
  problemSlug: string;
  problemTitle: string;
  difficulty: Difficulty;
  status: ExecutionStatus;
  language: Language;
  runtimeMs?: number;
  memoryKb?: number;
  passedTests?: number;
  totalTests?: number;
  createdAt: string;
};

/** A submission with the code that produced it. */
export type SubmissionDetail = SubmissionSummary & {
  sourceCode: string;
  failureMessage?: string;
};

/** Solved counts and acceptance, for the dashboard. Mirrors `UserStatsResponse`. */
export type UserStats = {
  solved: number;
  totalProblems: number;
  submissions: number;
  acceptedSubmissions: number;
  acceptanceRate?: number;
  progress: DifficultyProgress[];
};

export type DifficultyProgress = { difficulty: Difficulty; solved: number; total: number };

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
