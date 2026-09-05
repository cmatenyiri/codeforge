/** Editor preferences the solver can change from the toolbar. */
export type EditorSettings = {
  fontSize: number;
  tabSize: number;
  minimap: boolean;
  wordWrap: boolean;
};

export const DEFAULT_EDITOR_SETTINGS: EditorSettings = {
  fontSize: 14,
  // Four matches the generated Java and Python stubs; the ECMAScript ones use
  // two, but a single setting the solver controls beats silently disagreeing
  // with whatever they type next.
  tabSize: 4,
  minimap: false,
  wordWrap: false,
};

export const FONT_SIZES = [12, 13, 14, 16, 18, 20] as const;

export const TAB_SIZES = [2, 4, 8] as const;
