<script lang="ts">
  import RichText from './RichText.svelte';
  import { fetchTemplate } from './api';
  import TemplateTree from './TemplateTree.svelte';
  import { t } from './i18n.svelte';
  import type { TemplateView } from './types';

  let template = $state<TemplateView | null>(null);
  let error = $state<string | null>(null);
  let onlyFilled = $state(false);

  $effect(() => {
    void (async () => {
      try {
        template = await fetchTemplate();
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause);
      }
    })();
  });

  const unused = $derived(template ? template.nodes - template.filled : 0);
</script>

<section class="explorer">
  <header>
    <h2>{t('template.title')}</h2>
    <p class="meta">
      <RichText text={t('template.lede')} />
    </p>
  </header>

  {#if error}
    <p class="error" role="alert">{error}</p>
  {:else if !template}
    <p class="meta">{t('chart.loading')}</p>
  {:else}
    <div class="summary">
      <p class="figure">
        {template.filled}<span class="of">/ {template.nodes}</span>
      </p>
      <div>
        <p class="headline">{t('template.usedHeadline')}</p>
        <p class="meta">
          {#if template.hasData}
            {t('template.usedNote', unused)}
          {:else}
            {t('template.noData')}
          {/if}
        </p>
      </div>
      <label class="filter">
        <input type="checkbox" bind:checked={onlyFilled} />
        <span>{t('template.onlyFilled')}</span>
      </label>
    </div>

    <p class="legend">
      <span class="dot filled" aria-hidden="true"></span>{t('template.legend.filled')}
      <span class="dot" aria-hidden="true"></span>{t('template.legend.empty')}
      <span class="occ">1..1</span>{t('template.legend.occurrences')}
    </p>

    <div class="tree">
      <ul>
        <TemplateTree node={template.root} {onlyFilled} />
      </ul>
    </div>

    <p class="meta closing">{t('template.closing')}</p>
  {/if}
</section>

<style>
  .explorer {
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

  .summary {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 18px;
    padding: 15px 18px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-left: 3px solid var(--series-reading);
    border-radius: var(--radius);
  }

  .figure {
    margin: 0;
    font-size: 2rem;
    line-height: 1;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: var(--series-reading);
  }

  .of {
    margin-left: 5px;
    font-size: 0.95rem;
    font-weight: 400;
    color: var(--text-secondary);
  }

  .headline {
    margin: 0;
    font-size: 0.88rem;
    font-weight: 600;
  }

  .filter {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.8rem;
    color: var(--text-secondary);
  }

  .legend {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 7px;
    margin: 0;
    font-size: 0.76rem;
    color: var(--text-muted);
  }

  .legend .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    border: 1px solid var(--text-muted);
  }

  .legend .dot.filled {
    background: var(--series-reading);
    border-color: var(--series-reading);
  }

  .legend .dot:not(:first-child),
  .legend .occ {
    margin-left: 14px;
  }

  .occ {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    color: var(--series-resting);
  }

  .tree {
    padding: 12px 14px 16px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    overflow-x: auto;
  }

  .tree ul {
    margin: 0;
    padding: 0;
  }

  .closing {
    max-width: 82ch;
  }

  .error {
    margin: 0;
    color: var(--critical);
    font-size: 0.84rem;
  }
</style>
