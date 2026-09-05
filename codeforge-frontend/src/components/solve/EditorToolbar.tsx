import CloudUploadRounded from '@mui/icons-material/CloudUploadRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import RefreshRounded from '@mui/icons-material/RefreshRounded';
import SettingsRounded from '@mui/icons-material/SettingsRounded';
import {
  Box,
  Button,
  CircularProgress,
  Divider,
  FormControlLabel,
  IconButton,
  MenuItem,
  Popover,
  Select,
  Stack,
  Switch,
  Tooltip,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type Language } from '../../api/types';
import { FONT_SIZES, TAB_SIZES, type EditorSettings } from './editor-settings';
import { LANGUAGE_LABEL } from './verdict';

type EditorToolbarProps = {
  language: Language;
  languages: Language[];
  onLanguageChange: (language: Language) => void;
  settings: EditorSettings;
  onSettingsChange: (settings: EditorSettings) => void;
  onReset: () => void;
  onRun: () => void;
  running: boolean;
  canRun: boolean;
};

const SettingsPopover = ({
  settings,
  onChange,
}: {
  settings: EditorSettings;
  onChange: (settings: EditorSettings) => void;
}) => {
  const { t } = useTranslation();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  return (
    <>
      <Tooltip title={t('solve.editorSettings')}>
        <IconButton
          size="small"
          aria-label={t('solve.editorSettings')}
          onClick={(event) => {
            setAnchor(event.currentTarget);
          }}
        >
          <SettingsRounded fontSize="small" />
        </IconButton>
      </Tooltip>

      <Popover
        open={anchor !== null}
        anchorEl={anchor}
        onClose={() => {
          setAnchor(null);
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Stack spacing={2} sx={{ p: 2, width: 240 }}>
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="body2">{t('solve.fontSize')}</Typography>
            <Select
              size="small"
              value={settings.fontSize}
              onChange={(event) => {
                onChange({ ...settings, fontSize: Number(event.target.value) });
              }}
              sx={{ width: 84 }}
            >
              {FONT_SIZES.map((size) => (
                <MenuItem key={size} value={size}>
                  {size}px
                </MenuItem>
              ))}
            </Select>
          </Stack>

          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="body2">{t('solve.tabSize')}</Typography>
            <Select
              size="small"
              value={settings.tabSize}
              onChange={(event) => {
                onChange({ ...settings, tabSize: Number(event.target.value) });
              }}
              sx={{ width: 84 }}
            >
              {TAB_SIZES.map((size) => (
                <MenuItem key={size} value={size}>
                  {size}
                </MenuItem>
              ))}
            </Select>
          </Stack>

          <FormControlLabel
            control={
              <Switch
                size="small"
                checked={settings.wordWrap}
                onChange={(event) => {
                  onChange({ ...settings, wordWrap: event.target.checked });
                }}
              />
            }
            label={<Typography variant="body2">{t('solve.wordWrap')}</Typography>}
          />

          <FormControlLabel
            control={
              <Switch
                size="small"
                checked={settings.minimap}
                onChange={(event) => {
                  onChange({ ...settings, minimap: event.target.checked });
                }}
              />
            }
            label={<Typography variant="body2">{t('solve.minimap')}</Typography>}
          />
        </Stack>
      </Popover>
    </>
  );
};

export const EditorToolbar = ({
  language,
  languages,
  onLanguageChange,
  settings,
  onSettingsChange,
  onReset,
  onRun,
  running,
  canRun,
}: EditorToolbarProps) => {
  const { t } = useTranslation();

  return (
    <Stack
      direction="row"
      spacing={1}
      sx={{
        px: 1.5,
        py: 1,
        alignItems: 'center',
        borderBottom: 1,
        borderColor: 'border.subtle',
        flexShrink: 0,
      }}
    >
      <Select
        size="small"
        value={language}
        onChange={(event) => {
          onLanguageChange(event.target.value as Language);
        }}
        // Not `solve.language`: the header already has a control labelled
        // "Language" for the interface language, and two identically labelled
        // comboboxes on one page are indistinguishable to a screen reader.
        inputProps={{ 'aria-label': t('solve.programmingLanguage') }}
        sx={{ width: 140 }}
      >
        {languages.map((option) => (
          <MenuItem key={option} value={option}>
            {LANGUAGE_LABEL[option]}
          </MenuItem>
        ))}
      </Select>

      <Box sx={{ flex: 1 }} />

      <Tooltip title={t('solve.resetCode')}>
        {/* The span is what lets a disabled button still show a tooltip, but it
            is also what Tooltip labels — so the button needs its own name. */}
        <span>
          <IconButton size="small" onClick={onReset} disabled={running} aria-label={t('solve.resetCode')}>
            <RefreshRounded fontSize="small" />
          </IconButton>
        </span>
      </Tooltip>

      <SettingsPopover settings={settings} onChange={onSettingsChange} />

      <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

      <Tooltip title={t('solve.runHint')}>
        <span>
          <Button
            size="small"
            variant="soft"
            startIcon={running ? <CircularProgress size={14} color="inherit" /> : <PlayArrowRounded />}
            onClick={onRun}
            disabled={running || !canRun}
            aria-label={t('solve.run')}
          >
            {t('solve.run')}
          </Button>
        </span>
      </Tooltip>

      {/* Submitting judges the hidden cases and records a verdict; that is a
          separate feature and is deliberately not wired up yet. */}
      <Tooltip title={t('solve.submitComingSoon')}>
        <span>
          <Button size="small" color="success" startIcon={<CloudUploadRounded />} disabled>
            {t('solve.submit')}
          </Button>
        </span>
      </Tooltip>
    </Stack>
  );
};
