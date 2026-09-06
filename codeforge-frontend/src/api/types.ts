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

/* ------------------------------------------------------------------------- */
/* Authoring (admin only)                                                     */
/* ------------------------------------------------------------------------- */

/** Where a problem is in its authoring life. Mirrors `com.codeforge.domain.ProblemState`. */
export type ProblemState = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

/**
 * The value shapes a solution signature can be built from.
 *
 * Mirrors `com.codeforge.domain.DataType`. Deliberately small: every member has
 * to be expressible in all four languages, parseable from one line of a test
 * case's input, and printable in one canonical form.
 */
export type DataType =
  | 'INT'
  | 'LONG'
  | 'DOUBLE'
  | 'BOOLEAN'
  | 'STRING'
  | 'INT_ARRAY'
  | 'LONG_ARRAY'
  | 'DOUBLE_ARRAY'
  | 'STRING_ARRAY'
  | 'INT_MATRIX';

/** One argument of the function a solver implements. Order is argument order. */
export type ProblemParameterPayload = { name: string; type: DataType };

/** `id` is the stored row this replaces, so an edit keeps it instead of recreating it. */
export type ProblemExamplePayload = {
  id?: number | null;
  input: string;
  output: string;
  explanation?: string | null;
};

export type TestCasePayload = {
  id?: number | null;
  /** One line per argument, in signature order. */
  input: string;
  expectedOutput: string;
  /** Graded but never shown; only a submission is judged against it. */
  hidden: boolean;
};

export type EditorialPayload = {
  contentMarkdown: string;
  timeComplexity?: string | null;
  spaceComplexity?: string | null;
  solutions: Partial<Record<Language, string>>;
};

/**
 * A whole problem, as one write. Mirrors `ProblemUpsertRequest`.
 *
 * Create and edit send the same shape, and the shape is the complete document
 * rather than a patch: the alternative makes "I removed the last example"
 * indistinguishable from "I did not touch the examples".
 */
export type ProblemUpsertPayload = {
  title: string;
  /** Blank derives one from the title, which is what an author wants until a rename would break links. */
  slug: string;
  difficulty: Difficulty;
  description: string;
  constraintsMarkdown?: string | null;
  published: boolean;
  archived: boolean;
  tagIds: number[];
  functionName?: string | null;
  returnType?: DataType | null;
  parameters: ProblemParameterPayload[];
  examples: ProblemExamplePayload[];
  hints: string[];
  testCases: TestCasePayload[];
  /** null leaves the problem without a written solution, or deletes the one it has. */
  editorial: EditorialPayload | null;
};

/** One row in the authoring catalogue. Mirrors `AdminProblemSummaryResponse`. */
export type AdminProblemSummary = {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  state: ProblemState;
  tags: Tag[];
  testCaseCount: number;
  sampleTestCaseCount: number;
  hasEditorial: boolean;
  /** Whether a signature has been authored — without one the problem cannot be opened in the editor. */
  solvable: boolean;
  totalSubmissions: number;
  acceptanceRate?: number;
  updatedAt: string;
};

/**
 * A problem in full, for the form. Mirrors `AdminProblemDetailResponse`.
 *
 * The one response that carries the hidden test cases — an author has to see
 * what submissions are judged against.
 */
export type AdminProblemDetail = {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  description: string;
  constraintsMarkdown?: string;
  state: ProblemState;
  published: boolean;
  archived: boolean;
  tags: Tag[];
  functionName?: string;
  returnType?: DataType;
  parameters: ProblemParameterPayload[];
  examples: ProblemExamplePayload[];
  hints: string[];
  testCases: TestCasePayload[];
  editorial?: EditorialPayload;
  /** Generated from the stored signature, so the author sees the stub a solver gets. */
  starterCode: Partial<Record<Language, string>>;
  totalSubmissions: number;
  acceptedSubmissions: number;
  createdAt: string;
  updatedAt: string;
};

/** A signature to render starter code for, before it has been saved. */
export type SignaturePreviewPayload = {
  functionName: string;
  returnType: DataType;
  parameters: ProblemParameterPayload[];
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/* ------------------------------------------------------------------------- */
/* Mock interviews                                                            */
/* ------------------------------------------------------------------------- */

/** Mirrors `com.codeforge.domain.InterviewFormat`. */
export type InterviewFormat = 'WARM_UP' | 'STANDARD' | 'HARD';

/** Mirrors `com.codeforge.domain.InterviewStatus`. */
export type InterviewStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

/** The band a finished round lands in. Mirrors `InterviewOutcome`. */
export type InterviewOutcome = 'NO_SOLVE' | 'PARTIAL' | 'SOLID' | 'STRONG';

/**
 * A process observation on the report. Mirrors `InterviewInsight`.
 *
 * Sent as a code and translated here like every other string, which is why the
 * backend never returns prose.
 */
export type InterviewInsight =
  | 'NO_SUBMISSION'
  | 'ALL_SOLVED'
  | 'CLEAN_RUN'
  | 'FINISHED_EARLY'
  | 'RAN_OUT_OF_TIME'
  | 'SLOW_WARM_UP'
  | 'HINTS_USED'
  | 'MANY_ATTEMPTS'
  | 'SKIPPED_PROBLEM';

/** One format on the lobby screen. Mirrors `InterviewFormatResponse`. */
export type InterviewFormatOption = {
  format: InterviewFormat;
  durationMinutes: number;
  problemCount: number;
  /** The difficulty wanted at each position, warm-up first. */
  slots: Difficulty[];
};

/**
 * One problem's place in a running round. Mirrors `InterviewSlotResponse`.
 *
 * Carries no description: the problem itself is fetched a slot at a time, which
 * is also what starts its clock.
 */
export type InterviewSlot = {
  position: number;
  problemId: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  /** The gentler opener of a two-problem round. */
  warmUp: boolean;
  /** Not reached yet: the round is sequential, so this problem cannot be opened. */
  locked: boolean;
  /** Solved or skipped, and read-only from here on. */
  resolved: boolean;
  solved: boolean;
  skipped: boolean;
  attempts: number;
  hintsRevealed: number;
};

/**
 * A running interview. Mirrors `InterviewSessionResponse`.
 *
 * `remainingSeconds` is the server's answer and the only one that counts; the
 * screen ticks down from it between polls purely so the clock moves.
 */
export type InterviewSession = {
  id: number;
  format: InterviewFormat;
  status: InterviewStatus;
  startedAt: string;
  durationMinutes: number;
  remainingSeconds: number;
  /** The problem the round is on; absent once every one is solved or skipped. */
  activePosition?: number;
  problems: InterviewSlot[];
};

/**
 * A problem as it appears mid-round. Mirrors `InterviewProblemResponse`.
 *
 * Note what is not here, compared with `ProblemDetail`: no `hasEditorial`, no
 * `solved`/`attempted` for the catalogue at large, and no topic tags — "sliding
 * window" above the statement is the answer to most problems that have it. None
 * of it is withheld by this client; the server does not send it.
 */
export type InterviewProblemDetail = {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  description: string;
  constraintsMarkdown?: string;
  examples: ProblemExample[];
  sampleTestCases: TestCase[];
  hiddenTestCaseCount: number;
  starterCode: Partial<Record<Language, string>>;
  position: number;
  warmUp: boolean;
  /** Whether this slot has been solved in *this* round. */
  solved: boolean;
  skipped: boolean;
  /**
   * False once the round has moved past this problem. The statement stays
   * readable; the editor goes read-only and the judge refuses it either way.
   */
  editable: boolean;
  /**
   * What a closed problem shows: the code the judge actually saw.
   *
   * Not the local draft — that keeps taking keystrokes while a submission is
   * being judged, so an acceptance can arrive and lock the problem over text
   * that was never submitted. Absent while the problem is open, and for one
   * skipped without a single attempt.
   */
  submittedSourceCode?: string;
  submittedLanguage?: Language;
  attempts: number;
  /** How many the problem has, so the button can say how many are left. */
  hintCount: number;
  /** Only those revealed so far; each one was counted against the round. */
  hints: string[];
};

/** Mirrors `InterviewHintResponse`. */
export type InterviewHints = { hintCount: number; hintsRevealed: number; hints: string[] };

/** How one problem went. Mirrors `InterviewProblemResultResponse`. */
export type InterviewProblemResult = {
  position: number;
  problemId: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  warmUp: boolean;
  solved: boolean;
  skipped: boolean;
  /** Measured from opening the problem, not from the start of the round. */
  timeToSolveSeconds?: number;
  attempts: number;
  hintsRevealed: number;
  submissionId?: number;
};

/** The debrief. Mirrors `InterviewReportResponse`. */
export type InterviewReport = {
  id: number;
  format: InterviewFormat;
  status: InterviewStatus;
  /** Absent while the round is still running, and for an abandoned one. */
  outcome?: InterviewOutcome;
  startedAt: string;
  endedAt?: string;
  durationMinutes: number;
  elapsedSeconds: number;
  solved: number;
  total: number;
  attempts: number;
  hintsRevealed: number;
  /** The candidate's own answer; absent means they have not said. */
  usedOutsideHelp?: boolean;
  problems: InterviewProblemResult[];
  insights: InterviewInsight[];
};

/** One row in the interview history. Mirrors `InterviewSummaryResponse`. */
export type InterviewSummary = {
  id: number;
  format: InterviewFormat;
  status: InterviewStatus;
  outcome?: InterviewOutcome;
  startedAt: string;
  endedAt?: string;
  durationMinutes: number;
  /** Solved problems; absent for a round that never finished. */
  score?: number;
  total: number;
};
