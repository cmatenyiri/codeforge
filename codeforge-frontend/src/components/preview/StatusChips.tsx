import { Chip, type ChipProps } from '@mui/material';
import { type SxProps, type Theme } from '@mui/material/styles';

export type Difficulty = 'easy' | 'medium' | 'hard';
export type Verdict = 'accepted' | 'wrongAnswer' | 'timeLimit' | 'runtimeError' | 'compileError' | 'pending';

const difficultyLabels: Record<Difficulty, string> = {
  easy: 'Easy',
  medium: 'Medium',
  hard: 'Hard',
};

const verdictLabels: Record<Verdict, string> = {
  accepted: 'Accepted',
  wrongAnswer: 'Wrong Answer',
  timeLimit: 'Time Limit Exceeded',
  runtimeError: 'Runtime Error',
  compileError: 'Compile Error',
  pending: 'Pending',
};

/** Lets a caller layer their own `sx` on top of the component's own styles. */
const mergeSx = (base: SxProps<Theme>, extra: SxProps<Theme> | undefined): SxProps<Theme> =>
  extra === undefined ? base : ([base, extra].flat() as SxProps<Theme>);

/** Difficulty badge, driven entirely by `palette.difficulty.*`. */
export const DifficultyChip = ({ level, sx, ...props }: { level: Difficulty } & ChipProps) => (
  <Chip
    {...props}
    label={difficultyLabels[level]}
    sx={mergeSx({ color: `difficulty.${level}`, backgroundColor: `difficulty.${level}Bg` }, sx)}
  />
);

/** Judge verdict badge, driven entirely by `palette.verdict.*`. */
export const VerdictChip = ({ verdict, sx, ...props }: { verdict: Verdict } & ChipProps) => (
  <Chip
    {...props}
    label={verdictLabels[verdict]}
    sx={mergeSx({ color: `verdict.${verdict}`, backgroundColor: `verdict.${verdict}Bg` }, sx)}
  />
);
