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

  /** The bar itself, measured so the page can scroll what it talks about clear of it. */
  let bar = $state<HTMLElement | null>(null);

  /** Long enough for a tab that has to fetch before it can render what the step points at. */
  const WAIT_FOR_TARGET_MS = 2500;
  const RETRY_MS = 120;

  // The bar covers the bottom of the viewport, so the page gets that much padding while the
  // tour is open: otherwise the last thing on a tab could never be scrolled out from under it.
  $effect(() => {
    const height = bar?.offsetHeight ?? 0;
    // Read the step so this re-measures when the text changes length.
    void step;
    document.body.style.paddingBottom = `${height}px`;
    return () => {
      document.body.style.paddingBottom = '';
    };
  });

  /**
   * Scrolls the target into the part of the viewport the bar does not cover.
   *
   * <p>scrollIntoView centres on the whole viewport and does not know about the bar, so a tall
   * target — two JSON panels side by side — ended up with its lower half behind the card that was
   * telling the reader to look at it. Centred in what is visible when it fits; otherwise its top
   * goes just under whatever is sticky up there, which on the pipeline tab is the stage rail.
   */
  function reveal(target: Element): void {
    const covered = bar?.offsetHeight ?? 0;
    const visible = window.innerHeight - covered;
    const rect = target.getBoundingClientRect();
    const margin = 24;
    const sticky = document.querySelector('.rail');
    const room = (sticky ? sticky.getBoundingClientRect().height : 0) + margin;
    const top =
      rect.height + 2 * margin < visible
        ? rect.top + window.scrollY - (visible - rect.height) / 2
        : rect.top + window.scrollY - room;
    window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
  }

  // Putting the reader where the step is about, then ringing what it talks about. Some tabs run a
  // request before the target exists — the pipeline inspector traces a reading first — so this waits
  // for it rather than looking once and giving up.
  $effect(() => {
    const current = step;
    onshow(current.tab);
    if (current.stage) onstage?.(current.stage);
    if (!current.focus) return;

    let timer: ReturnType<typeof setTimeout>;
    let waited = 0;

    const look = () => {
      const target = document.querySelector(current.focus!);
      if (target) {
        target.classList.add('tour-focus');
        reveal(target);
        return;
      }
      waited += RETRY_MS;
      if (waited < WAIT_FOR_TARGET_MS) timer = setTimeout(look, RETRY_MS);
    };
    timer = setTimeout(look, RETRY_MS);

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

<aside class="tour" aria-label={t('tour.label')} bind:this={bar}>
  <div class="inner">
    <div class="progress" aria-hidden="true">
      {#each TOUR as step, i (step.key)}
        <span class="pip" class:done={i <= index}></span>
      {/each}
    </div>

    <div class="text">
      <p class="count" aria-live="polite">{t('tour.step', index + 1, TOUR.length)}</p>
      <h2>{t(`${step.key}.title`)}</h2>
      <p class="body">{t(`${step.key}.body`)}</p>
    </div>

    <div class="actions">
      <button type="button" class="ghost" onclick={finish}>
        <Icon name="close" size={13} />
        {last ? t('tour.close') : t('tour.stop')}
      </button>
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
  </div>
</aside>

<style>
  .tour {
    /* A bar along the bottom, not a card in a corner: several steps ask the reader to look at the
       JSON behind this, and a card in the corner covered exactly the panel it was pointing at. A
       bar takes a known strip, and the page is padded by that much, so anything can be scrolled
       clear of it. */
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 40;
    padding: 12px 20px 14px;
    background: var(--surface-1);
    border-top: 1px solid var(--border);
    box-shadow: 0 -10px 30px rgba(0, 0, 0, 0.12);
  }

  .inner {
    max-width: 900px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 6px 28px;
    align-items: end;
  }

  .text {
    min-width: 0;
  }

  .progress {
    grid-column: 1 / -1;
    display: flex;
    gap: 4px;
    margin-bottom: 6px;
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
    margin: 6px 0 0;
    font-size: 0.86rem;
    line-height: 1.5;
    color: var(--text-secondary);
    max-width: 72ch;
  }

  .actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 9px;
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

  /* Stacked on a narrow screen: the buttons go under the text rather than beside it, and
     everything is a size smaller, because the bar is taking a third of a phone's screen. */
  @media (max-width: 760px) {
    .inner {
      grid-template-columns: 1fr;
    }

    .body {
      font-size: 0.82rem;
    }

    .actions {
      justify-content: flex-start;
    }

    button {
      padding: 6px 12px;
      font-size: 0.8rem;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .tour {
      box-shadow: none;
    }
  }
</style>
