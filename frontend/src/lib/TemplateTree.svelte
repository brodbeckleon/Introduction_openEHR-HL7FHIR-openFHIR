<script lang="ts">
  import { untrack } from 'svelte';
  import Term from './Term.svelte';
  import { t } from './i18n.svelte';
  import TemplateTree from './TemplateTree.svelte';
  import type { TemplateNode } from './types';

  interface Props {
    node: TemplateNode;
    /** Hides the branches the data never reaches. */
    onlyFilled: boolean;
    depth?: number;
  }

  const { node, onlyFilled, depth = 0 }: Props = $props();

  // Deep branches start closed so the shape stays readable; the first two levels are the shape.
  // untrack because this is the starting state, not a rule — a node the reader opened stays open.
  let open = $state(untrack(() => depth < 2));

  const visible = $derived(node.children.filter((child) => !onlyFilled || child.filled));
  const hasChildren = $derived(visible.length > 0);
</script>

<li class:empty={!node.filled}>
  <div class="row" style="padding-left: {depth * 16}px">
    {#if hasChildren}
      <button
        type="button"
        class="twisty"
        aria-expanded={open}
        aria-label={open ? t('template.collapse') : t('template.expand')}
        onclick={() => (open = !open)}
      >
        {open ? '▾' : '▸'}
      </button>
    {:else}
      <span class="twisty spacer" aria-hidden="true"></span>
    {/if}

    <span class="dot" class:filled={node.filled} aria-hidden="true"></span>

    {#if node.attribute}<span class="attribute">{node.attribute}</span>{/if}
    <span class="rm"><Term term={node.rmType}>{node.rmType}</Term></span>

    {#if node.name}<span class="name">{node.name}</span>{/if}
    {#if node.nodeId}<span class="node-id"><Term term={node.nodeId}>{node.nodeId}</Term></span>{/if}
    {#if node.occurrences}
      <span class="occurrences" class:mandatory={node.mandatory}>{node.occurrences}</span>
    {/if}
  </div>

  {#if node.path}
    <div class="path" style="padding-left: {depth * 16 + 44}px">{node.path}</div>
  {/if}

  {#if hasChildren && open}
    <ul>
      {#each visible as child (child.path + child.rmType + (child.nodeId ?? ''))}
        <TemplateTree node={child} {onlyFilled} depth={depth + 1} />
      {/each}
    </ul>
  {/if}
</li>

<style>
  li {
    list-style: none;
  }

  ul {
    margin: 0;
    padding: 0;
  }

  .row {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 5px;
    font-size: 0.8rem;
  }

  .twisty {
    flex: none;
    width: 16px;
    background: none;
    border: none;
    padding: 0;
    color: var(--text-muted);
    font-size: 0.7rem;
    cursor: pointer;
  }

  .spacer {
    display: inline-block;
  }

  .dot {
    flex: none;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    border: 1px solid var(--text-muted);
    align-self: center;
  }

  /* Filled is openEHR orange: this is the record's own model, not FHIR's. */
  .dot.filled {
    background: var(--series-reading);
    border-color: var(--series-reading);
  }

  .attribute {
    color: var(--text-secondary);
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  }

  .rm {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-weight: 600;
  }

  li.empty .rm,
  li.empty .attribute {
    color: var(--text-muted);
    font-weight: 500;
  }

  .name {
    color: var(--text-secondary);
  }

  .node-id {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.72rem;
    color: var(--text-muted);
  }

  .occurrences {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.72rem;
    color: var(--text-muted);
  }

  .occurrences.mandatory {
    color: var(--series-resting);
  }

  .path {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.67rem;
    color: var(--text-muted);
    opacity: 0.75;
    overflow-x: auto;
    white-space: nowrap;
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
