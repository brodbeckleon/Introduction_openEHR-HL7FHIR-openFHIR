<script lang="ts">
  /**
   * The small set of icons this application uses, drawn inline.
   *
   * Inline rather than a sprite or a library: there are a dozen of them, they are a few hundred
   * bytes each, and they inherit `currentColor` so a button that changes colour changes its icon
   * with it. A dependency for this would cost more than it saves.
   *
   * They are decorative by default — every icon here sits in a control that also carries a label,
   * whether visible or as `aria-label` — so they are hidden from assistive technology.
   */

  export type IconName =
    | 'copy'
    | 'check'
    | 'download'
    | 'upload'
    | 'external'
    | 'pencil'
    | 'reset'
    | 'play'
    | 'compass'
    | 'close'
    | 'left'
    | 'right'
    | 'sparkle'
    | 'info';

  interface Props {
    name: IconName;
    /** Matches the surrounding text size by default. */
    size?: number;
  }

  const { name, size = 15 }: Props = $props();

  /** Stroke paths on a 24×24 grid, so they line up with text at any size. */
  const PATHS: Record<IconName, string[]> = {
    copy: [
      'M9 9h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V11a2 2 0 0 1 2-2z',
      'M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1',
    ],
    check: ['M20 6 9 17l-5-5'],
    download: ['M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4', 'm7 10 5 5 5-5', 'M12 15V3'],
    upload: ['M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4', 'm17 8-5-5-5 5', 'M12 3v12'],
    external: [
      'M15 3h6v6',
      'M10 14 21 3',
      'M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6',
    ],
    pencil: ['M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z'],
    reset: ['M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8', 'M3 3v5h5'],
    play: ['M6 4.5v15l13-7.5z'],
    compass: ['M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z', 'm16.2 7.8-2.1 6.4-6.3 2.1 2.1-6.4z'],
    close: ['M18 6 6 18', 'm6 6 12 12'],
    left: ['M19 12H5', 'm12 19-7-7 7-7'],
    right: ['M5 12h14', 'm12 5 7 7-7 7'],
    info: ['M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z', 'M12 16v-4', 'M12 8h.01'],
    sparkle: [
      'M12 3v6',
      'M12 15v6',
      'M3 12h6',
      'M15 12h6',
      'm5.6 5.6 4.2 4.2',
      'm14.2 14.2 4.2 4.2',
      'm18.4 5.6-4.2 4.2',
      'm9.8 14.2-4.2 4.2',
    ],
  };

  /** `play` reads as a solid triangle; the rest are outlines. */
  const FILLED = new Set<IconName>(['play']);
</script>

<svg
  class="icon"
  width={size}
  height={size}
  viewBox="0 0 24 24"
  fill={FILLED.has(name) ? 'currentColor' : 'none'}
  stroke={FILLED.has(name) ? 'none' : 'currentColor'}
  stroke-width="2"
  stroke-linecap="round"
  stroke-linejoin="round"
  aria-hidden="true"
  focusable="false"
>
  {#each PATHS[name] as d (d)}
    <path {d} />
  {/each}
</svg>

<style>
  .icon {
    flex: none;
    /* Nudged so it centres against lowercase text rather than sitting on the baseline. */
    vertical-align: -0.16em;
  }
</style>
