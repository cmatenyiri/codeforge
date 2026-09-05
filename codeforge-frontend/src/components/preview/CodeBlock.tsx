import { Box, Paper, Stack, Typography } from '@mui/material';
import { Fragment } from 'react';

/** A token kind, mapped to `theme.palette.code.*`. */
type TokenKind = 'keyword' | 'string' | 'number' | 'func' | 'comment' | 'variable' | 'operator' | 'type' | 'plain';

export type CodeToken = [text: string, kind: TokenKind];

type CodeBlockProps = {
  lines: CodeToken[][];
  /** 1-based line numbers to tint as the "current" execution line. */
  highlight?: number[];
  showLineNumbers?: boolean;
};

const Token = ({ text, kind }: { text: string; kind: TokenKind }) => (
  <Box component="span" sx={kind === 'plain' ? { color: 'code.variable' } : { color: `code.${kind}` }}>
    {text}
  </Box>
);

/**
 * A static, hand-tokenised snippet. Real syntax highlighting will come from the
 * Monaco editor later; this exists so the code palette can be reviewed here.
 */
export const CodeBlock = ({ lines, highlight = [], showLineNumbers = true }: CodeBlockProps) => (
  <Paper variant="sunken" sx={{ overflow: 'hidden', backgroundColor: 'code.bg' }}>
    <Box sx={{ overflowX: 'auto', py: 1.5 }}>
      <Stack component="pre" sx={{ m: 0, typography: 'code', minWidth: 'max-content' }}>
        {lines.map((tokens, index) => (
          <Stack
            key={index}
            direction="row"
            sx={{
              px: 1.5,
              backgroundColor: highlight.includes(index + 1) ? 'code.lineHighlight' : 'transparent',
              borderLeft: 2,
              borderColor: highlight.includes(index + 1) ? 'primary.main' : 'transparent',
            }}
          >
            {showLineNumbers ? (
              <Typography
                component="span"
                variant="code"
                sx={{ color: 'code.gutter', userSelect: 'none', width: 28, textAlign: 'right', mr: 2 }}
              >
                {index + 1}
              </Typography>
            ) : null}
            <Box component="span" sx={{ whiteSpace: 'pre' }}>
              {tokens.map(([text, kind], tokenIndex) => (
                <Fragment key={tokenIndex}>
                  <Token text={text} kind={kind} />
                </Fragment>
              ))}
            </Box>
          </Stack>
        ))}
      </Stack>
    </Box>
  </Paper>
);
