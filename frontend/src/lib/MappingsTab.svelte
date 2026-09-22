<script lang="ts">
  import RichText from './RichText.svelte';
  import { fetchMappingRules, fetchMappings } from './api';
  import MappingPanel from './MappingPanel.svelte';
  import MappingRules from './MappingRules.svelte';
  import { t } from './i18n.svelte';
  import type { MappingLink, MappingRule, MappingSource } from './types';

  interface Props {
    /**
     * A correspondence the pipeline inspector sent over, so the rule behind it is opened and
     * highlighted rather than hunted for.
     */
    focus?: MappingLink | null;
  }

  const { focus = null }: Props = $props();

  let mappings = $state<MappingSource[]>([]);
  let rules = $state<MappingRule[]>([]);
  let error = $state<string | null>(null);

  /**
   * A rule chosen from the list, shaped as the link the editor already knows how to highlight.
   * Starts as whatever the pipeline inspector sent over, if anything.
   */
  let chosen = $state<MappingLink | null>(null);
  const highlighted = $derived(chosen ?? focus);
  const selected = $derived(
    highlighted?.mappingFile && highlighted.fromLine
      ? { file: highlighted.mappingFile, fromLine: highlighted.fromLine }
      : null,
  );

  function select(rule: MappingRule): void {
    chosen = {
      id: rule.file + rule.fromLine,
      label: rule.name,
      kind: rule.kind === 'constant' ? 'generated' : 'mapped',
      explanation: '',
      mappingFile: rule.file,
      fromLine: rule.fromLine,
      toLine: rule.toLine,
    };
  }

  $effect(() => {
    void (async () => {
      try {
        [mappings, rules] = await Promise.all([fetchMappings(), fetchMappingRules()]);
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause);
      }
    })();
  });
</script>

<section class="mappings">
  <header>
    <h2>{t('mappings.title')}</h2>
    <p class="meta">
      <RichText text={t('mappings.lede')} />
    </p>
  </header>

  {#if error}
    <p class="error" role="alert">{error}</p>
  {:else if mappings.length === 0}
    <p class="meta">{t('chart.loading')}</p>
  {:else}
    {#if focus && !chosen}
      <p class="focus">{t('mappings.focus', focus.label)}</p>
    {/if}

    <MappingRules {rules} {selected} onselect={select} />

    <MappingPanel
      {mappings}
      link={highlighted}
      onchanged={async (fresh) => {
        mappings = fresh;
        // The rules are read from the files, so an edit can add, move or remove one.
        rules = await fetchMappingRules();
      }}
    />
  {/if}
</section>

<style>
  .mappings {
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

  .focus {
    margin: 0;
    font-size: 0.82rem;
    color: var(--series-reading);
  }

  .error {
    margin: 0;
    color: var(--critical);
    font-size: 0.84rem;
  }
</style>
