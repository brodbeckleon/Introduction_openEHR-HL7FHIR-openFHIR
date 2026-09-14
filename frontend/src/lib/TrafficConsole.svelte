<script lang="ts">
  import RichText from './RichText.svelte';
  import { clearTraffic, fetchTraffic } from './api';
  import JsonPanel from './JsonPanel.svelte';
  import { locale, t } from './i18n.svelte';
  import type { TrafficEntry } from './types';

  const POLL_MS = 2000;

  let entries = $state<TrafficEntry[]>([]);
  let error = $state<string | null>(null);
  let expanded = $state<number | null>(null);
  let live = $state(true);
  let filter = $state<'all' | 'openFHIR' | 'EHRbase'>('all');

  /** Newest first for reading, but the backend hands them over oldest first. */
  let highestSeq = $state(0);

  $effect(() => {
    void poll();
    if (!live) return;
    const timer = setInterval(() => void poll(), POLL_MS);
    return () => clearInterval(timer);
  });

  async function poll(): Promise<void> {
    try {
      const fresh = await fetchTraffic(highestSeq);
      error = null;
      if (fresh.length === 0) return;
      highestSeq = Math.max(highestSeq, ...fresh.map((entry) => entry.seq));
      entries = [...fresh.reverse(), ...entries];
    } catch (cause) {
      error = cause instanceof Error ? cause.message : String(cause);
    }
  }

  async function clear(): Promise<void> {
    await clearTraffic();
    entries = [];
    expanded = null;
    // Keep the sequence: the backend counter does not restart, and asking from 0 again would
    // replay whatever arrived between the two calls.
  }

  const shown = $derived(
    filter === 'all' ? entries : entries.filter((entry) => entry.server === filter),
  );

  /** Bodies are strings on the wire; show them as JSON when they are JSON, verbatim when not. */
  function asJson(body: string | undefined): unknown {
    if (!body) return null;
    try {
      return JSON.parse(body) as unknown;
    } catch {
      return null;
    }
  }

  function time(at: string): string {
    return new Date(at).toLocaleTimeString(locale(), { hour12: false });
  }

  function statusClass(entry: TrafficEntry): string {
    if (entry.status === undefined) return 'failed';
    if (entry.status >= 400) return 'warned';
    return 'ok';
  }
</script>

<section class="console">
  <header>
    <h2>{t('traffic.title')}</h2>
    <p class="meta">
      <RichText text={t('traffic.lede')} />
    </p>
  </header>

  <div class="controls">
    <div class="filters" role="group" aria-label="Filter by server">
      {#each ['all', 'openFHIR', 'EHRbase'] as const as option (option)}
        <button
          type="button"
          class:active={filter === option}
          data-server={option}
          onclick={() => (filter = option)}
        >
          {option === 'all' ? t('traffic.both') : option}
        </button>
      {/each}
    </div>
    <label class="live">
      <input type="checkbox" bind:checked={live} />
      <span>{t('traffic.live')}</span>
    </label>
    <button type="button" class="clear" onclick={clear}>{t('traffic.clear')}</button>
  </div>

  {#if error}
    <p class="error" role="alert">{error}</p>
  {/if}

  {#if shown.length === 0}
    <p class="empty">
      {t('traffic.empty')}
    </p>
  {:else}
    <ol class="calls">
      {#each shown as entry (entry.seq)}
        <li>
          <button
            type="button"
            class="call"
            data-server={entry.server}
            aria-expanded={expanded === entry.seq}
            onclick={() => (expanded = expanded === entry.seq ? null : entry.seq)}
          >
            <span class="clock">{time(entry.at)}</span>
            <span class="server">{entry.server}</span>
            <span class="method">{entry.method}</span>
            <span class="path">
              {entry.path}{#if entry.query}<span class="query">?{entry.query}</span>{/if}
            </span>
            <span class="status {statusClass(entry)}">{entry.status ?? t('traffic.failed')}</span>
            <span class="duration">{entry.durationMs} ms</span>
          </button>

          {#if expanded === entry.seq}
            <div class="bodies">
              {#if entry.error}
                <p class="error">{entry.error}</p>
              {/if}
              <div class="panels">
                {#if asJson(entry.requestBody)}
                  <JsonPanel
                    label={t('traffic.request')}
                    actor={entry.method}
                    standard={entry.server === 'EHRbase' ? 'openehr' : 'fhir'}
                    value={asJson(entry.requestBody)}
                  />
                {:else if entry.requestBody}
                  <figure class="raw">
                    <figcaption>{t('traffic.request')}</figcaption>
                    <pre>{entry.requestBody}</pre>
                  </figure>
                {/if}

                {#if asJson(entry.responseBody)}
                  <JsonPanel
                    label={t('traffic.response')}
                    actor={String(entry.status ?? '')}
                    standard={entry.server === 'EHRbase' ? 'openehr' : 'fhir'}
                    value={asJson(entry.responseBody)}
                  />
                {:else if entry.responseBody}
                  <figure class="raw">
                    <figcaption>{t('traffic.response')}</figcaption>
                    <pre>{entry.responseBody}</pre>
                  </figure>
                {:else}
                  <p class="meta no-body">
                    {t('traffic.noBody')}
                  </p>
                {/if}
              </div>
            </div>
          {/if}
        </li>
      {/each}
    </ol>
  {/if}
</section>

<style>
  .console {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  h2 {
    font-size: 1.15rem;
  }

  .meta {
    margin: 6px 0 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .controls {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 14px;
  }

  .filters {
    display: flex;
    gap: 6px;
  }

  .filters button {
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 4px 13px;
    font-size: 0.78rem;
    color: var(--text-secondary);
    cursor: pointer;
  }

  .filters button.active {
    color: var(--text-primary);
    border-color: var(--text-muted);
  }

  .filters button[data-server='openFHIR'].active {
    border-color: var(--series-resting);
    color: var(--series-resting);
  }

  .filters button[data-server='EHRbase'].active {
    border-color: var(--series-reading);
    color: var(--series-reading);
  }

  .live {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 0.8rem;
    color: var(--text-secondary);
  }

  .clear {
    margin-left: auto;
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 7px;
    padding: 5px 13px;
    font-size: 0.78rem;
    color: var(--text-secondary);
    cursor: pointer;
  }

  .calls {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .call {
    width: 100%;
    display: grid;
    grid-template-columns: 5.5em 5.5em 3.6em 1fr auto auto;
    align-items: baseline;
    gap: 12px;
    text-align: left;
    padding: 8px 12px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    /* Same two colours as everywhere else: blue is FHIR, orange is openEHR. */
    border-left: 3px solid var(--server-colour, var(--border));
    border-radius: 7px;
    cursor: pointer;
    font-size: 0.78rem;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  }

  .call[data-server='openFHIR'] {
    --server-colour: var(--series-resting);
  }

  .call[data-server='EHRbase'] {
    --server-colour: var(--series-reading);
  }

  .clock,
  .duration {
    color: var(--text-muted);
  }

  .server {
    color: var(--text-secondary);
  }

  .method {
    font-weight: 600;
  }

  .path {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .query {
    color: var(--text-muted);
  }

  .status.ok {
    color: var(--success);
  }

  .status.warned,
  .status.failed {
    color: var(--critical);
  }

  .bodies {
    padding: 12px 0 16px;
  }

  .panels {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
    align-items: start;
  }

  @media (max-width: 780px) {
    .panels {
      grid-template-columns: 1fr;
    }

    .call {
      grid-template-columns: 1fr auto;
      row-gap: 4px;
    }
  }

  .raw {
    margin: 0;
    min-width: 0;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    overflow: hidden;
  }

  .raw figcaption {
    padding: 9px 12px;
    border-bottom: 1px solid var(--border);
    font-size: 0.8rem;
    font-weight: 600;
  }

  .raw pre {
    margin: 0;
    padding: 10px 12px;
    max-height: 420px;
    overflow: auto;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.72rem;
    line-height: 1.5;
  }

  .no-body {
    align-self: center;
  }

  .empty {
    margin: 0;
    padding: 28px 20px;
    background: var(--surface-1);
    border: 1px dashed var(--border);
    border-radius: var(--radius);
    color: var(--text-secondary);
    text-align: center;
    font-size: 0.85rem;
  }

  .error {
    margin: 0;
    color: var(--critical);
    font-size: 0.84rem;
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
