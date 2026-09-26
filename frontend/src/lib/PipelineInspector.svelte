<script lang="ts">
  import Icon from './Icon.svelte';
  import { fetchTraceSamples, recordOf, runTrace } from './api';
  import JsonPanel from './JsonPanel.svelte';
  import More from './More.svelte';
  import StoreColumns from './StoreColumns.svelte';
  import { t } from './i18n.svelte';
  import { patient } from './patient.svelte';
  import type { MappingLink, Trace, TraceSample, TraceStep } from './types';

  interface Props {
    /** Opens the mappings tab at the rule behind a correspondence. */
    onshowRule?: (link: MappingLink) => void;
    /** A stage id the tour wants shown, so its text and the JSON on screen agree. */
    wantedStage?: string | null;
  }

  const { onshowRule, wantedStage = null }: Props = $props();

  // The tour names a stage; honouring it once the trace exists keeps the two in step. It also has
  // to open the right direction, or the tour points at a stage the rail is not showing.
  $effect(() => {
    if (!wantedStage || !trace) return;
    const wanted = trace.steps.findIndex((s) => s.id === wantedStage);
    if (wanted >= 0) show(wanted);
  });

  let samples = $state<TraceSample[]>([]);
  let trace = $state<Trace | null>(null);
  let error = $state<string | null>(null);
  let running = $state(false);

  /** Which stage is on screen; the panel to its left is the stage it came from. */
  let selectedStep = $state(0);
  /**
   * Which half of the journey the rail is showing.
   *
   * <p>A POST and a GET are two operations, not one long line. Keeping `selectedStep` an index into
   * the whole run and filtering only what is drawn means everything that names a stage — the tour,
   * the landing on the composition — keeps working and just opens the right half.
   */
  let travel = $state<'in' | 'out'>('in');

  /** Selects a stage by its position in the whole run, opening the half it belongs to. */
  function show(index: number): void {
    selectedStep = index;
    const direction = trace?.steps[index]?.direction;
    if (direction) travel = direction;
    selectedLink = null;
  }
  let selectedLink = $state<string | null>(null);
  let activeSample = $state<string | null>(null);
  let fileInput = $state<HTMLInputElement | null>(null);

  $effect(() => {
    // Not before the directory has answered: a trace sent without a patient gets the backend's
    // default, and on a fresh browser that is not necessarily the patient the header then shows.
    // Reading it here also re-runs this once it arrives.
    if (!patient()) return;
    void (async () => {
      try {
        samples = await fetchTraceSamples();
        if (trace) return;
        const first = samples[0];
        if (first) await run(first);
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause);
      }
    })();
  });

  async function run(sample: TraceSample): Promise<void> {
    activeSample = sample.id;
    await traceJson(JSON.stringify(sample.json));
  }

  async function traceJson(json: string): Promise<void> {
    running = true;
    error = null;
    try {
      trace = await runTrace(json);
      show(landing(trace.steps));
    } catch (cause) {
      trace = null;
      error = cause instanceof Error ? cause.message : String(cause);
    } finally {
      running = false;
    }
  }

  async function onFileChosen(event: Event): Promise<void> {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    activeSample = null;
    await traceJson(await file.text());
    input.value = '';
  }

  /**
   * Where a fresh trace opens.
   *
   * <p>On the composition when there is one: that is the step worth looking at first. Otherwise
   * on the stage the way in stopped at — a rejected input is the whole lesson of that sample, and
   * the way back exists whether or not the way in got anywhere, so "the last step" would open the
   * GET with the patient selected and the inputs gone.
   */
  function landing(all: TraceStep[]): number {
    const composition = all.findIndex((item) => item.id === 'composition');
    if (composition >= 0) return composition;
    const stopped = all.findIndex((item) => item.direction === 'in' && item.status === 'error');
    if (stopped >= 0) return stopped;
    const lastIn = all.map((item) => item.direction).lastIndexOf('in');
    return lastIn >= 0 ? lastIn : all.length - 1;
  }

  const activeSummary = $derived(samples.find((s) => s.id === activeSample)?.summary ?? null);

  const steps = $derived<TraceStep[]>(trace?.steps ?? []);
  const step = $derived<TraceStep | null>(steps[selectedStep] ?? null);
  /**
   * The stage this one came from — shown beside it, so every screen reads "this became that".
   *
   * <p>Not simply the one before it in the list. The pipeline is mostly a chain but not entirely:
   * looking up who a reading is about reaches a different store and produces nothing the next stage
   * is built from. Those stages are branches, and they are skipped here — otherwise the Bundle
   * would be shown as having been made out of the Patient. A branch itself has no "came from" at
   * all: nothing in the chain became it, which is the whole reason it hangs off the line.
   */
  const previous = $derived.by<TraceStep | null>(() => {
    if (!step || step.branch) return null;
    for (let index = selectedStep - 1; index >= 0; index--) {
      const candidate = steps[index];
      if (candidate && candidate.direction === step.direction && !candidate.branch)
        return candidate;
    }
    return null;
  });

  interface RailColumn {
    item: TraceStep;
    index: number;
    branches: { item: TraceStep; index: number }[];
  }

  /**
   * The half on screen as the rail draws it: the line, and what hangs off each stage of it.
   *
   * <p>Every stage keeps its place in the whole run, so the tour and the landing on the composition
   * can still name one by index. A branch attaches to the stage before it — which is how the
   * backend orders them, and the only thing the rail has to know.
   */
  const columns = $derived.by<RailColumn[]>(() => {
    const built: RailColumn[] = [];
    steps.forEach((item, index) => {
      if (item.direction !== travel) return;
      const previousColumn = built[built.length - 1];
      if (item.branch && previousColumn) previousColumn.branches.push({ item, index });
      else built.push({ item, index, branches: [] });
    });
    return built;
  });

  /** True when a half has anything to show — the way back is empty until the mapping has run. */
  const hasTravel = (direction: 'in' | 'out'): boolean =>
    steps.some((item) => item.direction === direction);

  /** A step that failed has no JSON; one that was rejected at the door still has the input. */
  const hasJson = (candidate: TraceStep | null): boolean =>
    candidate !== null && candidate.json !== null && candidate.json !== undefined;

  /**
   * The correspondences, on both of the steps openFHIR produced.
   *
   * <p>One rule per correspondence, one file, and openFHIR runs it in whichever direction it is
   * asked for — so the same chips belong on the way back. Only these two steps have the FHIR and
   * the openEHR document side by side, which is what a correspondence needs to be shown at all.
   */
  const links = $derived<MappingLink[]>(
    step?.id === 'composition' || step?.id === 'roundtrip' ? (trace?.links ?? []) : [],
  );
  const link = $derived<MappingLink | null>(links.find((l) => l.id === selectedLink) ?? null);

  /**
   * Which end of a correspondence belongs in a panel — decided by what the panel holds, not by
   * which side of the screen it is on. The two swap places on the way back.
   */
  const pointerFor = (panel: TraceStep | null, chosen: MappingLink | null): string | null => {
    if (!panel || !chosen) return null;
    return (panel.standard === 'openehr' ? chosen.openehrPointer : chosen.fhirPointer) ?? null;
  };

  const kindLabel = (kind: MappingLink['kind']): string => t(`inspector.kind.${kind}`);
</script>

<section class="inspector">
  <header>
    <h2>{t('inspector.title')}</h2>
    <p class="meta">
      {t('inspector.lede')}
    </p>
  </header>

  <!-- Two operations, shown apart: a POST puts a reading in, a GET brings it back with the other
     half of the record attached. Directly under the inputs, because it is the second choice a
     reader makes: what goes in, then which half of the journey to look at. -->
  <div class="travel" role="tablist" aria-label={t('inspector.travel')}>
    {#each [['in', 'inspector.travel.in'], ['out', 'inspector.travel.out']] as const as [id, key] (id)}
      <button
        type="button"
        role="tab"
        aria-selected={travel === id}
        class:active={travel === id}
        disabled={!hasTravel(id)}
        onclick={() => {
          travel = id;
          const first = steps.findIndex((item) => item.direction === id);
          if (first >= 0) show(first);
        }}
      >
        {t(key)}
      </button>
    {/each}
  </div>

  {#if travel === 'in'}
    <div class="inputs">
      {#each samples as sample (sample.id)}
        <button
          type="button"
          class="sample"
          class:active={activeSample === sample.id}
          disabled={running}
          onclick={() => run(sample)}
        >
          {sample.label}
        </button>
      {/each}
      <button
        type="button"
        class="sample own"
        disabled={running}
        onclick={() => fileInput?.click()}
      >
        {t('inspector.ownFile')}
      </button>
      <input
        bind:this={fileInput}
        type="file"
        accept="application/json,application/fhir+json,.json"
        onchange={onFileChosen}
        hidden
      />
    </div>

    {#if activeSummary}
      <p class="summary">{activeSummary}</p>
    {/if}
  {/if}

  {#if error}
    <p class="error" role="alert">{error}</p>
  {:else if running && !trace}
    <p class="meta">{t('inspector.running')}</p>
  {:else if trace}
    {#if travel === 'in'}
      <p class="recognised">
        {t('inspector.recognised')} <strong>{trace.inputLabel}</strong>.
        <span class="dry-note">{t('inspector.dry')}</span>
      </p>
    {/if}

    <ol class="rail">
      {#each columns as column, position (column.item.id)}
        {@const next = columns[position + 1]}
        <li>
          <div class="line">
            <button
              type="button"
              class="stage"
              data-standard={column.item.standard}
              class:active={column.index === selectedStep}
              class:failed={column.item.status === 'error'}
              onclick={() => show(column.index)}
            >
              <span class="stage-index">{position + 1}</span>
              <span class="stage-title">{column.item.title}</span>
              <!-- Only where no arrow can say it: every other stage is named by the arrow that
                   leads into it, and printing it twice would make the actor look like a place. -->
              {#if position === 0}
                <span class="stage-actor">{column.item.actor}</span>
              {/if}
            </button>
            <!-- The arrow lives on the line rather than between the columns, so a branch hanging
                 below one of them never ends up with an arrow pointing at it. It is labelled with
                 who does the step: a stage is a document, and openFHIR is not somewhere the
                 reading is — it is what turns one document into the next. -->
            {#if next}
              <span class="arrow">
                <span class="who">{next.item.actor}</span>
                <span class="glyph" aria-hidden="true">→</span>
              </span>
            {/if}
          </div>
          {#each column.branches as branch (branch.item.id)}
            <div class="aside">
              <button
                type="button"
                class="stage branch"
                data-standard={branch.item.standard}
                class:active={branch.index === selectedStep}
                class:failed={branch.item.status === 'error'}
                onclick={() => show(branch.index)}
              >
                <span class="stage-title">{branch.item.title}</span>
                <span class="stage-actor">{branch.item.actor}</span>
              </button>
            </div>
          {/each}
        </li>
      {/each}
    </ol>

    {#if step}
      <article class="explanation">
        {#if step.summary}
          <p>{step.summary}</p>
          <More><p>{step.explanation}</p></More>
        {:else}
          <p>{step.explanation}</p>
        {/if}
        {#if step.call}
          <p class="call">
            <code>{step.call}</code>
            {#if step.durationMs != null}<span class="timing">{step.durationMs} ms</span>{/if}
          </p>
        {/if}
        {#if step.query}
          <pre class="query">{step.query.trim()}</pre>
        {/if}
        {#if step.note}
          <p class="note" class:bad={step.status === 'error'}>{step.note}</p>
        {/if}
      </article>

      <!-- The one stage where both stores meet: the Bundle, taken apart again by where each
           entry lives. It used to be a tab of its own; here it sits beside the JSON it explains. -->
      {#if step.id === 'assembled' && step.json}
        <StoreColumns record={recordOf(step.json)} />
      {/if}

      {#if links.length}
        <section class="links">
          <h3>{t('inspector.links.title')}</h3>
          <p class="meta">
            {t('inspector.links.lede')}
          </p>
          <div class="chips">
            {#each links as item (item.id)}
              <button
                type="button"
                class="chip"
                data-kind={item.kind}
                class:active={selectedLink === item.id}
                onclick={() => (selectedLink = selectedLink === item.id ? null : item.id)}
              >
                <span>{item.label}</span>
                <span class="chip-kind">{kindLabel(item.kind)}</span>
              </button>
            {/each}
          </div>
          {#if link}
            <p class="link-explanation">{link.explanation}</p>
            {#if onshowRule && link.mappingFile}
              <button type="button" class="rule-link" onclick={() => onshowRule(link)}>
                {t('inspector.openRule', link.mappingFile)}
                <Icon name="right" size={13} />
              </button>
            {/if}
          {/if}
        </section>
      {/if}

      <!-- A rejected input still has JSON worth seeing: the wrong code is the whole lesson. -->
      {#if hasJson(step)}
        <div class="panels" class:single={!hasJson(previous)}>
          {#if hasJson(previous) && previous}
            <JsonPanel
              label={previous.title}
              actor={previous.actor}
              standard={previous.standard}
              value={previous.json}
              highlight={pointerFor(previous, link)}
            />
            <!-- Left is always the stage this one came from, so the direction never varies: the
                 arrow says "became", which is the one thing two panels side by side do not. -->
            <span class="became" aria-hidden="true"><Icon name="right" size={18} /></span>
          {/if}
          <JsonPanel
            label={step.title}
            actor={step.actor}
            standard={step.standard}
            value={step.json}
            highlight={pointerFor(step, link)}
          />
        </div>
      {/if}

      {#if step.differences?.length}
        <section class="differences">
          <h3>{t('inspector.diff.title')}</h3>
          <p class="meta">
            {t('inspector.diff.lede')}
          </p>
          <ul>
            {#each step.differences as difference (difference.pointer)}
              <li class={difference.kind}>
                <code>{difference.pointer}</code>
                <span class="difference-kind">{difference.kind}</span>
                {#if difference.before}<span class="before">{difference.before}</span>{/if}
                {#if difference.after}<span class="after">→ {difference.after}</span>{/if}
              </li>
            {/each}
          </ul>
        </section>
      {/if}
    {/if}
  {/if}
</section>

<style>
  .inspector {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  h2 {
    font-size: 1.15rem;
  }

  h3 {
    font-size: 0.95rem;
  }

  .meta {
    margin: 6px 0 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .inputs {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 10px;
  }

  .sample {
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 5px 13px;
    font-size: 0.79rem;
    color: var(--text-secondary);
    cursor: pointer;
  }

  .sample.active {
    border-color: var(--series-resting);
    color: var(--series-resting);
  }

  .sample.own {
    border-style: dashed;
  }

  .summary {
    margin: 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .recognised {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-secondary);
  }

  .dry-note {
    color: var(--text-muted);
  }

  .travel {
    display: inline-flex;
    gap: 2px;
    padding: 2px;
    border: 1px solid var(--border);
    border-radius: 999px;
  }

  .travel button {
    background: none;
    border: none;
    border-radius: 999px;
    padding: 5px 14px;
    font: inherit;
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--text-muted);
    cursor: pointer;
  }

  .travel button.active {
    background: var(--band-fill);
    color: var(--text-primary);
  }

  .travel button:disabled {
    opacity: 0.4;
    cursor: default;
  }

  .rail {
    list-style: none;
    display: flex;
    flex-wrap: wrap;
    /* Column gap is the arrow's own margin; the row gap is for when the line wraps. */
    gap: 10px 0;
    margin: 0;
    /* Sticky because reading the JSON below and switching stage are the same activity, and the
       switcher scrolling away means scrolling back up for every step. */
    position: sticky;
    top: 0;
    z-index: 2;
    padding: 10px 0;
    background: var(--page);
  }

  /* A column: one stage of the line, and whatever hangs off it. */
  .rail li {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }

  .line {
    display: flex;
    align-items: center;
  }

  /* The arrow between stages: this is a pipeline, not a menu — and the one place the actor
     belongs, because what openFHIR and the backend do happens between two documents. */
  .arrow {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1px;
    margin: 0 9px;
    color: var(--text-muted);
  }

  .arrow .who {
    font-size: 0.68rem;
    letter-spacing: 0.02em;
    white-space: nowrap;
  }

  .arrow .glyph {
    font-size: 0.95rem;
    line-height: 0.9;
  }

  /* A branch hangs under the stage it belongs to rather than after it. The FHIR store is consulted
     beside the journey; nothing on the line is made out of what it answers, and a stage drawn in
     the line would say it was. */
  .aside {
    display: flex;
    padding-left: 18px;
  }

  .branch {
    position: relative;
    margin-top: 9px;
    border-top-style: dashed;
    border-right-style: dashed;
    border-bottom-style: dashed;
  }

  /* The corner: down out of the stage above, then across into this one. */
  .branch::before {
    content: '';
    position: absolute;
    left: -15px;
    top: -14px;
    bottom: 50%;
    /* Wide enough to meet the button's own border rather than stopping just short of it. */
    width: 15px;
    /* Solid and muted rather than dashed: the dashes are on the stage itself, and two dashed
       things next to each other read as one smudge at this size. */
    border-left: 1px solid var(--text-muted);
    border-bottom: 1px solid var(--text-muted);
    border-bottom-left-radius: 5px;
  }

  .stage {
    display: flex;
    align-items: baseline;
    gap: 7px;
    padding: 7px 12px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-left: 3px solid var(--standard-colour, var(--border));
    border-radius: 7px;
    cursor: pointer;
    font-size: 0.8rem;
  }

  .stage[data-standard='fhir'] {
    --standard-colour: var(--series-resting);
  }

  .stage[data-standard='openehr'] {
    --standard-colour: var(--series-reading);
  }

  .stage.active {
    background: var(--band-fill);
  }

  .stage.failed {
    border-color: var(--critical);
    color: var(--critical);
  }

  .stage-index {
    color: var(--text-muted);
    font-variant-numeric: tabular-nums;
  }

  .stage-title {
    font-weight: 500;
  }

  .stage-actor {
    font-size: 0.72rem;
    color: var(--text-muted);
  }

  .explanation {
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 13px 16px;
  }

  .explanation p {
    margin: 0;
    font-size: 0.84rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .call {
    margin-top: 10px !important;
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 10px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.76rem !important;
  }

  .timing {
    color: var(--text-muted);
  }

  .query {
    margin: 12px 0 0;
    padding: 11px 13px;
    overflow-x: auto;
    background: var(--page);
    border: 1px solid var(--border);
    border-radius: 7px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.74rem;
    line-height: 1.55;
    color: var(--text-primary);
  }

  .note {
    margin-top: 10px !important;
    font-size: 0.8rem !important;
    color: var(--text-muted) !important;
  }

  .note.bad {
    color: var(--critical) !important;
  }

  .panels {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    gap: 14px;
    align-items: start;
  }

  .panels.single {
    grid-template-columns: 1fr;
  }

  .became {
    /* Centred in the row, so it reads as connecting the two panels rather than labelling one. */
    align-self: center;
    color: var(--text-muted);
  }

  @media (max-width: 780px) {
    .panels {
      grid-template-columns: 1fr;
    }

    .became {
      justify-self: center;
      margin: 0;
      /* Stacked, so the same arrow has to point the way the eye now travels. */
      transform: rotate(90deg);
    }
  }

  .links,
  .differences {
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 16px 18px;
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
  }

  .chip {
    display: flex;
    flex-direction: column;
    gap: 2px;
    text-align: left;
    padding: 7px 12px;
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 7px;
    cursor: pointer;
    font-size: 0.8rem;
  }

  .chip.active {
    background: var(--band-fill);
    border-color: var(--series-resting);
  }

  .chip-kind {
    font-size: 0.7rem;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .chip[data-kind='generated'] .chip-kind {
    color: var(--series-reading);
  }

  .rule-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    margin-top: 10px;
    background: none;
    border: none;
    padding: 0;
    color: var(--series-reading);
    font-size: 0.82rem;
    cursor: pointer;
    text-align: left;
  }

  .link-explanation {
    margin: 14px 0 0;
    font-size: 0.86rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .differences ul {
    list-style: none;
    margin: 12px 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 0.78rem;
  }

  .differences li {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 8px;
  }

  .differences code {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  }

  .difference-kind {
    font-size: 0.68rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--text-muted);
  }

  .differences li.lost .difference-kind {
    color: var(--critical);
  }

  .before {
    color: var(--text-secondary);
  }

  .after {
    color: var(--text-muted);
  }

  .error {
    margin: 0;
    color: var(--critical);
    font-size: 0.86rem;
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }

  button:disabled {
    opacity: 0.6;
    cursor: default;
  }
</style>
