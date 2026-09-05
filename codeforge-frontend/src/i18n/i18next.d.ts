/* eslint-disable @typescript-eslint/consistent-type-definitions --
   i18next is configured through declaration merging, which needs `interface`. */
import { type defaultNS, type resources } from './index';

/**
 * Makes every `t('…')` call key-checked against `en.json`, so a renamed or
 * misspelled key is a compile error rather than a string that silently renders
 * as itself.
 */
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: typeof defaultNS;
    resources: (typeof resources)['en'];
  }
}
