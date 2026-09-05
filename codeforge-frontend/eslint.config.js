import eslint from '@eslint/js';
import prettierConfig from 'eslint-config-prettier';
import { importX } from 'eslint-plugin-import-x';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import prettierPlugin from 'eslint-plugin-prettier';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import unicorn from 'eslint-plugin-unicorn';
import { defineConfig } from 'eslint/config';
import { configs as tseslintConfigs } from 'typescript-eslint';

export default defineConfig(
  // -------------------------------------------------------------------------
  // Common configuration for all files
  // -------------------------------------------------------------------------

  // Global ignores (replaces .eslintignore)
  {
    ignores: [
      'node_modules/',
      'dist/',
      'coverage/',
      '.next/',
      'next-env.d.ts',
      'docs/',
      '.idea/',
      '.vscode/',
    ],
  },

  // -------------------------------------------------------------------------
  // Base JS recommended rules
  // -------------------------------------------------------------------------
  eslint.configs.recommended,
  {
    rules: {
      semi: ['error', 'always'],
      quotes: ['error', 'single', { avoidEscape: true }],
      'max-len': ['error', { code: 120, ignoreStrings: true, ignoreTemplateLiterals: true, ignoreComments: true }],
      'max-classes-per-file': ['error', 5],
      'no-console': 'off',
      'linebreak-style': 'off',
      'no-underscore-dangle': ['error', { allow: ['_id'] }],
      'max-nested-callbacks': ['error', 4],
      'max-params': ['error', 8],
      'max-depth': ['error', 5],
      'max-lines-per-function': ['error', 200],
      complexity: ['error', 20],
      'class-methods-use-this': 'off',
      'arrow-parens': ['error', 'always'],
      'require-await': 'error',
      'no-param-reassign': 'off',
      'no-plusplus': 'off',
      'lines-between-class-members': ['error', 'always', { exceptAfterSingleLine: true }],
      'no-trailing-spaces': ['error', { skipBlankLines: true, ignoreComments: true }],
      camelcase: ['error', { properties: 'never' }],
      'spaced-comment': 'error',
      'no-duplicate-imports': 'error',
      'arrow-body-style': 'off',
    },
  },

  // -------------------------------------------------------------------------
  // TypeScript strict + stylistic rules (applied to TS files)
  // -------------------------------------------------------------------------
  ...tseslintConfigs.strictTypeChecked,
  ...tseslintConfigs.stylisticTypeChecked,
  {
    rules: {
      // ── typescript-eslint ────────────────────────────────────────────────
      '@typescript-eslint/consistent-type-definitions': ['error', 'type'],
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'no-type-imports', fixStyle: 'inline-type-imports' },
      ],
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-confusing-void-expression': [
        'error',
        { ignoreArrowShorthand: true, ignoreVoidOperator: true },
      ],
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-floating-promises': ['error', { ignoreIIFE: true, ignoreVoid: true }],
      '@typescript-eslint/no-meaningless-void-operator': 'off',
      '@typescript-eslint/no-misused-promises': ['error', { checksVoidReturn: false }],
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/no-unnecessary-condition': 'off',
      '@typescript-eslint/no-unnecessary-type-parameters': 'off',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      '@typescript-eslint/no-use-before-define': 'error',
      '@typescript-eslint/prefer-nullish-coalescing': 'error',
      '@typescript-eslint/prefer-optional-chain': 'error',
      '@typescript-eslint/restrict-template-expressions': 'off',
      '@typescript-eslint/switch-exhaustiveness-check': 'error',
    },
  },

  // -------------------------------------------------------------------------
  // Unicorn — modern JS idioms
  // -------------------------------------------------------------------------
  unicorn.configs.recommended,
  {
    rules: {
      /// ── eslint-plugin-unicorn ────────────────────────────────────────────
      // Enforce kebab-case filenames (standard Node.js convention)
      'unicorn/filename-case': ['error', { case: 'kebabCase' }],
      // Prefer for...of over forEach
      'unicorn/no-array-for-each': 'error',
      // These unicorn rules conflict with our style or typescript-eslint equivalents
      'unicorn/no-null': 'off', // TypeScript APIs use null legitimately
      'unicorn/prevent-abbreviations': 'off', // Too noisy for a codebase with established names
      'unicorn/no-negated-condition': 'off', // Sometimes clearer to negate a condition than to invert the whole logic
      'unicorn/no-array-reduce': 'off', // Sometimes reduce is clearer than a for loop or map+filter+etc.
      'unicorn/consistent-function-scoping': ['error', { checkArrowFunctions: false }], // Sometimes clearer to define a helper function inside another function, especially if it's only used there
    },
  },

  // import-x — module graph correctness and ordering
  // Cast needed: PluginFlatConfig.languageOptions is narrower than ConfigWithExtends expects
  /** @type {any} */ (importX.flatConfigs.recommended),
  /** @type {any} */ (importX.flatConfigs.typescript),
  {
    rules: {
      // ── eslint-plugin-import-x ───────────────────────────────────────────
      'import-x/no-unresolved': 'error',
      'import-x/no-named-as-default-member': 'off',
      'import-x/prefer-default-export': 'off',
      'import-x/no-extraneous-dependencies': 'error',
      // No duplicate import statements for the same module
      'import-x/no-duplicates': 'error',
      // Detect circular dependencies
      'import-x/no-cycle': 'error',
      // Enforce consistent import ordering
      'import-x/order': [
        'error',
        {
          groups: [
            ['builtin', 'external', 'internal'],
            ['parent', 'sibling'],
          ],
          'newlines-between': 'never',
          alphabetize: { order: 'ignore', caseInsensitive: true },
        },
      ],
    },
  },

  // Prettier integration — disables conflicting ESLint formatting rules
  // and adds prettier/prettier as an ESLint rule
  prettierConfig,
  {
    plugins: {
      prettier: prettierPlugin,
    },
    rules: {
      'prettier/prettier': 'error',
    },
  },

  // -------------------------------------------------------------------------
  // Common globals for all TS files (e.g. Atomics, SharedArrayBuffer)
  // -------------------------------------------------------------------------
  {
    languageOptions: {
      globals: {
        Atomics: 'readonly',
        SharedArrayBuffer: 'readonly',
      },
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },

  // -------------------------------------------------------------------------
  // JSX/TSX and React-level TS files: browser React environment
  // -------------------------------------------------------------------------
  {
    name: 'tsx-files',
    files: [
      // react files
      '**/*.tsx',
      '**/*.jsx',
    ],
    extends: [
      // react configs
      react.configs.flat.recommended,
      react.configs.flat['jsx-runtime'],
    ],
    plugins: {
      'react-hooks': reactHooks,
      'jsx-a11y': jsxA11y,
    },
    languageOptions: {
      globals: {
        window: 'readonly',
        document: 'readonly',
        navigator: 'readonly',
        console: 'readonly',
        fetch: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        URL: 'readonly',
        URLSearchParams: 'readonly',
        FormData: 'readonly',
        Blob: 'readonly',
        File: 'readonly',
        FileReader: 'readonly',
        Event: 'readonly',
        CustomEvent: 'readonly',
        HTMLElement: 'readonly',
        HTMLInputElement: 'readonly',
        HTMLButtonElement: 'readonly',
        HTMLFormElement: 'readonly',
        HTMLSelectElement: 'readonly',
        HTMLTextAreaElement: 'readonly',
        Element: 'readonly',
        Node: 'readonly',
        NodeList: 'readonly',
        MutationObserver: 'readonly',
        IntersectionObserver: 'readonly',
        ResizeObserver: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        location: 'readonly',
        history: 'readonly',
        alert: 'readonly',
        confirm: 'readonly',
        prompt: 'readonly',
        Atomics: 'readonly',
        SharedArrayBuffer: 'readonly',
      },
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
        ecmaFeatures: { jsx: true },
      },
    },
    settings: {
      react: { version: 'detect' },
    },
    rules: {
      // react-hooks recommended rules
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'off',

      // jsx-a11y
      'jsx-a11y/click-events-have-key-events': 'off',

      // react
      'react/destructuring-assignment': 'off',
      'react/jsx-no-useless-fragment': 'off',
      'react/jsx-props-no-spreading': 'off',
      'react/react-in-jsx-scope': 'off',
      'react/require-default-props': 'off',
    },
  },

  // ------------------------------------------------
  // JSX/TSX files exclusively: file-name convention
  // ------------------------------------------------
  {
    // react files
    files: ['**/*.tsx', '**/*.jsx'],
    rules: {
      'unicorn/filename-case': [
        'error',
        {
          case: 'pascalCase',
          ignore: ['page.tsx', 'layout.tsx', 'not-found.tsx'],
        },
      ],
    },
  },
);
