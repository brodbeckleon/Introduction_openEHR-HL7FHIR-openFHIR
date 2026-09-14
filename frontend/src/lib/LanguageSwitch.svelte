<script lang="ts">
  import { language, setLanguage, t } from './i18n.svelte';

  interface Props {
    /** Called after the language changed, so backend text can be fetched again. */
    onchange?: () => void;
  }

  const { onchange }: Props = $props();

  const OPTIONS = [
    { id: 'en', label: 'EN', title: 'English' },
    { id: 'de', label: 'DE', title: 'Deutsch' },
  ] as const;

  function choose(next: 'en' | 'de'): void {
    if (next === language()) return;
    setLanguage(next);
    // The pipeline stages and mapping descriptions come from the backend and were fetched in the
    // old language; whoever owns them has to ask again.
    onchange?.();
  }
</script>

<div class="switch" role="group" aria-label={t('lang.label')}>
  {#each OPTIONS as option (option.id)}
    <button
      type="button"
      title={option.title}
      aria-pressed={language() === option.id}
      class:active={language() === option.id}
      onclick={() => choose(option.id)}
    >
      {option.label}
    </button>
  {/each}
</div>

<style>
  .switch {
    display: inline-flex;
    gap: 2px;
    padding: 2px;
    border: 1px solid var(--border);
    border-radius: 999px;
  }

  button {
    background: none;
    border: none;
    border-radius: 999px;
    padding: 3px 11px;
    font-size: 0.74rem;
    font-weight: 600;
    letter-spacing: 0.04em;
    color: var(--text-muted);
    cursor: pointer;
  }

  button.active {
    background: var(--band-fill);
    color: var(--text-primary);
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
