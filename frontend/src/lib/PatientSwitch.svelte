<script lang="ts">
  import { patient, patients, setPatient } from './patient.svelte';
  import { t } from './i18n.svelte';

  interface Props {
    /** Called after the patient changed, so anything already fetched can be fetched again. */
    onchange?: () => void;
  }

  const { onchange }: Props = $props();

  const selected = $derived(patients().find((one) => one.id === patient()) ?? null);

  function choose(event: Event): void {
    const next = (event.currentTarget as HTMLSelectElement).value;
    if (next === patient()) return;
    setPatient(next);
    onchange?.();
  }
</script>

<!-- Nothing to switch between until the directory has loaded, or when only one patient exists. -->
{#if patients().length > 1}
  <div class="switch">
    <label for="patient-select">{t('patient.label')}</label>
    <select id="patient-select" value={patient() ?? ''} onchange={choose}>
      {#each patients() as one (one.id)}
        <option value={one.id}>{one.name}</option>
      {/each}
    </select>
    <!-- The EHR id is the whole point of the projection: it is what ties this patient to openEHR. -->
    {#if selected?.ehrId}
      <span class="ehr" title={t('patient.ehr')}>{selected.ehrId.slice(0, 8)}</span>
    {/if}
  </div>
{/if}

<style>
  .switch {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 2px 4px 2px 10px;
    border: 1px solid var(--border);
    border-radius: 999px;
  }

  label {
    font-size: 0.74rem;
    font-weight: 600;
    letter-spacing: 0.04em;
    color: var(--text-muted);
  }

  select {
    background: none;
    border: none;
    padding: 3px 4px;
    font: inherit;
    font-size: 0.78rem;
    color: var(--text-primary);
    cursor: pointer;
  }

  select:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
    border-radius: 4px;
  }

  .ehr {
    font-family: var(--font-mono, ui-monospace, monospace);
    font-size: 0.68rem;
    color: var(--text-muted);
    background: var(--band-fill);
    border-radius: 999px;
    padding: 2px 8px;
  }
</style>
