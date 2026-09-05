/**
 * The avatar catalogue, matching `com.codeforge.domain.Avatar` one-for-one.
 *
 * <p>Artwork is drawn here rather than stored: the backend only records which
 * one was picked, so there is no upload, no object storage and no moderation to
 * worry about, and restyling them ships without touching data.
 */
export const AVATAR_IDS = [
  'FORGE',
  'CIRCUIT',
  'NEBULA',
  'PRISM',
  'VERTEX',
  'CIPHER',
  'LATTICE',
  'PULSE',
  'ORBIT',
  'GLITCH',
] as const;

export type AvatarId = (typeof AVATAR_IDS)[number];

export type AvatarDesign = {
  /** Gradient stops for the tile background. */
  from: string;
  to: string;
  /** Foreground mark, drawn on a 24×24 viewBox. */
  path: string;
};

export const AVATAR_DESIGNS: Record<AvatarId, AvatarDesign> = {
  // Ember — the brand mark.
  FORGE: { from: '#FF7A2F', to: '#FFB067', path: 'M12 3 L18 12 L12 21 L6 12 Z' },
  // Traces and a via.
  CIRCUIT: { from: '#2DD4E6', to: '#12A0B2', path: 'M4 12 H10 M14 12 H20 M12 4 V10 M12 14 V20 M10 10 H14 V14 H10 Z' },
  // Concentric arcs.
  NEBULA: { from: '#7C6BFF', to: '#C792EA', path: 'M12 4 A8 8 0 0 1 20 12 M12 8 A4 4 0 0 1 16 12 M12 12 h0.01' },
  // Split triangle.
  PRISM: { from: '#3DD68C', to: '#7BE8B0', path: 'M12 4 L20 18 H4 Z M12 4 V18' },
  // Wireframe corner.
  VERTEX: { from: '#F5A623', to: '#FFD68F', path: 'M4 20 L12 4 L20 20 Z M4 20 L20 20 M12 4 L12 20' },
  // Key teeth.
  CIPHER: { from: '#FF5C6C', to: '#FFA0A8', path: 'M5 12 H13 M13 8 V16 M16 10 V14 M19 10 V14' },
  // Grid.
  LATTICE: { from: '#4AB3FF', to: '#9BD6FF', path: 'M4 9 H20 M4 15 H20 M9 4 V20 M15 4 V20' },
  // Waveform.
  PULSE: { from: '#E5C07B', to: '#F5A623', path: 'M3 12 H7 L10 5 L14 19 L17 12 H21' },
  // Ellipse and body.
  ORBIT: { from: '#56B6C2', to: '#2DD4E6', path: 'M12 12 m-4 0 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 M3 12 A9 5 0 0 0 21 12' },
  // Offset bars.
  GLITCH: { from: '#C792EA', to: '#7C6BFF', path: 'M4 7 H14 M8 12 H20 M4 17 H13' },
};

export const isAvatarId = (value: string | null | undefined): value is AvatarId =>
  value !== null && value !== undefined && (AVATAR_IDS as readonly string[]).includes(value);
