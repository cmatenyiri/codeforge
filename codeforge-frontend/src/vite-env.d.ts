/* eslint-disable @typescript-eslint/consistent-type-definitions --
   Vite's env typing is done through declaration merging, which needs `interface`. */
interface ImportMetaEnv {
  /** Backend origin. Defaults to http://localhost:8080 when unset. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
