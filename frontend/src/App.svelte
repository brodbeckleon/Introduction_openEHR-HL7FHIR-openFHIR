<script lang="ts">
  import Icon from './lib/Icon.svelte';
  import AqlPlayground from './lib/AqlPlayground.svelte';
  import GettingStarted from './lib/GettingStarted.svelte';
  import HeartRateChart from './lib/HeartRateChart.svelte';
  import ManualEntry from './lib/ManualEntry.svelte';
  import MappingsTab from './lib/MappingsTab.svelte';
  import PipelineInspector from './lib/PipelineInspector.svelte';
  import RecordComposition from './lib/RecordComposition.svelte';
  import LanguageSwitch from './lib/LanguageSwitch.svelte';
  import PatientSwitch from './lib/PatientSwitch.svelte';
  import StandardsDiagram from './lib/StandardsDiagram.svelte';
  import TemplateExplorer from './lib/TemplateExplorer.svelte';
  import Tour from './lib/Tour.svelte';
  import { tourSeen } from './lib/tour';
  import TrafficConsole from './lib/TrafficConsole.svelte';
  import {
    backendIsCurrent,
    exportBundle,
    fetchPatients,
    fetchSampleBundle,
    fetchSeries,
    importFile,
  } from './lib/api';
  import { patient, rememberPatients } from './lib/patient.svelte';
  import { locale, t } from './lib/i18n.svelte';
  import type {
    DailyRestingHeartRate,
    HeartRateSeries,
    MappingLink,
    OutcomeIssue,
  } from './lib/types';

  const DAYS = 30;

  const TABS = [
    { id: 'overview', key: 'tab.overview' },
    { id: 'pipeline', key: 'tab.pipeline' },
    { id: 'traffic', key: 'tab.traffic' },
    { id: 'aql', key: 'tab.aql' },
    { id: 'mappings', key: 'tab.mappings' },
    { id: 'template', key: 'tab.template' },
    { id: 'record', key: 'tab.record' },
  ] as const;

  type Tab = (typeof TABS)[number]['id'];

  function tabFromHash(): Tab {
    const candidate = location.hash.slice(1);
    return TABS.some((entry) => entry.id === candidate && candidate !== 'overview')
      ? (candidate as Tab)
      : 'overview';
  }

  /** In the hash so a view can be linked to — "look at the inspector" is a thing people say. */
  let tab = $state<Tab>(tabFromHash());

  /** An Observation handed from the manual form to the inspector, traced instead of stored. */
  let handoff = $state<unknown | null>(null);

  function show(next: Tab): void {
    tab = next;
    history.replaceState(null, '', next === 'overview' ? location.pathname : `#${next}`);
  }

  function traceIt(observation: unknown): void {
    handoff = observation;
    show('pipeline');
  }

  /** The tour runs over the whole app, switching tabs as it goes. */
  let touring = $state(false);
  let tourStage = $state<string | null>(null);
  let tourDone = $state(tourSeen());

  // The introduction can be hidden, but never lost: hiding it leaves a link to bring it back, and
  // the tour is reachable from the header on every tab.
  const INTRO_KEY = 'hrm.getting-started.dismissed';
  let introHidden = $state(localStorage.getItem(INTRO_KEY) === 'true');

  function setIntroHidden(hidden: boolean): void {
    introHidden = hidden;
    localStorage.setItem(INTRO_KEY, String(hidden));
    // The panel lives on the overview, so bringing it back has to go there — otherwise the button
    // is pressed from another tab and nothing appears to happen.
    if (!hidden) show('overview');
  }

  function startTour(): void {
    tourStage = null;
    touring = true;
  }

  /** A correspondence the inspector wants the rule for; the mappings tab opens at it. */
  let mappingFocus = $state<MappingLink | null>(null);

  function showRule(link: MappingLink): void {
    mappingFocus = link;
    show('mappings');
  }

  let series = $state<HeartRateSeries | null>(null);
  let loadError = $state<string | null>(null);
  let showTable = $state(false);

  let importing = $state(false);
  let importIssues = $state<OutcomeIssue[] | null>(null);
  let fileInput = $state<HTMLInputElement | null>(null);

  /** Set when the backend predates this page — the cause of symptoms that look like lost data. */
  let staleBackend = $state(false);

  $effect(() => {
    // Read the patient here rather than leaving it to the fetch: that makes the dependency explicit,
    // so switching records reloads the chart instead of relying on where the read happens to land.
    patient();
    void load();
    void (async () => {
      staleBackend = !(await backendIsCurrent());
    })();
  });

  // The directory is loaded once. A backend that predates it answers 404, and the switch simply
  // stays hidden — every request then omits the parameter and gets the configured default.
  $effect(() => {
    void (async () => {
      try {
        rememberPatients(await fetchPatients());
      } catch {
        // Nothing to switch between; the default patient still works.
      }
    })();
  });

  async function load(): Promise<void> {
    try {
      series = await fetchSeries(DAYS);
      loadError = null;
    } catch (error) {
      loadError = message(error);
    }
  }

  /** Reads the chosen file and posts it to the import endpoint. */
  async function onFileChosen(event: Event): Promise<void> {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    importing = true;
    importIssues = null;
    try {
      const outcome = await importFile(await file.text());
      importIssues = outcome.issue;
      await load();
    } catch (error) {
      importIssues = [{ severity: 'error', code: 'exception', diagnostics: message(error) }];
    } finally {
      importing = false;
      input.value = ''; // so re-picking the same file fires change again
    }
  }

  /** Imports a freshly generated month of readings through the ordinary FHIR import path. */
  async function loadSample(): Promise<void> {
    importing = true;
    importIssues = null;
    try {
      importIssues = (await importFile(await fetchSampleBundle(DAYS))).issue;
      await load();
    } catch (error) {
      importIssues = [{ severity: 'error', code: 'exception', diagnostics: message(error) }];
    } finally {
      importing = false;
    }
  }

  /** Downloads everything stored as a FHIR Bundle. */
  async function download(): Promise<void> {
    const bundle = await exportBundle(DAYS);
    const url = URL.createObjectURL(new Blob([bundle], { type: 'application/fhir+json' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `heart-rate-${new Date().toISOString().slice(0, 10)}.fhir.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
  }

  const restingDays = $derived<DailyRestingHeartRate[]>(series ? series.days : []);

  const latestResting = $derived<DailyRestingHeartRate | null>(
    restingDays.length ? (restingDays[restingDays.length - 1] ?? null) : null,
  );

  const averageResting = $derived<number | null>(
    restingDays.length
      ? Math.round(restingDays.reduce((sum, day) => sum + day.resting, 0) / restingDays.length)
      : null,
  );

  /** Latest day against the average of the days before it — the number people actually look for. */
  const delta = $derived.by<number | null>(() => {
    if (!latestResting || restingDays.length < 2) return null;
    const earlier = restingDays.slice(0, -1);
    const baseline = earlier.reduce((sum, day) => sum + day.resting, 0) / earlier.length;
    return Math.round((latestResting.resting - baseline) * 10) / 10;
  });

  function shortDate(date: string): string {
    return new Date(date).toLocaleDateString(locale(), { month: 'short', day: 'numeric' });
  }

  /** Bumped on a language change; keying the tabs on it remounts them so they refetch. */
  let reloadKey = $state(0);

  // The hash is the address of a view, so it has to keep working after load: the back button, a
  // pasted link, a hand-typed #pipeline.
  $effect(() => {
    const follow = () => (tab = tabFromHash());
    window.addEventListener('hashchange', follow);
    return () => window.removeEventListener('hashchange', follow);
  });

  /**
   * Rebuilds every view that holds fetched data.
   *
   * <p>Both switches need this for the same reason: the backend text was fetched in the old
   * language, or the data belongs to the record that is no longer being shown.
   */
  function reloadAll(): void {
    void load();
    reloadKey += 1;
  }
</script>

<main>
  <header>
    <div class="title-row">
      <h1>{t('app.title')}</h1>
      <div class="header-actions">
        <!-- Only while it is hidden: when the panel is on screen it carries its own Hide button. -->
        {#if introHidden}
          <button
            type="button"
            class="header-button"
            onclick={() => setIntroHidden(false)}
            title={t('start.show')}
          >
            <Icon name="info" />
            <span>{t('start.showShort')}</span>
          </button>
        {/if}
        <button type="button" class="header-button" onclick={startTour} title={t('tour.start')}>
          <Icon name="compass" />
          <span>{tourDone ? t('tour.restart') : t('tour.start')}</span>
        </button>
        <PatientSwitch onchange={reloadAll} />
        <LanguageSwitch onchange={reloadAll} />
      </div>
    </div>
    <p>{t('app.lede')}</p>
  </header>

  <nav aria-label="Views">
    {#each TABS as entry (entry.id)}
      <button type="button" class:active={tab === entry.id} onclick={() => show(entry.id)}>
        {t(entry.key)}
      </button>
    {/each}
  </nav>

  {#if staleBackend}
    <aside class="stale" role="alert">
      <strong>{t('stale.title')}</strong>
      {t('stale.body')}
    </aside>
  {/if}

  {#if tab === 'pipeline'}
    {#key reloadKey}
      <PipelineInspector {handoff} onshowRule={showRule} wantedStage={tourStage} />
    {/key}
  {:else if tab === 'traffic'}
    {#key reloadKey}<TrafficConsole />{/key}
  {:else if tab === 'aql'}
    {#key reloadKey}<AqlPlayground />{/key}
  {:else if tab === 'mappings'}
    {#key reloadKey}<MappingsTab focus={mappingFocus} />{/key}
  {:else if tab === 'record'}
    <RecordComposition />
  {:else if tab === 'template'}
    {#key reloadKey}<TemplateExplorer />{/key}
  {:else}
    {#if !introHidden}
      <GettingStarted
        hasData={restingDays.length > 0}
        {importing}
        onLoadSample={loadSample}
        onShow={show}
        onStartTour={startTour}
        {tourDone}
        onHide={() => setIntroHidden(true)}
      />
    {/if}

    <section class="explainer" aria-label="How the three standards fit together">
      <StandardsDiagram />
    </section>

    <section class="stats" aria-label="Summary">
      <article>
        <h3>{t('stat.resting')}</h3>
        {#if latestResting}
          <p class="figure">{latestResting.resting}<span class="unit">bpm</span></p>
          <p class="meta">{shortDate(latestResting.date)}</p>
        {:else}
          <p class="figure muted">–</p>
          <p class="meta">{t('stat.noReadings')}</p>
        {/if}
      </article>

      <article>
        <h3>{t('stat.average', DAYS)}</h3>
        <p class="figure">
          {averageResting ?? '–'}{#if averageResting}<span class="unit">bpm</span>{/if}
        </p>
        {#if delta !== null}
          <p class="meta delta" class:down={delta < 0} class:up={delta > 0}>
            {delta > 0 ? '↑' : delta < 0 ? '↓' : '→'}
            {t('stat.delta', Math.abs(delta))}
          </p>
        {:else}
          <p class="meta">{t('stat.needsTwo')}</p>
        {/if}
      </article>

      <article>
        <h3>{t('stat.coverage')}</h3>
        <p class="figure">
          {restingDays.length}<span class="unit">{t('stat.days', DAYS)}</span>
        </p>
        <p class="meta">{t('stat.coverageNote')}</p>
      </article>
    </section>

    {#if loadError}
      <p class="error" role="alert">
        {t('chart.loadError', loadError)}
      </p>
    {:else if series}
      {#if restingDays.length === 0}
        <p class="empty-state">
          {t('chart.empty')}
        </p>
      {:else}
        <HeartRateChart {series} />
      {/if}

      <ManualEntry days={restingDays} onstored={load} ontrace={traceIt} />

      <section class="exchange">
        <h2>{t('exchange.title')}</h2>
        <p class="meta">
          <strong>{t('exchange.lede.1')}</strong>
          {t('exchange.lede.2')}
        </p>
        <div class="actions">
          <button type="button" onclick={loadSample} disabled={importing}>
            <Icon name="sparkle" />
            {importing ? t('exchange.importing') : t('exchange.loadSample')}
          </button>
          <button
            type="button"
            class="secondary"
            onclick={() => fileInput?.click()}
            disabled={importing}
          >
            <Icon name="upload" />
            {t('exchange.import')}
          </button>
          <button type="button" class="secondary" onclick={download}>
            <Icon name="download" />
            {t('exchange.export')}
          </button>
          <a href="/fhir/Observation?days={DAYS}" target="_blank" rel="noreferrer"
            >{t('exchange.raw')}<Icon name="external" size={13} /></a
          >
        </div>
        <input
          bind:this={fileInput}
          type="file"
          accept="application/json,application/fhir+json,.json"
          onchange={onFileChosen}
          hidden
        />

        {#if importIssues}
          <ul class="issues">
            {#each importIssues as issue, index (index)}
              <li class={issue.severity}>{issue.diagnostics ?? issue.code}</li>
            {/each}
          </ul>
        {/if}
      </section>

      <section class="table-section">
        <button class="link" onclick={() => (showTable = !showTable)} aria-expanded={showTable}>
          {showTable ? t('table.hide') : t('table.show')}
        </button>
        {#if showTable}
          <table>
            <caption>{t('table.caption', DAYS)}</caption>
            <thead>
              <tr>
                <th scope="col">{t('table.date')}</th>
                <th scope="col">{t('table.resting')}</th>
              </tr>
            </thead>
            <tbody>
              {#each restingDays as day (day.date)}
                <tr>
                  <th scope="row">{shortDate(day.date)}</th>
                  <td>{day.resting}</td>
                </tr>
              {:else}
                <tr><td colspan="2" class="muted">{t('table.empty')}</td></tr>
              {/each}
            </tbody>
          </table>
        {/if}
      </section>
    {:else}
      <p class="meta">{t('chart.loading')}</p>
    {/if}
  {/if}
</main>

{#if touring}
  <Tour
    onshow={(next) => show(next)}
    onstage={(stage) => (tourStage = stage)}
    onclose={() => {
      touring = false;
      tourStage = null;
      tourDone = true;
    }}
  />
{/if}

<style>
  main {
    max-width: 900px;
    margin: 0 auto;
    padding: 40px 20px 64px;
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  header h1 {
    font-size: 1.6rem;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .header-button {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 5px 13px;
    font-size: 0.78rem;
    color: var(--text-secondary);
    cursor: pointer;
  }

  .header-button:hover {
    color: var(--text-primary);
    border-color: var(--text-muted);
  }

  /* On a narrow header the compass alone carries it; the title attribute keeps the name. */
  @media (max-width: 620px) {
    .header-button span {
      display: none;
    }
  }

  .title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }

  header p {
    margin: 6px 0 0;
    color: var(--text-secondary);
    max-width: 60ch;
  }

  nav {
    display: flex;
    gap: 4px;
    border-bottom: 1px solid var(--border);
    margin-bottom: -10px;
  }

  nav button {
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    padding: 7px 2px;
    margin-right: 18px;
    font-size: 0.88rem;
    color: var(--text-secondary);
    cursor: pointer;
  }

  nav button.active {
    color: var(--text-primary);
    border-bottom-color: var(--series-resting);
  }

  .stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
  }

  .stale {
    padding: 13px 16px;
    border: 1px solid var(--critical);
    border-left-width: 3px;
    border-radius: var(--radius);
    background: var(--surface-1);
    color: var(--text-secondary);
    font-size: 0.84rem;
    max-width: 80ch;
  }

  .stale strong {
    color: var(--critical);
  }

  .explainer {
    padding: 18px 20px 12px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .stats article,
  .exchange,
  .table-section {
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .stats article {
    padding: 16px 18px;
  }

  .exchange,
  .table-section {
    padding: 20px;
  }

  .stats h3 {
    font-size: 0.78rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--text-muted);
    font-weight: 600;
  }

  .figure {
    margin: 8px 0 0;
    font-size: 2.4rem;
    line-height: 1;
    font-weight: 600;
  }

  .figure.muted {
    color: var(--text-muted);
  }

  .unit {
    font-size: 0.95rem;
    font-weight: 400;
    color: var(--text-secondary);
    margin-left: 6px;
  }

  .meta {
    margin: 8px 0 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 70ch;
  }

  .delta.down {
    color: var(--success);
  }

  .delta.up {
    color: var(--critical);
  }

  .exchange h2 {
    font-size: 1.05rem;
    margin-bottom: 14px;
  }

  button:focus-visible,
  a:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }

  .actions button {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    background: var(--series-resting);
    color: #fff;
    border: none;
    border-radius: 7px;
    padding: 9px 18px;
    font-weight: 500;
    cursor: pointer;
  }

  .actions button.secondary {
    background: transparent;
    color: var(--series-resting);
    border: 1px solid var(--border);
  }

  button:disabled {
    opacity: 0.6;
    cursor: default;
  }

  .actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-top: 14px;
  }

  .issues {
    list-style: none;
    margin: 14px 0 0;
    padding: 0;
    font-size: 0.85rem;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .issues li {
    padding-left: 18px;
    position: relative;
    color: var(--text-secondary);
  }

  .issues li::before {
    position: absolute;
    left: 0;
  }

  .issues li.information {
    color: var(--success);
  }

  .issues li.information::before {
    content: '✓';
  }

  .issues li.warning::before {
    content: '!';
  }

  .issues li.error,
  .issues li.fatal {
    color: var(--critical);
  }

  .issues li.error::before,
  .issues li.fatal::before {
    content: '✕';
  }

  .link {
    background: none;
    border: none;
    padding: 0;
    color: var(--series-resting);
    cursor: pointer;
    font-size: 0.88rem;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 14px;
    font-size: 0.85rem;
    font-variant-numeric: tabular-nums;
  }

  caption {
    text-align: left;
    color: var(--text-secondary);
    font-size: 0.8rem;
    padding-bottom: 8px;
  }

  th,
  td {
    text-align: right;
    padding: 6px 8px;
    border-bottom: 1px solid var(--gridline);
  }

  thead th {
    color: var(--text-muted);
    font-weight: 600;
  }

  tbody th {
    text-align: left;
    font-weight: 500;
  }

  td.muted {
    text-align: left;
    color: var(--text-muted);
  }

  a {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: var(--series-resting);
    font-size: 0.88rem;
  }

  .empty-state {
    margin: 0;
    padding: 28px 20px;
    background: var(--surface-1);
    border: 1px dashed var(--border);
    border-radius: var(--radius);
    color: var(--text-secondary);
    text-align: center;
    font-size: 0.9rem;
  }

  .error {
    margin: 12px 0 0;
    color: var(--critical);
    font-size: 0.88rem;
  }
</style>
