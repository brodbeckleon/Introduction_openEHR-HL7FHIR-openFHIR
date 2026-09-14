<script lang="ts">
  import Icon from './Icon.svelte';
  import { t } from './i18n.svelte';

  interface Props {
    /**
     * Produces the text to copy, called on click rather than passed as a value: the documents here
     * are large, and building the string for every render to serve the rare click is waste.
     */
    text: () => string;
    /** Extra classes from the caller, so the button can sit in different chrome. */
    variant?: string;
    /** Shows the word beside the icon; icon-only where space is tight. */
    labelled?: boolean;
  }

  const { text, variant = '', labelled = false }: Props = $props();

  let copied = $state(false);
  let failed = $state(false);
  let timer: ReturnType<typeof setTimeout> | undefined;

  const label = $derived(
    copied ? t('panel.copied') : failed ? t('panel.copyFailed') : t('panel.copy'),
  );

  async function copy(): Promise<void> {
    clearTimeout(timer);
    failed = false;
    try {
      await navigator.clipboard.writeText(text());
      copied = true;
      timer = setTimeout(() => (copied = false), 1600);
    } catch {
      // Permission refused, or a context without the clipboard API at all — over plain http to
      // anything but localhost the browser does not offer one. Saying so beats a silent no-op.
      copied = false;
      failed = true;
      timer = setTimeout(() => (failed = false), 2600);
    }
  }

  $effect(() => () => clearTimeout(timer));
</script>

<button
  type="button"
  class="copy {variant}"
  class:done={copied}
  class:failed
  class:labelled
  title={label}
  aria-label={labelled ? undefined : label}
  onclick={copy}
>
  <Icon name={copied ? 'check' : failed ? 'close' : 'copy'} size={13} />
  {#if labelled}<span>{label}</span>{/if}
  <!-- The state change is announced without the button itself being a live region, which would
       also announce the label on every re-render. -->
  <span class="sr-only" aria-live="polite">{copied || failed ? label : ''}</span>
</button>

<style>
  .copy {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: none;
    border: none;
    padding: 2px;
    font-size: 0.71rem;
    color: var(--text-muted);
    cursor: pointer;
    white-space: nowrap;
  }

  .copy:hover {
    color: var(--text-primary);
  }

  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }

  .copy.done {
    color: var(--success);
  }

  .copy.failed {
    color: var(--critical);
  }

  .copy:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
