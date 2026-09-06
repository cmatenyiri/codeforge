import { type Language } from '../api/types';

/** Our language enum to Monaco's language ids. */
export const MONACO_LANGUAGE_ID = {
  JAVA: 'java',
  PYTHON: 'python',
  JAVASCRIPT: 'javascript',
  TYPESCRIPT: 'typescript',
} as const satisfies Record<Language, string>;

/**
 * The extension a model's file name gets.
 *
 * <p>Not cosmetic. The TypeScript worker decides whether a file is TypeScript or
 * JavaScript from its extension alone, not from the editor's language id — so a
 * model at an extension-less path is parsed as JavaScript, and every type
 * annotation is reported as "Type annotations can only be used in TypeScript
 * files".
 */
export const MONACO_FILE_EXTENSION = {
  JAVA: 'java',
  PYTHON: 'py',
  JAVASCRIPT: 'js',
  TYPESCRIPT: 'ts',
} as const satisfies Record<Language, string>;
