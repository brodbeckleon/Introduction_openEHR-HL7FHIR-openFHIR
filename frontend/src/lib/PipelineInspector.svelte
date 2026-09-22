<script lang="ts">
  import Icon from './Icon.svelte';
  import { fetchTraceSamples, runTrace } from './api';
  import JsonPanel from './JsonPanel.svelte';
  import { t } from './i18n.svelte';
  import type { MappingLink, Trace, TraceSample, TraceStep } from './types';

  interface Props {
    /** An Observation sent over from the manual form, traced instead of the default sample. */
    handoff?: unknown | null;
    /** Opens the mappings tab at the rule behind a correspondence. */
    onshowRule?: (link: MappingLink) => void;
    /** A stage id the tour wants shown, so its text and the JSON on screen agree. */
    wantedStage?: string | null;
  }

  const { handoff = null, onshowRule, wantedStage = null }: Props = $props();

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
  let store = $state(false);

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
    void (async () => {
      try {
        samples = await fetchTraceSamples();
        if (trace) return;
        if (handoff) {
          activeSample = null;
          await traceJson(JSON.stringify(handoff));
          return;
        }
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
      trace = await runTrace(json, store);
      // Land on the composition when there is one: that is the step worth looking at first.
      const composition = trace.steps.findIndex((step) => step.id === 'composition');
      show(composition >= 0 ? composition : trace.steps.length - 1);
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

  const activeSummary = $derived(samples.find((s) => s.id === activeSample)?.summary ?? null);

  const steps = $derived<TraceStep[]>(trace?.steps ?? []);
  const step = $derived<TraceStep | null>(steps[selectedStep] ?? null);
  /**
   * The stage this one came from — shown beside it, so every screen reads "this became that".
   *
   * <p>Not simply the one before it in the list. The pipeline is mostly a chain but not entirely:
   * looking up who a reading is about reaches a different store and produces nothing the next
   * stage is built from. Pairing a stage with whatever happens to precede it would make the Bundle
   * look like it was made out of the Patient.
   */
  const previous = $derived.by<TraceStep | null>(() => {
    if (!step) return null;
    for (let index = selectedStep - 1; index >= 0; index--) {
      const candidate = steps[index];
      if (candidate && candidate.direction === step.direction) return candidate;
    }
    return null;
  });

  /** The stages of the half on screen, paired with their place in the whole run. */
  const shown = $derived(
    steps.map((item, index) => ({ item, index })).filter(({ item }) => item.direction === travel),
  );

  /** True when a half has anything to show — the way back is empty until the mapping has run. */
  const hasTravel = (direction: 'in' | 'out'): boolean =>
    steps.some((item) => item.direction === direction);

  /** A step that failed has no JSON; one that was rejected at the door still has the input. */
  const hasJson = (candidate: TraceStep | null): boolean =>
    candidate !== null && candidate.json !== null && candidate.json !== undefined;

  /** The correspondences only make sense on the step where openFHIR produced the composition. */
  const links = $derived<MappingLink[]>(step?.id === 'composition' ? (trace?.links ?? []) : []);
  const link = $derived<MappingLink | null>(links.find((l) => l.id === selectedLink) ?? null);

  const kindLabel = (kind: MappingLink['kind']): string => t(`inspector.kind.${kind}`);
</script>

<section class="inspector">
  <header>
    <h2>{t('inspector.title')}</h2>
    <p class="meta">
      {t('inspector.lede')}
    </p>
  </header>

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
    <button type="button" class="sample own" disabled={running} onclick={() => fileInput?.click()}>
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

  <div class="store">
    <label>
      <input type="checkbox" bind:checked={store} disabled={running} />
      <span>{t('inspector.store.title')}</span>
    </label>
    <!-- A separate disclosure rather than one control doing two jobs: ticking the box and asking
         what it means are different intentions. -->
    <details>
      <summary>{t('inspector.store.why')}</summary>
      <p>{t('inspector.store.body')}</p>
    </details>
  </div>

  {#if error}
    <p class="error" role="alert">{error}</p>
  {:else if running && !trace}
    <p class="meta">{t('inspector.running')}</p>
  {:else if trace}
    <p class="recognised">
      {t('inspector.recognised')} <strong>{trace.inputLabel}</strong>.
      {#if trace.stored}<span class="stored-note">{t('inspector.wrote')}</span>{/if}
    </p>

    <!-- Two operations, shown apart: a POST puts a reading in, a GET brings it back with the other
         half of the record attached. -->
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

    <ol class="rail">
      {#each shown as { item, index }, position (item.id)}
        <li>
          <button
            type="button"
            class="stage"
            data-standard={item.standard}
            class:active={index === selectedStep}
            class:failed={item.status === 'error'}
            onclick={() => show(index)}
          >
            <span class="stage-index">{position + 1}</span>
            <span class="stage-title">{item.title}</span>
            <span class="stage-actor">{item.actor}</span>
          </button>
        </li>
      {/each}
    </ol>

    {#if step}
      <article class="explanation">
        <p>{step.explanation}</p>
        {#if step.call}
          <p class="call">
            <code>{step.call}</code>
            {#if step.durationMs !== undefined}<span class="timing">{step.durationMs} ms</span>{/if}
          </p>
        {/if}
        {#if step.query}
          <pre class="query">{step.query.trim()}</pre>
        {/if}
        {#if step.note}
          <p class="note" class:bad={step.status === 'error'}>{step.note}</p>
        {/if}
      </article>

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
              highlight={link?.fhirPointer ?? null}
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
            highlight={link?.openehrPointer ?? null}
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

  .store {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 14px;
    font-size: 0.82rem;
    color: var(--text-secondary);
  }

  .store label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }

  .store summary {
    color: var(--text-muted);
    cursor: pointer;
  }

  .store p {
    margin: 8px 0 0;
    max-width: 78ch;
    color: var(--text-muted);
  }

  /* The explanation is wider than the row, so it gets its own line under both. */
  .store details[open] {
    flex-basis: 100%;
  }

  .recognised {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-secondary);
  }

  .stored-note {
    color: var(--series-reading);
  }

  .map {
    padding: 14px 16px 4px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .travel {
    display: inline-flex;
    gap: 2px;
    padding: 2px;
    margin-bottom: 12px;
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
    gap: 8px;
    margin: 0;
    /* Sticky because reading the JSON below and switching stage are the same activity, and the
       switcher scrolling away means scrolling back up for every step. */
    position: sticky;
    top: 0;
    z-index: 2;
    padding: 10px 0;
    background: var(--page);
  }

  /* The arrow between stages: this is a pipeline, not a menu. */
  .rail li:not(:last-child)::after {
    content: '→';
    color: var(--text-muted);
    margin-left: 8px;
  }

  .rail li {
    display: flex;
    align-items: center;
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
