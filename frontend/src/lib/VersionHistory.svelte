<script lang="ts">
  import RichText from './RichText.svelte';
  import { fetchDayHistory } from './api';
  import { locale, t } from './i18n.svelte';
  import type { DayHistory } from './types';

  interface Props {
    /** ISO date. The history is fetched again whenever it changes. */
    date: string;
    /** Bumped by the parent after a write, so the new version appears without a reload. */
    revision?: number;
  }

  const { date, revision = 0 }: Props = $props();

  let history = $state<DayHistory | null>(null);
  let error = $state<string | null>(null);
  let open = $state(false);

  $effect(() => {
    // Both are read so the effect re-runs on a correction as well as on a date change.
    const wanted = date;
    void revision;
    if (!open || !/^\d{4}-\d{2}-\d{2}$/.test(wanted)) return;

    void (async () => {
      try {
        const fresh = await fetchDayHistory(wanted);
        // A slower earlier request must not overwrite a newer one.
        if (fresh.date === date) {
          history = fresh;
          error = null;
        }
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause);
      }
    })();
  });

  const versions = $derived(history?.compositions.flatMap((c) => c.versions) ?? []);
  const corrected = $derived(versions.filter((v) => v.changeType === 'modification').length);

  function time(committed: string | undefined): string {
    if (!committed) return '';
    return new Date(committed).toLocaleString(locale(), {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  }
</script>

<div class="history">
  <button type="button" class="link" aria-expanded={open} onclick={() => (open = !open)}>
    {open ? t('history.hide') : t('history.show')}
  </button>

  {#if open}
    {#if error}
      <p class="error" role="alert">{error}</p>
    {:else if history === null}
      <p class="meta">{t('chart.loading')}</p>
    {:else if versions.length === 0}
      <p class="meta">{t('history.none')}</p>
    {:else}
      <p class="meta">
        <RichText text={t('history.lede')} />
      </p>

      {#if history.compositions.length > 1}
        <p class="meta caveat">
          {t('history.several', history.compositions.length)}
        </p>
      {/if}

      {#each history.compositions as composition, index (composition.uid)}
        <section class="composition">
          {#if history.compositions.length > 1}
            <p class="uid">
              {t('history.compositionOf', index + 1, history.compositions.length)} ·
              <code>{composition.uid.slice(0, 8)}</code>
            </p>
          {/if}
          <ol class="versions">
            {#each composition.versions as version (version.versionUid)}
              <li class:current={version.current}>
                <span class="tag">v{version.version}</span>
                <span class="bpm">{version.bpm ?? '–'}<span class="unit">bpm</span></span>
                <span class="change" data-change={version.changeType}>
                  {version.changeType ?? '—'}
                </span>
                <span class="when">{time(version.committed)}</span>
                {#if version.current && composition.versions.length > 1}
                  <span class="now">{t('history.inForce')}</span>
                {/if}
              </li>
            {/each}
          </ol>
        </section>
      {/each}

      {#if corrected > 0}
        <p class="meta">
          {corrected === 1 ? t('history.correction') : t('history.corrections', corrected)}
        </p>
      {/if}
    {/if}
  {/if}
</div>

<style>
  .history {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .link {
    align-self: flex-start;
    background: none;
    border: none;
    padding: 0;
    color: var(--series-resting);
    cursor: pointer;
    font-size: 0.84rem;
  }

  .meta {
    margin: 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .caveat {
    color: var(--text-muted);
  }

  .composition {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .uid {
    margin: 0;
    font-size: 0.76rem;
    color: var(--text-muted);
  }

  .versions {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  .versions li {
    display: grid;
    grid-template-columns: 2.6em 5.2em 7.5em 1fr auto;
    align-items: baseline;
    gap: 12px;
    padding: 7px 11px;
    border: 1px solid var(--border);
    /* The stripe is openEHR orange: this is the record's own history, not FHIR's. */
    border-left: 3px solid var(--gridline);
    border-radius: 7px;
    font-size: 0.8rem;
  }

  .versions li.current {
    border-left-color: var(--series-reading);
    background: var(--surface-1);
  }

  .tag {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    color: var(--text-muted);
  }

  .bpm {
    font-variant-numeric: tabular-nums;
    font-weight: 600;
  }

  .unit {
    margin-left: 4px;
    font-size: 0.76rem;
    font-weight: 400;
    color: var(--text-muted);
  }

  .change {
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--text-muted);
  }

  .change[data-change='creation'] {
    color: var(--success);
  }

  .change[data-change='deleted'] {
    color: var(--critical);
  }

  .when {
    color: var(--text-muted);
    font-variant-numeric: tabular-nums;
  }

  .now {
    font-size: 0.72rem;
    color: var(--series-reading);
  }

  .error {
    margin: 0;
    color: var(--critical);
    font-size: 0.82rem;
  }

  code {
    font-size: 0.95em;
  }

  @media (max-width: 620px) {
    .versions li {
      grid-template-columns: 2.6em 5.2em 1fr;
      row-gap: 4px;
    }
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
