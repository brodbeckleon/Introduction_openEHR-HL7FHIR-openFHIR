<script lang="ts">
  import Icon from './Icon.svelte';
  import { t } from './i18n.svelte';
  import { markTourSeen, TOUR } from './tour';
  import type { TourStep } from './tour';

  interface Props {
    /** Switches the tab a step is told on. */
    onshow: (tab: TourStep['tab']) => void;
    /** Selects a stage of the pipeline inspector, so a step can point at the right JSON. */
    onstage?: (stage: string) => void;
    onclose: () => void;
  }

  const { onshow, onstage, onclose }: Props = $props();

  let index = $state(0);
  const step = $derived(TOUR[index]!);
  const last = $derived(index === TOUR.length - 1);

  // Putting the reader where the step is about, then ringing what it talks about. The delay lets
  // the tab render first; an element that is not there is simply not highlighted.
  $effect(() => {
    const current = step;
    onshow(current.tab);
    if (current.stage) onstage?.(current.stage);

    const timer = setTimeout(() => {
      if (!current.focus) return;
      const target = document.querySelector(current.focus);
      if (!target) return;
      target.classList.add('tour-focus');
      target.scrollIntoView({ block: 'center', behavior: 'smooth' });
    }, 260);

    return () => {
      clearTimeout(timer);
      document.querySelectorAll('.tour-focus').forEach((el) => el.classList.remove('tour-focus'));
    };
  });

  function finish(): void {
    markTourSeen();
    onclose();
  }

  function onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') finish();
    if (event.key === 'ArrowRight' && !last) index += 1;
    if (event.key === 'ArrowLeft' && index > 0) index -= 1;
  }
</script>

<svelte:window onkeydown={onKeydown} />

<aside class="tour" aria-label={t('tour.label')}>
  <div class="progress" aria-hidden="true">
    {#each TOUR as step, i (step.key)}
      <span class="pip" class:done={i <= index}></span>
    {/each}
  </div>

  <p class="count" aria-live="polite">{t('tour.step', index + 1, TOUR.length)}</p>
  <h2>{t(`${step.key}.title`)}</h2>
  <p class="body">{t(`${step.key}.body`)}</p>

  <div class="actions">
    <button type="button" class="ghost" onclick={finish}>
      <Icon name="close" size={13} />
      {last ? t('tour.close') : t('tour.stop')}
    </button>
    <span class="spacer"></span>
    {#if index > 0}
      <button type="button" class="ghost" onclick={() => (index -= 1)}>
        <Icon name="left" size={13} />
        {t('tour.back')}
      </button>
    {/if}
    {#if last}
      <button type="button" class="primary" onclick={finish}>
        <Icon name="check" size={13} />
        {t('tour.finish')}
      </button>
    {:else}
      <button type="button" class="primary" onclick={() => (index += 1)}>
        {t('tour.next')}
        <Icon name="right" size={13} />
      </button>
    {/if}
  </div>
</aside>

<style>
  .tour {
    /* In the corner, not centred: several steps ask the reader to look at the JSON behind this
       card, and a card in the middle of the screen covers exactly that. */
    position: fixed;
    right: 22px;
    bottom: 22px;
    z-index: 40;
    width: min(430px, calc(100vw - 32px));
    padding: 18px 20px 16px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-left: 3px solid var(--series-resting);
    border-radius: var(--radius);
    box-shadow: 0 12px 36px rgba(0, 0, 0, 0.22);
  }

  .progress {
    display: flex;
    gap: 4px;
    margin-bottom: 12px;
  }

  .pip {
    flex: 1;
    height: 3px;
    border-radius: 2px;
    background: var(--gridline);
  }

  .pip.done {
    background: var(--series-resting);
  }

  .count {
    margin: 0;
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--text-muted);
  }

  h2 {
    margin: 5px 0 0;
    font-size: 1.02rem;
  }

  .body {
    margin: 8px 0 0;
    font-size: 0.86rem;
    line-height: 1.55;
    color: var(--text-secondary);
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 9px;
    margin-top: 16px;
  }

  .spacer {
    flex: 1;
  }

  button {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    border-radius: 7px;
    padding: 7px 16px;
    font-size: 0.84rem;
    font-weight: 500;
    cursor: pointer;
  }

  .primary {
    background: var(--series-resting);
    color: #fff;
    border: none;
  }

  .ghost {
    background: transparent;
    color: var(--text-secondary);
    border: 1px solid var(--border);
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }

  /* On a narrow screen there is no corner to hide in; full width at the bottom is honest. */
  @media (max-width: 760px) {
    .tour {
      right: 16px;
      left: 16px;
      width: auto;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .tour {
      box-shadow: 0 0 0 1px var(--border);
    }
  }
</style>
