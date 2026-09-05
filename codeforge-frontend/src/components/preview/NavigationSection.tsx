import BarChartRounded from '@mui/icons-material/BarChartRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import HistoryRounded from '@mui/icons-material/HistoryRounded';
import HomeRounded from '@mui/icons-material/HomeRounded';
import {
  Box,
  Breadcrumbs,
  Link,
  MenuItem,
  MenuList,
  Pagination,
  Paper,
  Step,
  StepLabel,
  Stepper,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { Block, Section } from './Section';

const interviewSteps = ['Arrays — Easy', 'Trees — Medium', 'Graphs — Hard', 'Results'];

export const NavigationSection = () => (
  <Section
    id="navigation"
    eyebrow="Components"
    title="Navigation"
    description="Tabs use a 2px accent rule rather than a filled pill, menus float on a raised overlay with a
      hairline border, and pagination numerals are monospaced so page controls never reflow as you page."
  >
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Tabs">
        <Box sx={{ borderBottom: 1, borderColor: 'border.subtle' }}>
          <Tabs value={0}>
            <Tab label="Description" />
            <Tab label="Editorial" />
            <Tab label="Solutions" />
            <Tab label="Submissions" />
          </Tabs>
        </Box>
        <Box sx={{ borderBottom: 1, borderColor: 'border.subtle' }}>
          <Tabs value={1} textColor="secondary" indicatorColor="secondary">
            <Tab icon={<BarChartRounded fontSize="small" />} iconPosition="start" label="Stats" />
            <Tab icon={<HistoryRounded fontSize="small" />} iconPosition="start" label="History" />
            <Tab label="Disabled" disabled />
          </Tabs>
        </Box>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Breadcrumbs, pagination & stepper">
        <Breadcrumbs separator={<ChevronRightRounded sx={{ fontSize: 15 }} />}>
          <Link href="#navigation" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <HomeRounded sx={{ fontSize: 15 }} />
            CodeForge
          </Link>
          <Link href="#navigation">Problems</Link>
          <Link href="#navigation">Graphs</Link>
          <Typography variant="body2" sx={{ color: 'text.primary' }}>
            Course Schedule
          </Typography>
        </Breadcrumbs>
        <Pagination count={12} page={3} shape="rounded" />
        <Pagination count={8} page={2} variant="outlined" shape="rounded" color="primary" />
        <Stepper activeStep={1} sx={{ mt: 1 }}>
          {interviewSteps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Menu" hint="rendered inline for inspection">
        <Paper
          variant="outlined"
          sx={{ width: 232, backgroundColor: 'surface.overlay', boxShadow: 8, overflow: 'hidden' }}
        >
          <MenuList>
            <MenuItem selected>Run sample tests</MenuItem>
            <MenuItem>Reset to starter code</MenuItem>
            <MenuItem>Copy as gist</MenuItem>
            <MenuItem disabled>Download submission</MenuItem>
          </MenuList>
        </Paper>
      </Block>
    </Paper>
  </Section>
);
