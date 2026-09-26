<script lang="ts">
  import { t } from './i18n.svelte';
  import type { AssembledRecord } from './types';

  /**
   * The one Bundle {@code $everything} answers with, taken apart again by where each entry lives.
   *
   * The Bundle does not say — deliberately, a FHIR client should not have to care — so the rule is
   * applied here: a Patient can only have come from the FHIR store, an Observation only from openEHR
   * by way of openFHIR. Shown at the pipeline stage that assembles the Bundle, beside its JSON.
   */
  interface Props {
    record: AssembledRecord;
  }

  const { record }: Props = $props();

  const fromStore = $derived(record.entries.filter((e) => e.origin === 'fhir-store'));
  const fromOpenEhr = $derived(record.entries.filter((e) => e.origin === 'openehr'));
  // Enough to make the point; the full list would be rows of the same shape.
  const SHOWN = 6;
</script>

<section class="stores">
  <div class="columns">
    <div class="store">
      <h3><span class="dot admin"></span>{t('record.fhirStore')}</h3>
      <p class="what">{t('record.fhirStore.what')}</p>
      {#each fromStore as entry (entry.id)}
        <div class="row">
          <span class="type">{entry.resourceType}</span>
          <span class="summary">{entry.summary}</span>
        </div>
      {/each}
    </div>

    <div class="store">
      <h3><span class="dot clinical"></span>{t('record.openehr')}</h3>
      <p class="what">{t('record.openehr.what')}</p>
      {#each fromOpenEhr.slice(0, SHOWN) as entry (entry.id)}
        <div class="row">
          <span class="type">{entry.resourceType}</span>
          <span class="summary">{entry.summary}</span>
          {#if entry.version}
            <span class="version" title={t('record.version')}>v{entry.version}</span>
          {/if}
        </div>
      {/each}
      {#if fromOpenEhr.length > SHOWN}
        <p class="muted">{t('record.more', String(fromOpenEhr.length - SHOWN))}</p>
      {/if}
    </div>
  </div>

  <p class="note">{t('record.note')}</p>
</section>

<style>
  .stores {
    margin-top: 14px;
  }

  .what,
  .note,
  .muted {
    color: var(--text-secondary);
    font-size: 0.85rem;
    line-height: 1.5;
  }

  .columns {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 18px;
  }

  .store {
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 14px 16px;
  }

  h3 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 4px;
    font-size: 0.9rem;
  }

  .dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    flex: none;
  }

  .dot.admin {
    background: var(--series-reading);
  }

  .dot.clinical {
    background: var(--series-resting);
  }

  .what {
    margin: 0 0 12px;
    font-size: 0.78rem;
  }

  .row {
    display: flex;
    align-items: baseline;
    gap: 8px;
    padding: 5px 0;
    border-top: 1px solid var(--gridline);
    font-size: 0.82rem;
  }

  .type {
    color: var(--text-muted);
    font-size: 0.72rem;
    min-width: 8ch;
  }

  .summary {
    color: var(--text-primary);
    flex: 1;
  }

  .version {
    font-family: ui-monospace, monospace;
    font-size: 0.68rem;
    color: var(--text-muted);
    background: var(--band-fill);
    border-radius: 999px;
    padding: 1px 7px;
  }

  .note {
    margin: 12px 0 0;
    max-width: 62ch;
    font-size: 0.8rem;
  }
</style>
