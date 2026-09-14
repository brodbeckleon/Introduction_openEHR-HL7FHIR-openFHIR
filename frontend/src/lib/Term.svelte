<script lang="ts">
  import { lookup } from './glossary';
  import type { Snippet } from 'svelte';

  interface Props {
    /** The token to explain. Also the label, unless `children` says otherwise. */
    term: string;
    /** Set when the token appeared as an object key rather than a value. */
    asKey?: boolean;
    children?: Snippet;
  }

  const { term, asKey = false, children }: Props = $props();

  const entry = $derived(lookup(term, asKey));

  // The panel lives in the top layer, so it is not clipped by the scrolling JSON panels it
  // appears inside. Nothing else positions reliably there.
  const id = `term-${Math.random().toString(36).slice(2, 9)}`;
  let trigger = $state<HTMLButtonElement | null>(null);
  let panel = $state<HTMLDivElement | null>(null);
  let open = $state(false);

  /** Places the panel under the word, nudged back inside the viewport when it would overflow. */
  function place(): void {
    if (!trigger || !panel) return;
    const anchor = trigger.getBoundingClientRect();
    const { width, height } = panel.getBoundingClientRect();
    const margin = 8;

    const left = Math.min(Math.max(margin, anchor.left), window.innerWidth - width - margin);
    const below = anchor.bottom + 6;
    const top = below + height + margin > window.innerHeight ? anchor.top - height - 6 : below;

    panel.style.left = `${Math.round(left)}px`;
    panel.style.top = `${Math.round(Math.max(margin, top))}px`;
  }

  function show(): void {
    if (!entry || !panel) return;
    // Not guarded on `open`: re-showing has to re-place, or a panel whose anchor has moved since
    // stays where it was. The top layer does not scroll with the page.
    if (!open) {
      panel.showPopover();
      open = true;
    }
    place();
  }

  function hide(): void {
    if (!panel || !open) return;
    panel.hidePopover();
    open = false;
  }

  /**
   * A fixed-position panel in the top layer does not follow its anchor, so while it is open the
   * page scrolling — or an inner panel scrolling, hence the capture phase — has to move it.
   */
  $effect(() => {
    if (!open) return;
    const follow = () => place();
    window.addEventListener('scroll', follow, { capture: true, passive: true });
    window.addEventListener('resize', follow, { passive: true });
    return () => {
      window.removeEventListener('scroll', follow, { capture: true });
      window.removeEventListener('resize', follow);
    };
  });
</script>

<!--
  The button and its panel must touch. Whitespace between them becomes a text node, and that space
  lands between the term and whatever follows it — "openFHIR , wie sie wirklich sind". Prettier would
  reformat them apart, hence the ignore.
-->
<!-- prettier-ignore -->
{#if entry}
  <button
    bind:this={trigger}
    type="button"
    class="term"
    data-standard={entry.standard}
    aria-describedby={open ? id : undefined}
    onmouseenter={show}
    onmouseleave={hide}
    onfocus={show}
    onblur={hide}
    onclick={() => (open ? hide() : show())}
  >{#if children}{@render children()}{:else}{term}{/if}</button><div
    bind:this={panel}
    {id}
    popover="manual"
    role="tooltip"
    class="panel"
    data-standard={entry.standard}
    ontoggle={(event) => (open = (event as ToggleEvent).newState === 'open')}
  >
    <p class="title">{entry.title}</p>
    <p class="body">{entry.body}</p>
    {#if entry.note}<p class="note">{entry.note}</p>{/if}
  </div>
{:else if children}
  {@render children()}
{:else}
  {term}
{/if}

<style>
  .term {
    /* Inherits so it disappears into the JSON it annotates — only the underline says it is there. */
    display: inline;
    padding: 0;
    border: none;
    background: none;
    font: inherit;
    color: inherit;
    cursor: help;
    text-decoration: underline dotted;
    text-underline-offset: 3px;
    text-decoration-color: var(--term-colour, var(--text-muted));
  }

  .term[data-standard='fhir'] {
    --term-colour: var(--series-resting);
  }

  .term[data-standard='openehr'] {
    --term-colour: var(--series-reading);
  }

  .term:hover,
  .term:focus-visible {
    text-decoration-style: solid;
  }

  .panel {
    position: fixed;
    margin: 0;
    inset: auto;
    max-width: 42ch;
    padding: 12px 14px;
    background: var(--surface-1);
    color: var(--text-secondary);
    border: 1px solid var(--border);
    border-left: 3px solid var(--term-colour, var(--border));
    border-radius: var(--radius);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
    font-family: var(--font);
    font-size: 0.82rem;
    line-height: 1.5;
    text-align: left;
    white-space: normal;
  }

  .panel[data-standard='fhir'] {
    --term-colour: var(--series-resting);
  }

  .panel[data-standard='openehr'] {
    --term-colour: var(--series-reading);
  }

  .panel:popover-open {
    display: block;
  }

  .title {
    margin: 0 0 6px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--text-primary);
  }

  .body,
  .note {
    margin: 0;
  }

  .note {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--gridline);
    color: var(--text-muted);
  }
</style>
