import { Box, Paper } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import { useEffect, useState } from 'react';
import { type Language } from '../../api/types';
import { MONACO_LANGUAGE_ID } from '../../monaco/languages';
import { MONACO_THEME } from '../../monaco/theme';

/**
 * A reference solution, syntax-highlighted.
 *
 * <p>Uses Monaco's `colorize` rather than mounting a second editor: the solving
 * page has already paid for Monaco, and `colorize` runs the same tokenizer and
 * the same theme over a string, returning HTML. A read-only editor instance
 * would cost a whole model, view and DOM tree to display twenty lines nobody can
 * type into.
 *
 * <p>Falls back to plain monospace if colorizing fails, so a tokenizer that has
 * not registered yet costs the highlighting and not the solution.
 */
export const EditorialCode = ({ code, language }: { code: string; language: Language }) => {
  const { mode, systemMode } = useColorScheme();
  const resolved = mode === 'system' ? systemMode : mode;
  const [html, setHtml] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setHtml(null);

    // Imported here rather than at module scope so the editorial tab does not
    // drag Monaco into a chunk that might otherwise not need it.
    void import('monaco-editor/editor/editor.api')
      .then(async (monaco) => {
        // colorize reads the globally active theme, so it has to be set first.
        monaco.editor.setTheme(MONACO_THEME[resolved === 'light' ? 'light' : 'dark']);
        return monaco.editor.colorize(code, MONACO_LANGUAGE_ID[language], { tabSize: 4 });
      })
      .then((colorized) => {
        if (!cancelled) {
          setHtml(colorized);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setHtml(null);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [code, language, resolved]);

  return (
    <Paper variant="sunken" sx={{ p: 1.75, overflowX: 'auto' }}>
      <Box
        component="pre"
        sx={{
          typography: 'code',
          m: 0,
          whiteSpace: 'pre',
          // colorize emits its own spans; the container supplies the metrics.
          '& span': { fontFamily: 'inherit !important', fontSize: 'inherit !important' },
        }}
        // Monaco's own tokenizer output, over a string this app authored.
        {...(html === null ? { children: code } : { dangerouslySetInnerHTML: { __html: html } })}
      />
    </Paper>
  );
};
