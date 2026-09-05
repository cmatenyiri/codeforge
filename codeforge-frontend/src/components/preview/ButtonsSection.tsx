import AddRounded from '@mui/icons-material/AddRounded';
import CloudUploadRounded from '@mui/icons-material/CloudUploadRounded';
import ContentCopyRounded from '@mui/icons-material/ContentCopyRounded';
import DeleteRounded from '@mui/icons-material/DeleteRounded';
import FormatListBulletedRounded from '@mui/icons-material/FormatListBulletedRounded';
import MoreVertRounded from '@mui/icons-material/MoreVertRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import RefreshRounded from '@mui/icons-material/RefreshRounded';
import SettingsRounded from '@mui/icons-material/SettingsRounded';
import ViewColumnRounded from '@mui/icons-material/ViewColumnRounded';
import {
  Box,
  Button,
  ButtonGroup,
  Fab,
  IconButton,
  Paper,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
} from '@mui/material';
import { Block, Row, Section } from './Section';

const colors = ['primary', 'secondary', 'success', 'warning', 'error', 'info'] as const;

export const ButtonsSection = () => (
  <Section
    id="buttons"
    eyebrow="Components"
    title="Actions"
    description="Buttons are compact and square-shouldered. Contained fills carry a one-pixel specular edge so they
      don't read as flat rectangles, and a custom `soft` variant covers tinted toolbar actions."
  >
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Variants">
        <Row>
          <Button variant="contained" startIcon={<PlayArrowRounded />}>
            Run code
          </Button>
          <Button variant="soft" startIcon={<CloudUploadRounded />}>
            Submit
          </Button>
          <Button variant="outlined">Reset editor</Button>
          <Button variant="text">Skip problem</Button>
          <Button variant="contained" disabled>
            Disabled
          </Button>
        </Row>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Colors">
        <Row>
          {colors.map((color) => (
            <Button key={color} color={color}>
              {color}
            </Button>
          ))}
        </Row>
        <Row>
          {colors.map((color) => (
            <Button key={color} variant="soft" color={color}>
              {color}
            </Button>
          ))}
        </Row>
        <Row>
          {colors.map((color) => (
            <Button key={color} variant="outlined" color={color}>
              {color}
            </Button>
          ))}
        </Row>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Sizes">
        <Row>
          <Button size="small">Small</Button>
          <Button size="medium">Medium</Button>
          <Button size="large">Large</Button>
          <Button size="small" variant="outlined" startIcon={<AddRounded />}>
            New problem
          </Button>
          <Button size="large" variant="soft" color="success" startIcon={<PlayArrowRounded />}>
            Start interview
          </Button>
        </Row>
      </Block>
    </Paper>

    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Icon buttons, groups & segmented controls">
        <Row>
          <Tooltip title="Copy solution">
            <IconButton>
              <ContentCopyRounded fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Reset">
            <IconButton>
              <RefreshRounded fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Editor settings">
            <IconButton>
              <SettingsRounded fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete draft">
            <IconButton color="error">
              <DeleteRounded fontSize="small" />
            </IconButton>
          </Tooltip>
          <IconButton>
            <MoreVertRounded fontSize="small" />
          </IconButton>
          <Box sx={{ width: 12 }} />
          <ButtonGroup variant="outlined">
            <Button>Java</Button>
            <Button>Python</Button>
            <Button>TypeScript</Button>
          </ButtonGroup>
          <ToggleButtonGroup value="split" exclusive size="small">
            <ToggleButton value="list">
              <FormatListBulletedRounded fontSize="small" sx={{ mr: 0.75 }} />
              List
            </ToggleButton>
            <ToggleButton value="split">
              <ViewColumnRounded fontSize="small" sx={{ mr: 0.75 }} />
              Split
            </ToggleButton>
          </ToggleButtonGroup>
          <Fab size="small" color="primary">
            <AddRounded />
          </Fab>
        </Row>
      </Block>
    </Paper>
  </Section>
);
