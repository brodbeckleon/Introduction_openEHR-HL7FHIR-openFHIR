<script lang="ts">
  import { postObservation } from './api';
  import JsonPanel from './JsonPanel.svelte';
  import VersionHistory from './VersionHistory.svelte';
  import { t } from './i18n.svelte';
  import { patient } from './patient.svelte';
  import type { DailyRestingHeartRate } from './types';

  interface Props {
    /** What the record already holds, so a correction can be shown as one. */
    days: DailyRestingHeartRate[];
    /** Called after a reading was stored, so the chart can reload. */
    onstored: () => Promise<void> | void;
  }

  const { days, onstored }: Props = $props();

  const LOINC = 'http://loinc.org';
  const UCUM = 'http://unitsofmeasure.org';

  function today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  let date = $state(today());
  let bpm = $state(58);
  let showResource = $state(false);
  let saving = $state(false);
  let result = $state<{ kind: 'ok' | 'error'; message: string } | null>(null);

  /** Bumped after every successful write so the version history refetches. */
  let revision = $state(0);

  const valid = $derived(
    /^\d{4}-\d{2}-\d{2}$/.test(date) && Number.isFinite(bpm) && bpm >= 20 && bpm <= 250,
  );

  /** What is on file for the chosen day, if anything — storing again corrects it. */
  const existing = $derived<DailyRestingHeartRate | null>(
    days.find((day) => day.date === date) ?? null,
  );

  /**
   * The Observation is built here rather than on the server, because building it is the lesson: a
   * number and a date are not a clinical fact until they carry a code, a unit and a subject.
   */
  const observation = $derived({
    resourceType: 'Observation',
    status: 'final',
    category: [
      {
        coding: [
          {
            system: 'http://terminology.hl7.org/CodeSystem/observation-category',
            code: 'vital-signs',
            display: 'Vital Signs',
          },
        ],
      },
    ],
    code: {
      coding: [{ system: LOINC, code: '40443-4', display: 'Heart rate --resting' }],
    },
    // Whoever the page is showing. The backend scopes the write by the ?patient= parameter, so a
    // fixed id here would not send the reading to the wrong record — it would do something worse
    // and label it with the wrong subject.
    subject: { reference: `Patient/${patient() ?? ''}` },
    // Midnight UTC: a resting heart rate is a whole-day value, not a moment.
    effectiveDateTime: `${date}T00:00:00Z`,
    valueQuantity: { value: bpm, unit: '/min', system: UCUM, code: '/min' },
  });

  async function save(): Promise<void> {
    saving = true;
    result = null;
    try {
      const corrected = existing;
      await postObservation(observation);
      result = {
        kind: 'ok',
        message: corrected
          ? t('manual.corrected', date, corrected.resting, bpm)
          : t('manual.stored', bpm, date),
      };
      revision += 1;
      await onstored();
    } catch (cause) {
      result = { kind: 'error', message: cause instanceof Error ? cause.message : String(cause) };
    } finally {
      saving = false;
    }
  }
</script>

<section class="manual">
  <h2>{t('manual.title')}</h2>
  <p class="meta">
    {t('manual.lede')}
  </p>

  <div class="form">
    <label>
      <span>{t('manual.date')}</span>
      <input type="date" bind:value={date} max={today()} />
    </label>
    <label>
      <span>{t('manual.rate')}</span>
      <span class="with-unit">
        <input type="number" bind:value={bpm} min="20" max="250" step="1" />
        <span class="unit">bpm</span>
      </span>
    </label>
    <button type="button" class="primary" disabled={!valid || saving} onclick={save}>
      {saving ? t('manual.storing') : existing ? t('manual.correct') : t('manual.store')}
    </button>
  </div>

  {#if !valid}
    <p class="hint">{t('manual.invalid')}</p>
  {:else if existing}
    <p class="hint">
      {t('manual.existing', existing.resting)}
    </p>
  {:else}
    <p class="hint">{t('manual.nothingYet')}</p>
  {/if}

  {#if result}
    <p class="result {result.kind}">{result.message}</p>
  {/if}

  {#if valid && existing}
    <VersionHistory {date} {revision} />
  {/if}

  <button
    type="button"
    class="link"
    aria-expanded={showResource}
    onclick={() => (showResource = !showResource)}
  >
    {showResource ? t('manual.hideResource') : t('manual.showResource')}
  </button>

  {#if showResource}
    <JsonPanel
      label="Observation"
      actor={t('manual.builtInBrowser')}
      standard="fhir"
      value={observation}
    />
    <p class="meta">
      {t('manual.resourceNote')}
    </p>
  {/if}
</section>

<style>
  .manual {
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  h2 {
    font-size: 1.05rem;
  }

  .meta {
    margin: 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .form {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: 12px;
  }

  label {
    display: flex;
    flex-direction: column;
    gap: 5px;
    font-size: 0.78rem;
    color: var(--text-secondary);
  }

  input {
    background: var(--page);
    border: 1px solid var(--border);
    border-radius: 7px;
    padding: 8px 10px;
    font-size: 0.9rem;
    font-variant-numeric: tabular-nums;
  }

  input[type='number'] {
    width: 6.5em;
  }

  .with-unit {
    display: flex;
    align-items: baseline;
    gap: 7px;
  }

  .unit {
    font-size: 0.8rem;
    color: var(--text-muted);
  }

  .form button {
    border-radius: 7px;
    padding: 9px 18px;
    font-weight: 500;
    cursor: pointer;
  }

  .primary {
    background: var(--series-resting);
    color: #fff;
    border: none;
  }

  button:disabled {
    opacity: 0.6;
    cursor: default;
  }

  .hint,
  .result {
    margin: 0;
    font-size: 0.82rem;
  }

  .hint {
    color: var(--text-muted);
  }

  .result.ok {
    color: var(--success);
  }

  .result.error {
    color: var(--critical);
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

  button:focus-visible,
  input:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
