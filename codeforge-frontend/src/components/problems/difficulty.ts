import { type Difficulty } from '../../api/types';

export const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];

/**
 * Literal keys rather than an interpolated `difficulty.${...}`, so a renamed or
 * missing translation is a compile error instead of a string that renders as
 * itself.
 */
export const DIFFICULTY_LABEL_KEY = {
  EASY: 'difficulty.easy',
  MEDIUM: 'difficulty.medium',
  HARD: 'difficulty.hard',
} as const satisfies Record<Difficulty, string>;

/** Palette token suffix for `palette.difficulty.*`. */
export const DIFFICULTY_TOKEN = {
  EASY: 'easy',
  MEDIUM: 'medium',
  HARD: 'hard',
} as const satisfies Record<Difficulty, string>;
