import i18next from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import de from './locales/de.json';
import en from './locales/en.json';
import fr from './locales/fr.json';

/** Everything the UI can be shown in. Order is the order in the language picker. */
export const supportedLanguages = [
  { code: 'en', label: 'English' },
  { code: 'de', label: 'Deutsch' },
  { code: 'fr', label: 'Français' },
] as const;

export type LanguageCode = (typeof supportedLanguages)[number]['code'];

export const defaultNS = 'translation';

export const resources = {
  en: { translation: en },
  de: { translation: de },
  fr: { translation: fr },
} as const;

void i18next
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    defaultNS,
    fallbackLng: 'en',
    supportedLngs: supportedLanguages.map((language) => language.code),
    // Keys are dotted paths that mirror the backend's error codes exactly, so a
    // code like `validation.username.taken` resolves without any translation.
    keySeparator: '.',
    nsSeparator: false,
    interpolation: {
      // React already escapes everything it renders.
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'codeforge.language',
      caches: ['localStorage'],
    },
  });
