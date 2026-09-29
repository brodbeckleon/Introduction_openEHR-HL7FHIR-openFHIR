<script lang="ts">
  import type { Snippet } from 'svelte';
  import { t } from './i18n.svelte';

  /**
   * The rest of an explanation, one click away.
   *
   * Every text on screen gets a sentence or two; what is worth knowing but not worth reading first
   * lives in here. A native details element, so it opens with the keyboard and is announced as the
   * disclosure it is without any script.
   */
  interface Props {
    children: Snippet;
  }

  const { children }: Props = $props();
</script>

<details class="more">
  <summary>{t('more')}</summary>
  <div class="body">{@render children()}</div>
</details>

<style>
  .more {
    margin-top: 6px;
    font-size: 0.82rem;
    color: var(--text-secondary);
  }

  summary {
    display: inline-block;
    cursor: pointer;
    color: var(--series-resting);
    font-size: 0.8rem;
    list-style: none;
  }

  summary::-webkit-details-marker {
    display: none;
  }

  summary::after {
    content: ' ▸';
  }

  .more[open] summary::after {
    content: ' ▾';
  }

  summary:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
    border-radius: 4px;
  }

  .body {
    margin-top: 6px;
    line-height: 1.55;
    max-width: 74ch;
  }

  .body :global(p) {
    margin: 0 0 6px;
  }
</style>
