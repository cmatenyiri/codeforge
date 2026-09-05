import LanguageRounded from '@mui/icons-material/LanguageRounded';
import { InputAdornment, MenuItem, TextField } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { supportedLanguages } from '../i18n';

export const LanguageSwitcher = () => {
  const { i18n, t } = useTranslation();
  const current = supportedLanguages.find((language) => i18n.resolvedLanguage === language.code)?.code ?? 'en';

  return (
    <TextField
      select
      size="small"
      value={current}
      onChange={(event) => {
        void i18n.changeLanguage(event.target.value);
      }}
      aria-label={t('common.language')}
      sx={{ width: 150 }}
      slotProps={{
        input: {
          startAdornment: (
            <InputAdornment position="start">
              <LanguageRounded fontSize="small" />
            </InputAdornment>
          ),
        },
      }}
    >
      {supportedLanguages.map((language) => (
        <MenuItem key={language.code} value={language.code}>
          {language.label}
        </MenuItem>
      ))}
    </TextField>
  );
};
