import {
  type AdminProblemDetail,
  type DataType,
  type Difficulty,
  type Language,
  type ProblemUpsertPayload,
} from '../../api/types';

/** Every type a signature can be built from, in the order the picker offers them. */
export const DATA_TYPES: DataType[] = [
  'INT',
  'LONG',
  'DOUBLE',
  'BOOLEAN',
  'STRING',
  'INT_ARRAY',
  'LONG_ARRAY',
  'DOUBLE_ARRAY',
  'STRING_ARRAY',
  'INT_MATRIX',
];

/** The languages a reference solution can be written in. */
export const LANGUAGES: Language[] = ['JAVA', 'PYTHON', 'JAVASCRIPT', 'TYPESCRIPT'];

/**
 * A client-side row identity.
 *
 * <p>React needs a stable key per row and the server's id cannot supply one: a
 * row the author has just added has no id yet, and two of them would collide on
 * `undefined`. A counter is enough — these never leave the browser.
 */
let sequence = 0;
export const newKey = (): string => {
  sequence += 1;
  return `row-${sequence}`;
};

export type ExampleRow = { key: string; id: number | null; input: string; output: string; explanation: string };

export type HintRow = { key: string; content: string };

export type ParameterRow = { key: string; name: string; type: DataType };

export type TestCaseRow = { key: string; id: number | null; input: string; expectedOutput: string; hidden: boolean };

export type EditorialForm = {
  contentMarkdown: string;
  timeComplexity: string;
  spaceComplexity: string;
  solutions: Partial<Record<Language, string>>;
};

/**
 * The whole authoring form, as one value.
 *
 * <p>Held in a single state object rather than a field per section: a save sends
 * the complete document, and the dirty check that guards against navigating away
 * is one comparison against the last saved snapshot.
 */
export type ProblemFormState = {
  title: string;
  slug: string;
  /**
   * Whether the slug has stopped following the title.
   *
   * <p>Client-only. A new problem's slug tracks its title while the author is
   * still deciding on one; the moment they edit the slug — or open a problem
   * that already exists — it stops, because a slug is a URL somebody may already
   * have bookmarked.
   */
  slugLocked: boolean;
  difficulty: Difficulty;
  description: string;
  constraintsMarkdown: string;
  published: boolean;
  archived: boolean;
  tagIds: number[];
  functionName: string;
  /** Empty until a signature is authored — a problem may legitimately have none yet. */
  returnType: DataType | '';
  parameters: ParameterRow[];
  examples: ExampleRow[];
  hints: HintRow[];
  testCases: TestCaseRow[];
  /** Whether the problem has a written solution at all; false sends `editorial: null`. */
  hasEditorial: boolean;
  editorial: EditorialForm;
};

export const emptyEditorial = (): EditorialForm => ({
  contentMarkdown: '',
  timeComplexity: '',
  spaceComplexity: '',
  solutions: {},
});

/** A blank problem: a draft, with one sample case and one example to fill in. */
export const emptyForm = (): ProblemFormState => ({
  title: '',
  slug: '',
  slugLocked: false,
  difficulty: 'EASY',
  description: '',
  constraintsMarkdown: '',
  published: false,
  archived: false,
  tagIds: [],
  functionName: '',
  returnType: '',
  parameters: [],
  examples: [{ key: newKey(), id: null, input: '', output: '', explanation: '' }],
  hints: [],
  testCases: [{ key: newKey(), id: null, input: '', expectedOutput: '', hidden: false }],
  hasEditorial: false,
  editorial: emptyEditorial(),
});

export const formFromDetail = (detail: AdminProblemDetail): ProblemFormState => ({
  title: detail.title,
  slug: detail.slug,
  // Always locked for a stored problem: its URL is in circulation, and a
  // retitling must not quietly move it.
  slugLocked: true,
  difficulty: detail.difficulty,
  description: detail.description,
  constraintsMarkdown: detail.constraintsMarkdown ?? '',
  published: detail.published,
  archived: detail.archived,
  tagIds: detail.tags.map((tag) => tag.id),
  functionName: detail.functionName ?? '',
  returnType: detail.returnType ?? '',
  parameters: detail.parameters.map((parameter) => ({ key: newKey(), name: parameter.name, type: parameter.type })),
  examples: detail.examples.map((example) => ({
    key: newKey(),
    id: example.id ?? null,
    input: example.input,
    output: example.output,
    explanation: example.explanation ?? '',
  })),
  hints: detail.hints.map((content) => ({ key: newKey(), content })),
  testCases: detail.testCases.map((testCase) => ({
    key: newKey(),
    id: testCase.id ?? null,
    input: testCase.input,
    expectedOutput: testCase.expectedOutput,
    hidden: testCase.hidden,
  })),
  hasEditorial: detail.editorial !== undefined,
  editorial: detail.editorial
    ? {
        contentMarkdown: detail.editorial.contentMarkdown,
        timeComplexity: detail.editorial.timeComplexity ?? '',
        spaceComplexity: detail.editorial.spaceComplexity ?? '',
        solutions: detail.editorial.solutions,
      }
    : emptyEditorial(),
});

/**
 * The form as the server's document.
 *
 * <p>Blank optional fields become null rather than empty strings, so "the author
 * cleared the constraints" and "the author never wrote any" are the same stored
 * state — which is what the reader's "no constraints" branch already assumes.
 */
export const formToPayload = (form: ProblemFormState): ProblemUpsertPayload => ({
  title: form.title.trim(),
  slug: form.slug.trim(),
  difficulty: form.difficulty,
  description: form.description,
  constraintsMarkdown: blankToNull(form.constraintsMarkdown),
  published: form.published,
  archived: form.archived,
  tagIds: form.tagIds,
  functionName: blankToNull(form.functionName),
  returnType: form.returnType === '' ? null : form.returnType,
  parameters: form.parameters.map((parameter) => ({ name: parameter.name.trim(), type: parameter.type })),
  examples: form.examples.map((example) => ({
    id: example.id,
    input: example.input,
    output: example.output,
    explanation: blankToNull(example.explanation),
  })),
  hints: form.hints.map((hint) => hint.content),
  testCases: form.testCases.map((testCase) => ({
    id: testCase.id,
    input: testCase.input,
    expectedOutput: testCase.expectedOutput,
    hidden: testCase.hidden,
  })),
  editorial: form.hasEditorial
    ? {
        contentMarkdown: form.editorial.contentMarkdown,
        timeComplexity: blankToNull(form.editorial.timeComplexity),
        spaceComplexity: blankToNull(form.editorial.spaceComplexity),
        // A language the author opened but never wrote in is not an authored
        // solution, and would otherwise render as an empty tab for the reader.
        solutions: Object.fromEntries(
          Object.entries(form.editorial.solutions).filter(([, source]) => source.trim() !== ''),
        ),
      }
    : null,
});

/** The same reduction the server applies, so the previewed slug is the stored one. */
export const slugify = (value: string): string =>
  value
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, '-')
    .replaceAll(/(^-|-$)/g, '');

/** Whether the form still holds a complete signature — what "solvable" means. */
export const hasSignature = (form: ProblemFormState): boolean =>
  form.functionName.trim() !== '' && form.returnType !== '';

/** Moves a row within a list, or returns the list unchanged at either end. */
export const moveRow = <T>(rows: T[], index: number, delta: number): T[] => {
  const target = index + delta;
  if (target < 0 || target >= rows.length) {
    return rows;
  }

  const next = [...rows];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
};

const blankToNull = (value: string): string | null => (value.trim() === '' ? null : value.trim());
