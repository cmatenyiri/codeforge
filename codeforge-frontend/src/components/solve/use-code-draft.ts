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

  // The draft and the key it belongs to are stored together, so the two can never
  // disagree about which language the code on screen is written in.
  const [draft, setDraft] = useState(() => ({ key, code: read(key) ?? starterCode }));

  // Adjusted during render rather than in an effect. An effect would leave one
  // committed render where the key is the new language but the code is still the
  // old one — long enough for the editor to build the new model out of the
  // previous language's source. React re-runs this component immediately
  // instead, before anything reaches the screen.
  if (draft.key !== key) {
    setDraft({ key, code: read(key) ?? starterCode });
  }

  const setCode = useCallback((code: string) => {
    setDraft((current) => ({ ...current, code }));
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      write(draft.key, draft.code);
    }, PERSIST_DELAY_MS);

    return () => {
      clearTimeout(timer);
    };
  }, [draft]);

  /** Discards the draft and returns to the generated stub. */
  const reset = useCallback(() => {
    remove(key);
    setDraft({ key, code: starterCode });
  }, [key, starterCode]);

  return { code: draft.code, setCode, reset };
};
