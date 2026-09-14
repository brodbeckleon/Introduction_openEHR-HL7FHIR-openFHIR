<script lang="ts">
  import { covers, jsonLines } from './jsonLines';
  import CopyButton from './CopyButton.svelte';
  import { lookup } from './glossary';
  import Term from './Term.svelte';
  import { t } from './i18n.svelte';
  import type { Standard } from './types';

  interface Props {
    label: string;
    /** Where this representation came from — openFHIR, EHRbase, the backend. */
    actor?: string;
    standard: Standard;
    value: unknown;
    /** Pointer to highlight; every line at or below it lights up. */
    highlight?: string | null;
    /** Shown instead of the JSON when there is nothing to show. */
    placeholder?: string;
  }

  const { label, actor, standard, value, highlight = null, placeholder }: Props = $props();

  const lines = $derived(value === undefined || value === null ? [] : jsonLines(value));

  /** Strips the quotes JSON puts around a key or a string value, so the glossary can match it. */
  const token = (text: string | null): string => (text ?? '').replace(/^"|"$/g, '');

  const explains = (text: string | null, asKey: boolean): boolean =>
    lookup(token(text), asKey) !== null;

  /** Scrolls the first highlighted line into view when the selection changes. */
  let container = $state<HTMLDivElement | null>(null);
  $effect(() => {
    if (!highlight || !container) return;
    const target = container.querySelector('.line.lit');
    target?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  });
</script>

<figure class="panel" data-standard={standard}>
  <figcaption>
    <span class="label">{label}</span>
    {#if actor}<span class="actor">{actor}</span>{/if}
    {#if lines.length > 0}
      <!-- Pretty-printed, because that is what the panel shows and what a validator wants. -->
      <CopyButton text={() => JSON.stringify(value, null, 2)} />
    {/if}
  </figcaption>

  {#if lines.length === 0}
    <p class="placeholder">{placeholder ?? t('panel.nothing')}</p>
  {:else}
    <div class="code" bind:this={container}>
      {#each lines as line, index (index)}
        <div class="line" class:lit={covers(highlight, line)}>
          <span class="gutter">{index + 1}</span>
          <!-- Keys and string values are looked up in the glossary; a hit becomes hoverable in place. -->
          <span class="content" style="padding-left: {line.indent * 1.1}rem">
            {#if line.key}<span class="key"
                >{#if explains(line.key, true)}<Term term={token(line.key)} asKey>{line.key}</Term
                  >{:else}{line.key}{/if}</span
              ><span class="punct">: </span>{/if}<span
              class="value"
              class:bracket={line.kind !== 'scalar'}
              >{#if line.kind === 'scalar' && explains(line.text, false)}<Term
                  term={token(line.text)}>{line.text}</Term
                >{:else}{line.text}{/if}</span
            >{#if line.comma}<span class="punct">,</span>{/if}
          </span>
        </div>
      {/each}
    </div>
  {/if}
</figure>

<style>
  .panel {
    margin: 0;
    display: flex;
    flex-direction: column;
    min-width: 0;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    overflow: hidden;
  }

  figcaption {
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding: 9px 12px;
    border-bottom: 1px solid var(--border);
    /* The stripe says which model you are looking at, at a glance. */
    border-left: 3px solid var(--standard-colour, var(--border));
  }

  /* FHIR and openEHR get one colour each and keep it everywhere in the inspector. */
  .panel[data-standard='fhir'] {
    --standard-colour: var(--series-resting);
  }

  .panel[data-standard='openehr'] {
    --standard-colour: var(--series-reading);
  }

  .label {
    font-size: 0.8rem;
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  .actor {
    /* Pushed right so the copy button sits at the end without needing a spacer element. */
    margin-left: auto;
    font-size: 0.72rem;
    color: var(--text-muted);
    white-space: nowrap;
  }

  .code {
    overflow: auto;
    /* Tall enough that the composition is readable without a second scrollbar fighting the page. */
    max-height: min(62vh, 620px);
    padding: 8px 0;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.74rem;
    line-height: 1.55;
  }

  .line {
    display: flex;
    white-space: pre;
  }

  .line.lit {
    background: var(--band-fill);
  }

  .gutter {
    flex: none;
    width: 2.6em;
    padding-right: 10px;
    text-align: right;
    color: var(--text-muted);
    opacity: 0.55;
    user-select: none;
  }

  .content {
    padding-right: 12px;
  }

  .key {
    color: var(--text-secondary);
  }

  .punct {
    color: var(--text-muted);
  }

  .value {
    color: var(--text-primary);
  }

  .value.bracket {
    color: var(--text-muted);
  }

  .placeholder {
    margin: 0;
    padding: 20px 14px;
    color: var(--text-muted);
    font-size: 0.82rem;
  }
</style>
