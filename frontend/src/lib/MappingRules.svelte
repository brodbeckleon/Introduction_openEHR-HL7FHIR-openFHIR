<script lang="ts">
  import Icon from './Icon.svelte';
  import RichText from './RichText.svelte';
  import { t } from './i18n.svelte';
  import type { MappingRule } from './types';

  interface Props {
    rules: MappingRule[];
    /** The rule currently highlighted in the editor, by file and starting line. */
    selected: { file: string; fromLine: number } | null;
    onselect: (rule: MappingRule) => void;
  }

  const { rules, selected, onselect }: Props = $props();

  const isSelected = (rule: MappingRule): boolean =>
    selected !== null && selected.file === rule.file && selected.fromLine === rule.fromLine;

  /**
   * Nesting is meaningful — a nested rule only applies inside its parent — so it stays visible.
   *
   * The depth reported is the YAML indentation, which jumps (1, 4, 7) because a nested rule sits
   * under `followedBy: mappings:`. Ranking the distinct depths within each file turns that into
   * steps of one, so two rules that look equally indented really are siblings.
   */
  const ranks = $derived.by<Record<string, number[]>>(() => {
    const byFile: Record<string, number[]> = {};
    for (const rule of rules) {
      const depths = (byFile[rule.file] ??= []);
      if (!depths.includes(rule.depth)) depths.push(rule.depth);
    }
    Object.values(byFile).forEach((depths) => depths.sort((a, b) => a - b));
    return byFile;
  });

  const indentOf = (rule: MappingRule): number =>
    Math.max(0, ranks[rule.file]?.indexOf(rule.depth) ?? 0) * 18;
</script>

<section class="rules">
  <h3>{t('rules.title')}</h3>
  <p class="meta"><RichText text={t('rules.lede')} /></p>

  <ul>
    <!-- A manual entry can write several paths from one block, so the line alone is not unique. -->
    {#each rules as rule (rule.file + rule.fromLine + rule.fhir)}
      <!-- The indent belongs to the row, not the button: a width:100% button with a left margin
           overflows its container by exactly that margin. -->
      <li style="padding-left: {indentOf(rule)}px">
        <button
          type="button"
          class="rule"
          class:active={isSelected(rule)}
          data-kind={rule.kind}
          onclick={() => onselect(rule)}
        >
          <span class="name">{rule.name}</span>

          {#if rule.kind === 'correspondence'}
            <span class="path fhir">{rule.fhir}</span>
            <span class="arrow" aria-hidden="true"><Icon name="right" size={12} /></span>
            <span class="path openehr">{rule.openehr}</span>
          {:else}
            <span class="constant">{t('rules.sets')}</span>
            <span class="path fhir">{rule.fhir}</span>
            <span class="equals" aria-hidden="true">=</span>
            <span class="path value">{rule.value}</span>
          {/if}

          <span class="where">{rule.file}:{rule.fromLine}</span>
        </button>
      </li>
    {/each}
  </ul>
</section>

<style>
  .rules {
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 16px 18px 18px;
  }

  h3 {
    font-size: 0.95rem;
  }

  .meta {
    margin: 6px 0 0;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 80ch;
  }

  ul {
    list-style: none;
    margin: 14px 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .rule {
    width: 100%;
    box-sizing: border-box;
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: 9px;
    text-align: left;
    padding: 8px 12px;
    background: transparent;
    border: 1px solid var(--border);
    border-left: 3px solid var(--series-resting);
    border-radius: 7px;
    cursor: pointer;
    font-size: 0.76rem;
  }

  /* A constant does not connect two models, it writes into one. Orange would be a lie here. */
  .rule[data-kind='constant'] {
    border-left-color: var(--text-muted);
  }

  .rule.active {
    background: var(--band-fill);
  }

  .name {
    font-weight: 600;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    color: var(--text-primary);
  }

  .path {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    overflow-wrap: anywhere;
  }

  .path.fhir {
    color: var(--series-resting);
  }

  .path.openehr {
    color: var(--series-reading);
  }

  .path.value {
    color: var(--text-primary);
  }

  .arrow,
  .equals {
    color: var(--text-muted);
  }

  .constant {
    color: var(--text-muted);
  }

  .where {
    margin-left: auto;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.7rem;
    color: var(--text-muted);
    white-space: nowrap;
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
