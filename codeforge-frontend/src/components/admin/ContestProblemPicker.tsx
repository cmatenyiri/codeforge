import ArrowDownwardRounded from '@mui/icons-material/ArrowDownwardRounded';
import ArrowUpwardRounded from '@mui/icons-material/ArrowUpwardRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import {
  Alert,
  Autocomplete,
  Box,
  IconButton,
  Paper,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi } from '../../api/admin-api';
import { type AdminProblemSummary } from '../../api/types';
import { DifficultyChip } from '../problems/DifficultyChip';
import { ProblemStateChip } from './ProblemStateChip';

/** One question of a contest, as the form holds it. */
export type ContestQuestion = {
  problemId: number;
  points: number;
  /** Filled in for a saved contest; absent for a row the author has just added. */
  title?: string;
  slug?: string;
  difficulty?: AdminProblemSummary['difficulty'];
  state?: AdminProblemSummary['state'];
  solvable?: boolean;
  /** Filled in for a saved contest: whether the problem has ever been public. */
  everPublished?: boolean;
};

/**
 * Picks the problems a contest asks, in order.
 *
 * <p>Only drafts that have never been public are offered. A problem in the
 * catalogue is readable — editorial and all — before the contest starts; one
 * that has been in it and was taken back out has been read already; and one
 * another announced contest holds is spoken for. The contest keeps its
 * questions hidden until it ends and then publishes them, so a question is new
 * to everybody sitting it.
 *
 * <p>Warnings are still worth the space they take, for questions saved before a
 * problem changed state. A public or once-public one will be refused when the
 * contest is announced — said only until the contest seals, since a finished
 * contest publishes its questions itself; and one with no signature or no test
 * cases renders an editor that cannot run, which is a wasted ninety minutes for
 * the whole field and cannot be fixed once the contest has sealed.
 */
export const ContestProblemPicker = ({
  questions,
  onChange,
  errorFor,
  disabled,
}: {
  questions: ContestQuestion[];
  onChange: (questions: ContestQuestion[]) => void;
  errorFor: (index: number) => string | undefined;
  /** True once the contest has sealed: the questions and their points are settled. */
  disabled: boolean;
}) => {
  const { t } = useTranslation();
  const [options, setOptions] = useState<AdminProblemSummary[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    let cancelled = false;

    const timer = setTimeout(() => {
      adminApi
        .list({ search, state: 'DRAFT', neverPublished: true, size: 20, sort: 'updated', order: 'desc' })
        .then((page) => {
          if (!cancelled) {
            setOptions(page.content);
          }
        })
        .catch(() => {
          // A failed lookup leaves the last options on screen; the author can
          // still reorder and re-point what is already there.
        });
    }, 250);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [search]);

  const move = (index: number, delta: number) => {
    const next = [...questions];
    const target = index + delta;
    if (target < 0 || target >= next.length) {
      return;
    }
    [next[index], next[target]] = [next[target]!, next[index]!];
    onChange(next);
  };

  const update = (index: number, patch: Partial<ContestQuestion>) => {
    onChange(questions.map((question, at) => (at === index ? { ...question, ...patch } : question)));
  };

  return (
    <Stack spacing={2}>
      {questions.map((question, index) => (
        <Paper key={`${question.problemId}-${index}`} variant="sunken" sx={{ p: 2 }}>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
            <Typography variant="overline" sx={{ color: 'text.disabled', mt: 1.5, minWidth: 28 }}>
              Q{index + 1}
            </Typography>

            <Stack spacing={1} sx={{ flex: 1, minWidth: 0 }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap' }} useFlexGap>
                <Typography sx={{ fontWeight: 600 }}>
                  {question.title ?? `#${question.problemId}`}
                </Typography>
                {question.difficulty ? <DifficultyChip difficulty={question.difficulty} size="small" /> : null}
                {question.state ? <ProblemStateChip state={question.state} size="small" /> : null}
              </Stack>

              {/* Only while the questions can still change. Once a contest has
                  started they are settled, and once it has ended they are
                  published on purpose — the warning would describe the release
                  as a mistake. */}
              {disabled ? null : question.state === 'PUBLISHED' ? (
                <Alert severity="warning" sx={{ py: 0 }}>
                  {t('admin.contest.publishedProblemWarning')}
                </Alert>
              ) : question.everPublished === true ? (
                <Alert severity="warning" sx={{ py: 0 }}>
                  {t('admin.contest.previouslyPublicWarning')}
                </Alert>
              ) : null}
              {question.solvable === false ? (
                <Alert severity="error" sx={{ py: 0 }}>
                  {t('admin.contest.notSolvable')}
                </Alert>
              ) : null}
              {errorFor(index) ? <Alert severity="error" sx={{ py: 0 }}>{errorFor(index)}</Alert> : null}
            </Stack>

            <TextField
              label={t('admin.contest.points')}
              type="number"
              size="small"
              value={question.points}
              disabled={disabled}
              onChange={(event) => {
                update(index, { points: Number(event.target.value) });
              }}
              sx={{ width: 96 }}
            />

            <Stack direction="row">
              <Tooltip title={t('admin.contest.moveUp')}>
                <span>
                  <IconButton size="small" disabled={disabled || index === 0} onClick={() => { move(index, -1); }}>
                    <ArrowUpwardRounded fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
              <Tooltip title={t('admin.contest.moveDown')}>
                <span>
                  <IconButton
                    size="small"
                    disabled={disabled || index === questions.length - 1}
                    onClick={() => { move(index, 1); }}
                  >
                    <ArrowDownwardRounded fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
              <Tooltip title={t('admin.contest.removeQuestion')}>
                <span>
                  <IconButton
                    size="small"
                    disabled={disabled}
                    onClick={() => {
                      onChange(questions.filter((_, at) => at !== index));
                    }}
                  >
                    <DeleteOutlineRounded fontSize="small" />
                  </IconButton>
                </span>
              </Tooltip>
            </Stack>
          </Stack>
        </Paper>
      ))}

      {/* Nothing to add once the contest has started — the editor says why, once,
          at the top of the page. */}
      {disabled ? null : (
        <Autocomplete
          options={options.filter(
            (option) => !questions.some((question) => question.problemId === option.id),
          )}
          getOptionLabel={(option) => option.title}
          inputValue={search}
          onInputChange={(_, value) => {
            setSearch(value);
          }}
          value={null}
          onChange={(_, option) => {
            if (option !== null) {
              onChange([
                ...questions,
                {
                  problemId: option.id,
                  // 3, 4, 5, 6 — the ramp the format is built around.
                  points: 3 + questions.length,
                  title: option.title,
                  slug: option.slug,
                  difficulty: option.difficulty,
                  state: option.state,
                  solvable: option.solvable,
                },
              ]);
              setSearch('');
            }
          }}
          renderOption={(props, option) => (
            <Box component="li" {...props} key={option.id}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', width: '100%' }}>
                <Box sx={{ flex: 1, minWidth: 0 }}>{option.title}</Box>
                <DifficultyChip difficulty={option.difficulty} size="small" />
                <ProblemStateChip state={option.state} size="small" />
              </Stack>
            </Box>
          )}
          renderInput={(params) => <TextField {...params} label={t('admin.contest.addQuestion')} />}
        />
      )}
    </Stack>
  );
};
