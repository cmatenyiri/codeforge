import SearchRounded from '@mui/icons-material/SearchRounded';
import {
  Autocomplete,
  Checkbox,
  FormControl,
  FormControlLabel,
  FormGroup,
  FormLabel,
  Grid,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Radio,
  RadioGroup,
  Rating,
  Select,
  Slider,
  Stack,
  Switch,
  TextField,
} from '@mui/material';
import { Block, Section } from './Section';

const tags = ['Arrays', 'Hash Table', 'Two Pointers', 'Dynamic Programming', 'Graphs', 'Trees', 'SQL', 'Strings'];

const TextInputs = () => (
  <Grid container spacing={2}>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <TextField label="Email" placeholder="you@example.com" fullWidth defaultValue="" />
    </Grid>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <TextField label="Password" type="password" fullWidth defaultValue="hunter2hunter2" />
    </Grid>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <TextField
        label="Search problems"
        placeholder="Two Sum"
        fullWidth
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchRounded fontSize="small" />
              </InputAdornment>
            ),
          },
        }}
      />
    </Grid>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <TextField label="Handle" defaultValue="taken_name" error helperText="That handle is already in use." fullWidth />
    </Grid>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <TextField label="Time limit" defaultValue="40" helperText="Minutes" fullWidth />
    </Grid>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <TextField label="Locked field" defaultValue="cannot edit" disabled fullWidth />
    </Grid>
    <Grid size={{ xs: 12, md: 8 }}>
      <TextField label="Problem description" multiline minRows={3} fullWidth defaultValue={'Given an array `nums`…'} />
    </Grid>
    <Grid size={{ xs: 12, md: 4 }}>
      <TextField label="Filled variant" variant="filled" fullWidth defaultValue="Alternate input surface" />
    </Grid>
  </Grid>
);

const Selects = () => (
  <Grid container spacing={2}>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <FormControl fullWidth>
        <InputLabel id="difficulty-label">Difficulty</InputLabel>
        <Select labelId="difficulty-label" label="Difficulty" value="medium">
          <MenuItem value="easy">Easy</MenuItem>
          <MenuItem value="medium">Medium</MenuItem>
          <MenuItem value="hard">Hard</MenuItem>
        </Select>
      </FormControl>
    </Grid>
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <FormControl fullWidth>
        <InputLabel id="language-label">Language</InputLabel>
        <Select labelId="language-label" label="Language" value="java">
          <MenuItem value="java">Java</MenuItem>
          <MenuItem value="python">Python</MenuItem>
          <MenuItem value="javascript">JavaScript</MenuItem>
          <MenuItem value="typescript">TypeScript</MenuItem>
        </Select>
      </FormControl>
    </Grid>
    <Grid size={{ xs: 12, md: 4 }}>
      <Autocomplete
        multiple
        options={tags}
        defaultValue={[tags[0], tags[3]]}
        renderInput={(params) => <TextField {...params} label="Tags" placeholder="Add tag" />}
      />
    </Grid>
  </Grid>
);

const Toggles = () => (
  <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
    <FormControl>
      <FormLabel sx={{ mb: 0.5 }}>Show me</FormLabel>
      <FormGroup>
        <FormControlLabel control={<Checkbox defaultChecked />} label="Solved problems" />
        <FormControlLabel control={<Checkbox />} label="Attempted problems" />
        <FormControlLabel control={<Checkbox indeterminate />} label="Premium only" />
        <FormControlLabel control={<Checkbox disabled />} label="Archived" disabled />
      </FormGroup>
    </FormControl>
    <FormControl>
      <FormLabel sx={{ mb: 0.5 }}>Editor theme</FormLabel>
      <RadioGroup defaultValue="forge-dark">
        <FormControlLabel value="forge-dark" control={<Radio />} label="Forge Dark" />
        <FormControlLabel value="forge-light" control={<Radio />} label="Forge Light" />
        <FormControlLabel value="high-contrast" control={<Radio />} label="High contrast" />
      </RadioGroup>
    </FormControl>
    <FormControl>
      <FormLabel sx={{ mb: 0.5 }}>Preferences</FormLabel>
      <FormGroup>
        <FormControlLabel control={<Switch defaultChecked />} label="Auto-run sample tests" />
        <FormControlLabel control={<Switch />} label="Vim keybindings" />
        <FormControlLabel control={<Switch defaultChecked />} label="Show hints after 10 min" />
      </FormGroup>
    </FormControl>
  </Stack>
);

export const FormsSection = () => (
  <Section
    id="forms"
    eyebrow="Components"
    title="Inputs"
    description="Inputs sit in a recessed surface rather than floating on the canvas, so a form reads as a set of
      wells cut into the panel. Focus is a one-pixel accent border plus a soft ring — visible, never shouty."
  >
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Text fields">
        <TextInputs />
      </Block>
    </Paper>
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Selects & autocomplete">
        <Selects />
      </Block>
    </Paper>
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Selection controls">
        <Toggles />
      </Block>
    </Paper>
    <Paper variant="outlined" sx={{ p: 2.5 }}>
      <Block label="Ranges">
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={5} sx={{ alignItems: { md: 'center' } }}>
          <Slider defaultValue={62} valueLabelDisplay="auto" sx={{ maxWidth: 260 }} />
          <Slider defaultValue={[20, 70]} valueLabelDisplay="auto" color="secondary" sx={{ maxWidth: 260 }} />
          <Rating defaultValue={4} />
        </Stack>
      </Block>
    </Paper>
  </Section>
);
