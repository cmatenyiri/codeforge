import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor/editor/editor.api';
// Side-effecting, and separate from the line above because `editor.main` ships
// no type declarations: it re-exports the very same module instance, so the
// typed handle comes from `editor.api` while this registers the contributions.
import 'monaco-editor/editor/editor.main';
import EditorWorker from 'monaco-editor/editor/editor.worker?worker';
import {
  javascriptDefaults,
  ModuleResolutionKind,
  ScriptTarget,
  typescriptDefaults,
  type CompilerOptions,
} from 'monaco-editor/languages/features/typescript/register';
import TypeScriptWorker from 'monaco-editor/languages/features/typescript/ts.worker?worker';
import { JAVA_COMPLETIONS, PYTHON_COMPLETIONS } from './completions';
import { monacoThemes } from './theme';

/**
 * Bootstraps Monaco once, before any editor mounts.
 *
 * <p>`editor.main` has to be imported, not just `editor.api`: the API module is
 * only the core editor and carries none of the contributions, so there is no
 * suggest widget at all and completion silently does nothing. `editor.main` also
 * registers every language Monaco ships, which looks wasteful next to the four
 * that can actually be run; measured, it is not. Importing the API plus only the
 * four grammars produced a solving-page payload within about 20 kB gzipped of
 * this one, because the cost is the editor core and the TypeScript service, not
 * the grammars.
 *
 * <p>Importing this module has the side effect of configuring the global Monaco
 * instance, so it is imported by the editor component rather than by the app
 * shell — that keeps Monaco inside the lazily-loaded solving-page chunk instead
 * of the initial download.
 *
 * <p>By default `@monaco-editor/react` fetches Monaco from a CDN at runtime.
 * The `loader.config` call at the bottom points it at the copy bundled here
 * instead, which is what makes the editor work offline and pins the version to
 * the one in package.json.
 */

/**
 * Only the TypeScript worker is wired up, because only it is reachable.
 *
 * <p>It is the language service behind both JavaScript and TypeScript
 * completion — what makes `nums.` list array methods rather than words that
 * happen to appear in the file. Java and Python have no worker in Monaco at all:
 * they get syntax highlighting from a Monarch grammar, plus the keyword
 * completions registered below. Anything else (the css/html/json services
 * `editor.main` also registers) falls back to the plain editor worker, and is
 * never asked for, since no model here is ever one of those languages.
 */
const workers: Record<string, new () => Worker> = {
  typescript: TypeScriptWorker,
  javascript: TypeScriptWorker,
};

globalThis.MonacoEnvironment = {
  getWorker(_workerId: string, label: string) {
    const Worker = workers[label] ?? EditorWorker;
    return new Worker();
  },
};

for (const [name, data] of Object.entries(monacoThemes)) {
  monaco.editor.defineTheme(name, data);
}

/**
 * Kept deliberately close to what the judge runs (TypeScript 3.7 targeting
 * ES2017), so the editor does not accept syntax the judge will reject. It is not
 * an exact match — Monaco ships a far newer compiler — but the library surface
 * is the part that misleads, and this pins that.
 */
const compilerOptions: CompilerOptions = {
  target: ScriptTarget.ES2017,
  lib: ['es2017', 'dom'],
  allowNonTsExtensions: true,
  moduleResolution: ModuleResolutionKind.NodeJs,
  noEmit: true,
  // What makes the JavaScript stub's JSDoc mean something. Without it the
  // service does not analyse the file at all and `nums.` falls back to
  // word-based suggestion — every identifier already in the buffer, and not one
  // array method. `checkJs` stays off: the types are there to inform completion,
  // not to underline a solver's JavaScript as if it were TypeScript.
  allowJs: true,
  checkJs: false,
  // `strict` off matches the judge's bare `tsc` invocation, and keeps the stub
  // from being underlined before a single character has been typed.
  strict: false,
};

for (const defaults of [typescriptDefaults, javascriptDefaults]) {
  defaults.setCompilerOptions(compilerOptions);
  defaults.setEagerModelSync(true);
  defaults.setDiagnosticsOptions({
    noSemanticValidation: false,
    noSyntaxValidation: false,
  });
}

/**
 * Java and Python get a keyword list, because Monaco gives them nothing else.
 *
 * <p>Without a language server the alternative is word-based suggestion, which
 * can only ever offer identifiers already typed somewhere in the file — so
 * `for` does not complete until you have written a loop. Real completion for
 * these two needs a language server (jdtls, Pyright) behind a WebSocket; this is
 * the useful floor beneath that.
 */
for (const [language, completions] of [
  ['java', JAVA_COMPLETIONS],
  ['python', PYTHON_COMPLETIONS],
] as const) {
  monaco.languages.registerCompletionItemProvider(language, {
    provideCompletionItems(model, position) {
      const word = model.getWordUntilPosition(position);
      const range: monaco.IRange = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      };

      return {
        suggestions: completions.map((completion) => ({
          label: completion.label,
          kind: completion.snippet
            ? monaco.languages.CompletionItemKind.Snippet
            : monaco.languages.CompletionItemKind.Keyword,
          insertText: completion.insertText ?? completion.label,
          insertTextRules: completion.snippet
            ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
            : undefined,
          detail: completion.detail,
          range,
        })),
      };
    },
  });
}

loader.config({ monaco });
