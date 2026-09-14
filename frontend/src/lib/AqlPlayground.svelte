<script lang="ts">
  import RichText from './RichText.svelte';
  import Icon from './Icon.svelte';
  import { fetchAqlExamples, runAql } from './api';
  import { t } from './i18n.svelte';
  import type { AqlExample, AqlResult } from './types';

  let examples = $state<AqlExample[]>([]);
  let chosen = $state<string | null>(null);
  let query = $state('');
  let result = $state<AqlResult | null>(null);
  let running = $state(false);
  let error = $state<string | null>(null);

  $effect(() => {
    void (async () => {
      try {
        examples = await fetchAqlExamples();
        const first = examples[0];
        if (first && !query) pick(first);
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause);
      }
    })();
  });

  function pick(example: AqlExample): void {
    chosen = example.id;
    query = example.query;
    result = null;
  }

  const teaches = $derived(examples.find((e) => e.id === chosen)?.teaches ?? null);

  async function run(): Promise<void> {
    running = true;
    error = null;
    try {
      result = await runAql(query);
    } catch (cause) {
      error = cause instanceof Error ? cause.message : String(cause);
    } finally {
      running = false;
    }
  }

  /** Ctrl/Cmd+Enter runs it, because that is what every query console does. */
  function onKeydown(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
      event.preventDefault();
      void run();
    }
  }

  /** A cell is JSON; scalars are shown bare, structures as compact JSON. */
  function cell(value: unknown): string {
    if (value === null || value === undefined) return '—';
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  }

  const isNull = (value: unknown): boolean => value === null || value === undefined;
</script>

<section class="playground">
  <header>
    <h2>{t('aql.title')}</h2>
    <p class="meta">
      <RichText text={t('aql.lede')} />
    </p>
  </header>

  <div class="examples">
    {#each examples as example (example.id)}
      <button
        type="button"
        class="example"
        class:active={chosen === example.id}
        onclick={() => pick(example)}
      >
        {example.label}
      </button>
    {/each}
  </div>

  {#if teaches}
    <p class="teaches">{teaches}</p>
  {/if}

  <textarea
    bind:value={query}
    onkeydown={onKeydown}
    spellcheck="false"
    autocomplete="off"
    aria-label={t('aql.queryLabel')}></textarea>

  <div class="actions">
    <button type="button" class="primary" disabled={running} onclick={run}>
      <Icon name="play" size={13} />
      {running ? t('aql.running') : t('aql.run')}
    </button>
    <span class="hint">{t('aql.shortcut')}</span>
    {#if result && !result.error}
      <span class="timing">
        {result.returned === 1 ? t('aql.row') : t('aql.rows', result.returned)} · {result.durationMs}
        ms
      </span>
    {/if}
  </div>

  {#if error}
    <p class="error" role="alert">{error}</p>
  {:else if result?.error}
    <p class="error" role="alert">{result.error}</p>
  {:else if result}
    {#if result.rows.length === 0}
      <p class="meta">{t('aql.noRows')}</p>
    {:else}
      <!-- The column metadata is the AQL-specific part: each name has a path behind it. -->
      <div class="results">
        <table>
          <thead>
            <tr>
              {#each result.columns as column, index (index)}
                <th scope="col">
                  <span class="name">{column.name}</span>
                  {#if column.path}<span class="path">{column.path}</span>{/if}
                </th>
              {/each}
            </tr>
          </thead>
          <tbody>
            {#each result.rows as row, rowIndex (rowIndex)}
              <tr>
                {#each row as value, cellIndex (cellIndex)}
                  <td class:null={isNull(value)}>{cell(value)}</td>
                {/each}
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
      {#if result.truncated}
        <p class="meta">{t('aql.truncated', result.rows.length, result.returned)}</p>
      {/if}
      {#if result.rows.some((row) => row.every(isNull))}
        <p class="meta null-note">{t('aql.nullNote')}</p>
      {/if}
    {/if}
  {/if}
</section>

<style>
  .playground {
    display: flex;
    flex-direction: column;
    gap: 14px;
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

  .examples {
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
  }

  .example {
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 5px 13px;
    font-size: 0.79rem;
    color: var(--text-secondary);
    cursor: pointer;
  }

  .example.active {
    /* openEHR orange: AQL is its query language, and has no FHIR counterpart. */
    border-color: var(--series-reading);
    color: var(--series-reading);
  }

  .teaches {
    margin: 0;
    padding: 11px 14px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-left: 3px solid var(--series-reading);
    border-radius: var(--radius);
    font-size: 0.84rem;
    color: var(--text-secondary);
    max-width: 82ch;
  }

  textarea {
    display: block;
    width: 100%;
    height: 200px;
    padding: 12px 14px;
    resize: vertical;
    background: var(--surface-1);
    color: var(--text-primary);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.78rem;
    line-height: 1.6;
    tab-size: 2;
  }

  .actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
  }

  .primary {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: var(--series-reading);
    color: #fff;
    border: none;
    border-radius: 7px;
    padding: 8px 18px;
    font-size: 0.86rem;
    font-weight: 500;
    cursor: pointer;
  }

  .primary:disabled {
    opacity: 0.6;
    cursor: default;
  }

  .hint,
  .timing {
    font-size: 0.78rem;
    color: var(--text-muted);
  }

  .timing {
    margin-left: auto;
    font-variant-numeric: tabular-nums;
  }

  .results {
    overflow-x: auto;
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.78rem;
    font-variant-numeric: tabular-nums;
  }

  th {
    text-align: left;
    vertical-align: bottom;
    padding: 9px 12px;
    border-bottom: 1px solid var(--border);
    background: var(--surface-1);
    white-space: nowrap;
  }

  .name {
    display: block;
    font-weight: 600;
    color: var(--text-primary);
  }

  .path {
    display: block;
    margin-top: 3px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.68rem;
    font-weight: 400;
    color: var(--text-muted);
  }

  td {
    padding: 7px 12px;
    border-bottom: 1px solid var(--gridline);
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    max-width: 46ch;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  td.null {
    color: var(--text-muted);
  }

  .null-note {
    color: var(--series-reading);
  }

  .error {
    margin: 0;
    padding: 11px 14px;
    background: var(--surface-1);
    border: 1px solid var(--critical);
    border-left-width: 3px;
    border-radius: var(--radius);
    color: var(--critical);
    font-size: 0.83rem;
    max-width: 82ch;
  }

  button:focus-visible,
  textarea:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
