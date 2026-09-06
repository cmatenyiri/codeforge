import { Box } from '@mui/material';
import Markdown from 'react-markdown';

/**
 * Renders authored Markdown with the app's typography.
 *
 * <p>Shared by the problem statement and the editorial, which is why it styles
 * headings and fenced blocks as well as prose: a statement uses neither, but a
 * walkthrough is structured writing and reads as a wall of text without them.
 *
 * <p>Only the elements the authored content actually uses are styled; anything
 * else falls through to sensible browser defaults.
 */
export const MarkdownBody = ({ children }: { children: string }) => (
  <Box
    sx={{
      typography: 'body1',
      color: 'text.secondary',
      '& p': { m: 0, mb: 1.5 },
      '& p:last-child': { mb: 0 },
      '& strong': { color: 'text.primary', fontWeight: 650 },
      '& ul, & ol': { pl: 2.5, m: 0, mb: 1.5 },
      '& li': { mb: 0.5 },
      '& h3': { typography: 'h5', color: 'text.primary', mt: 3, mb: 1 },
      '& h3:first-of-type': { mt: 0 },
      '& h4': { typography: 'subtitle1', color: 'text.primary', mt: 2.5, mb: 1 },
      '& code': {
        typography: 'code',
        px: 0.5,
        py: '0.1em',
        borderRadius: 0.75,
        backgroundColor: 'surface.sunken',
        border: 1,
        borderColor: 'border.subtle',
        color: 'text.primary',
      },
      // A fenced block is already a panel, so it drops the inline chrome that
      // would otherwise draw a border around every line inside it.
      '& pre': {
        m: 0,
        mb: 1.5,
        p: 1.5,
        borderRadius: 1.5,
        border: 1,
        borderColor: 'border.subtle',
        backgroundColor: 'surface.sunken',
        overflowX: 'auto',
      },
      '& pre code': { border: 0, p: 0, backgroundColor: 'transparent', borderRadius: 0 },
    }}
  >
    <Markdown>{children}</Markdown>
  </Box>
);
