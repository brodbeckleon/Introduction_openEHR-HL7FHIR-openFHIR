<script lang="ts">
  import { t } from './i18n.svelte';

  /**
   * The three standards, what each is for, and which way the data moves.
   *
   * The README says this in ASCII; a reading is faster than a paragraph, and in the pipeline
   * inspector the box belonging to whoever is acting right now lights up, which turns the same
   * picture into a position indicator.
   */

  type System = 'fhir' | 'openfhir' | 'openehr';

  interface Props {
    /** Lights up one box — the system responsible for the stage being looked at. */
    active?: System | null;
    /** Trims the captions for use above the inspector, where the detail is on screen anyway. */
    compact?: boolean;
    /**
     * Makes the boxes selectable. Given, each one opens the tab where that standard's own artefact
     * can be read: the Observation, the mappings, the template. Left out, the diagram stays a
     * picture — which is what it should be anywhere there is nowhere to go.
     */
    onselect?: (system: System) => void;
  }

  const { active = null, compact = false, onselect }: Props = $props();

  /** Captions are one string with | between the lines, so a translation can rebalance them. */
  const caption = (key: string): string[] => t(key).split('|');

  // A <g> carrying role="button" is reachable and clickable, but unlike a real button it gets no
  // keyboard activation for free.
  function activate(system: System, event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onselect?.(system);
    }
  }
</script>

<figure class="diagram" class:compact>
  <svg
    viewBox="0 0 900 {compact ? 152 : 232}"
    role={onselect ? 'group' : 'img'}
    aria-label={onselect ? t('diagram.alt') : undefined}
    xmlns="http://www.w3.org/2000/svg"
  >
    <title>{t('diagram.alt')}</title>

    <defs>
      <marker
        id="arrow-fhir"
        viewBox="0 0 10 10"
        refX="9"
        refY="5"
        markerWidth="6"
        markerHeight="6"
        orient="auto-start-reverse"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" class="head-fhir" />
      </marker>
      <marker
        id="arrow-openehr"
        viewBox="0 0 10 10"
        refX="9"
        refY="5"
        markerWidth="6"
        markerHeight="6"
        orient="auto-start-reverse"
      >
        <path d="M 0 0 L 10 5 L 0 10 z" class="head-openehr" />
      </marker>
    </defs>

    <!-- Exchange -->
    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
    <!-- The role is only "button" when onselect is given, which the linter cannot evaluate; with
         it, role + tabindex + Enter/Space is the ARIA pattern for a custom button. -->
    <g
      class="node fhir"
      class:lit={active === 'fhir'}
      class:selectable={onselect}
      role={onselect ? 'button' : undefined}
      tabindex={onselect ? 0 : undefined}
      aria-label={onselect ? t('diagram.open.fhir') : undefined}
      onclick={() => onselect?.('fhir')}
      onkeydown={(event) => activate('fhir', event)}
    >
      {#if onselect}<title>{t('diagram.open.fhir')}</title>{/if}
      <rect x="8" y="44" width="236" height="96" rx="10" />
      <text class="name" x="28" y="76">HL7 FHIR</text>
      <text class="role" x="28" y="98">{t('diagram.fhir.role')}</text>
      <text class="artefact" x="28" y="122">Observation · LOINC 40443-4</text>
    </g>

    <!-- Translation -->
    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
    <!-- The role is only "button" when onselect is given, which the linter cannot evaluate; with
         it, role + tabindex + Enter/Space is the ARIA pattern for a custom button. -->
    <g
      class="node openfhir"
      class:lit={active === 'openfhir'}
      class:selectable={onselect}
      role={onselect ? 'button' : undefined}
      tabindex={onselect ? 0 : undefined}
      aria-label={onselect ? t('diagram.open.openfhir') : undefined}
      onclick={() => onselect?.('openfhir')}
      onkeydown={(event) => activate('openfhir', event)}
    >
      {#if onselect}<title>{t('diagram.open.openfhir')}</title>{/if}
      <rect x="332" y="44" width="236" height="96" rx="10" />
      <text class="name" x="352" y="76">openFHIR</text>
      <text class="role" x="352" y="98">{t('diagram.openfhir.role')}</text>
      <text class="artefact" x="352" y="122">FHIR Connect mappings</text>
    </g>

    <!-- Persistence -->
    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
    <!-- The role is only "button" when onselect is given, which the linter cannot evaluate; with
         it, role + tabindex + Enter/Space is the ARIA pattern for a custom button. -->
    <g
      class="node openehr"
      class:lit={active === 'openehr'}
      class:selectable={onselect}
      role={onselect ? 'button' : undefined}
      tabindex={onselect ? 0 : undefined}
      aria-label={onselect ? t('diagram.open.openehr') : undefined}
      onclick={() => onselect?.('openehr')}
      onkeydown={(event) => activate('openehr', event)}
    >
      {#if onselect}<title>{t('diagram.open.openehr')}</title>{/if}
      <rect x="656" y="44" width="236" height="96" rx="10" />
      <text class="name" x="676" y="76">openEHR</text>
      <text class="role" x="676" y="98">{t('diagram.openehr.role')}</text>
      <text class="artefact" x="676" y="122">COMPOSITION · pulse.v2</text>
    </g>

    <!-- Left gap: FHIR travels in and out -->
    <line class="flow fhir-flow" x1="248" y1="74" x2="324" y2="74" marker-end="url(#arrow-fhir)" />
    <line
      class="flow fhir-flow"
      x1="324"
      y1="112"
      x2="248"
      y2="112"
      marker-end="url(#arrow-fhir)"
    />
    <text class="edge" x="286" y="64" text-anchor="middle">{t('diagram.import')}</text>
    <text class="edge" x="286" y="132" text-anchor="middle">{t('diagram.export')}</text>

    <!-- Right gap: openEHR goes to the record and comes back -->
    <line
      class="flow openehr-flow"
      x1="572"
      y1="74"
      x2="648"
      y2="74"
      marker-end="url(#arrow-openehr)"
    />
    <line
      class="flow openehr-flow"
      x1="648"
      y1="112"
      x2="572"
      y2="112"
      marker-end="url(#arrow-openehr)"
    />
    <text class="edge" x="610" y="64" text-anchor="middle">{t('diagram.store')}</text>
    <text class="edge" x="610" y="132" text-anchor="middle">AQL</text>

    {#if !compact}
      <!-- What each one is actually good at. This is the part people come away with. -->
      {#each [['diagram.fhir.caption', 8], ['diagram.openfhir.caption', 332], ['diagram.openehr.caption', 656]] as [key, x] (key)}
        {#each caption(key as string) as line, row (row)}
          <text class="caption" {x} y={176 + row * 20}>{line}</text>
        {/each}
      {/each}
    {/if}
  </svg>
</figure>

<style>
  .diagram {
    margin: 0;
  }

  svg {
    display: block;
    width: 100%;
    height: auto;
  }

  .node rect {
    fill: var(--surface-1);
    stroke: var(--border);
    stroke-width: 1;
  }

  /* The same two colours the JSON panels use: blue is FHIR, orange is openEHR. */
  .node.fhir rect {
    stroke: var(--series-resting);
  }

  .node.openehr rect {
    stroke: var(--series-reading);
  }

  .node.lit rect {
    fill: var(--band-fill);
  }

  .node.selectable {
    cursor: pointer;
  }

  .node.selectable rect {
    transition:
      fill 120ms ease,
      stroke-width 120ms ease;
  }

  /* --gridline rather than --band-fill: the latter is both the lit state's fill and the blue that
     means FHIR here, so hovering would read as "active" and, on the neutral openFHIR box, as "this
     one is FHIR". A grey wash carries no such meaning and is visible in both themes. */
  .node.selectable:hover rect,
  .node.selectable:focus-visible rect {
    fill: var(--gridline);
    stroke-width: 2;
  }

  /* The browser's own focus ring lands on the group's bounding box rather than the rounded
     rectangle, so the ring is drawn on the shape instead. */
  .node.selectable:focus-visible {
    outline: none;
  }

  .node.selectable:focus-visible rect {
    stroke-dasharray: 4 3;
  }

  .name {
    font-size: 19px;
    font-weight: 600;
    fill: var(--text-primary);
  }

  .node.fhir .name {
    fill: var(--series-resting);
  }

  .node.openehr .name {
    fill: var(--series-reading);
  }

  .role {
    font-size: 13px;
    fill: var(--text-secondary);
  }

  .artefact {
    font-size: 12px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    fill: var(--text-muted);
  }

  .flow {
    stroke-width: 1.5;
    fill: none;
  }

  .fhir-flow {
    stroke: var(--series-resting);
  }

  .openehr-flow {
    stroke: var(--series-reading);
  }

  .head-fhir {
    fill: var(--series-resting);
  }

  .head-openehr {
    fill: var(--series-reading);
  }

  .edge {
    font-size: 11.5px;
    fill: var(--text-muted);
    letter-spacing: 0.03em;
  }

  .caption {
    font-size: 12.5px;
    fill: var(--text-secondary);
  }
</style>
