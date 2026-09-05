import { useCallback, useEffect, useState } from 'react';
import { type Language } from '../../api/types';

/**
 * Keeps a per-language draft of the solver's code in `localStorage`.
 *
 * <p>Losing work to a refresh is the fastest way to make an editor feel unsafe,
 * and until submissions exist there is nowhere else for the code to live. The
 * key includes the language so switching between them is non-destructive: each
 * language keeps its own attempt.
 */

const draftKey = (slug: string, language: Language) => `codeforge.draft.${slug}.${language}`;

/** Storage can be unavailable (private browsing, blocked site data), never fatally. */
const read = (key: string): string | null => {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
};

const write = (key: string, value: string): void => {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Nothing to do: the draft is a convenience, not the source of truth.
  }
};

const remove = (key: string): void => {
  try {
    localStorage.removeItem(key);
  } catch {
    // As above.
  }
};

/** How long typing has to pause before the draft is written. */
const PERSIST_DELAY_MS = 400;

export const useCodeDraft = (slug: string, language: Language, starterCode: string) => {
  const key = draftKey(slug, language);
  const [code, setCode] = useState(() => read(key) ?? starterCode);

  // The key changes when the language does, which is the cue to load that
  // language's own draft rather than carry the previous one across.
  useEffect(() => {
    setCode(read(key) ?? starterCode);
  }, [key, starterCode]);

  useEffect(() => {
    const timer = setTimeout(() => {
      write(key, code);
    }, PERSIST_DELAY_MS);

    return () => {
      clearTimeout(timer);
    };
  }, [key, code]);

  /** Discards the draft and returns to the generated stub. */
  const reset = useCallback(() => {
    remove(key);
    setCode(starterCode);
  }, [key, starterCode]);

  return { code, setCode, reset };
};
