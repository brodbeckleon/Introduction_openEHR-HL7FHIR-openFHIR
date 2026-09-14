<script lang="ts">
  import RichText from './RichText.svelte';
  import Icon from './Icon.svelte';
  import { t } from './i18n.svelte';

  interface Props {
    /** Step one is done once the record holds something. */
    hasData: boolean;
    importing: boolean;
    onLoadSample: () => Promise<void> | void;
    onShow: (tab: 'pipeline' | 'traffic') => void;
    onStartTour: () => void;
    /** Whether the tour has been through once, which changes what the button offers. */
    tourDone: boolean;
    /** Hiding is owned by the parent, which also renders the way back. */
    onHide: () => void;
  }

  const { hasData, importing, onLoadSample, onShow, onStartTour, tourDone, onHide }: Props =
    $props();
</script>

<section class="start">
  <header>
    <h2>{t('start.title')}</h2>
    <button type="button" class="dismiss" onclick={onHide} aria-label={t('start.hideLabel')}>
      {t('start.hide')}
    </button>
  </header>

  <p class="lede">
    <RichText text={t('start.lede')} />
  </p>

  <div class="tour-offer">
    <p>{t('tour.offer')}</p>
    <button type="button" class="action" onclick={onStartTour}>
      <Icon name="compass" />
      {tourDone ? t('tour.restart') : t('tour.start')}
    </button>
  </div>

  <ol class="steps">
    <li class:done={hasData}>
      <span class="marker" aria-hidden="true">{hasData ? '✓' : '1'}</span>
      <div>
        <h3>{t('start.step1.title')}</h3>
        <p>
          <RichText text={t('start.step1.body')} />
        </p>
        {#if hasData}
          <p class="done-note">{t('start.step1.done')}</p>
        {:else}
          <button type="button" class="action" disabled={importing} onclick={onLoadSample}>
            <Icon name="sparkle" />
            {importing ? t('exchange.importing') : t('exchange.loadSample')}
          </button>
        {/if}
      </div>
    </li>

    <li>
      <span class="marker" aria-hidden="true">2</span>
      <div>
        <h3>{t('start.step2.title')}</h3>
        <p>
          <RichText text={t('start.step2.body')} />
        </p>
        <button type="button" class="action secondary" onclick={() => onShow('pipeline')}>
          {t('start.step2.action')}
          <Icon name="right" size={13} />
        </button>
      </div>
    </li>

    <li>
      <span class="marker" aria-hidden="true">3</span>
      <div>
        <h3>{t('start.step3.title')}</h3>
        <p>
          {t('start.step3.body')}
        </p>
        <button type="button" class="action secondary" onclick={() => onShow('traffic')}>
          {t('start.step3.action')}
          <Icon name="right" size={13} />
        </button>
      </div>
    </li>
  </ol>

  <p class="hint">
    <RichText text={t('start.hint')} />
  </p>
</section>

<style>
  .start {
    padding: 20px 22px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-left: 3px solid var(--series-resting);
    border-radius: var(--radius);
  }

  header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
  }

  h2 {
    font-size: 1.1rem;
  }

  h3 {
    font-size: 0.92rem;
  }

  .dismiss {
    background: none;
    border: none;
    padding: 0;
    color: var(--text-muted);
    font-size: 0.8rem;
    cursor: pointer;
  }

  .lede,
  .hint {
    margin: 8px 0 0;
    font-size: 0.86rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .hint {
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid var(--gridline);
    font-size: 0.82rem;
    color: var(--text-muted);
  }

  .tour-offer {
    margin-top: 16px;
    padding: 14px 16px;
    background: var(--page);
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .tour-offer p {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-secondary);
    max-width: 74ch;
  }

  .steps {
    list-style: none;
    margin: 18px 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .steps li {
    display: flex;
    gap: 13px;
    align-items: flex-start;
  }

  .marker {
    flex: none;
    width: 25px;
    height: 25px;
    display: grid;
    place-items: center;
    border-radius: 50%;
    border: 1px solid var(--border);
    font-size: 0.78rem;
    font-variant-numeric: tabular-nums;
    color: var(--text-secondary);
  }

  .steps li.done .marker {
    border-color: var(--success);
    color: var(--success);
  }

  .steps p {
    margin: 4px 0 0;
    font-size: 0.84rem;
    color: var(--text-secondary);
    max-width: 74ch;
  }

  .done-note {
    color: var(--success) !important;
  }

  .action {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    margin-top: 9px;
    background: var(--series-resting);
    color: #fff;
    border: none;
    border-radius: 7px;
    padding: 7px 15px;
    font-size: 0.84rem;
    font-weight: 500;
    cursor: pointer;
  }

  .action.secondary {
    background: transparent;
    color: var(--series-resting);
    border: 1px solid var(--border);
  }

  .action:disabled {
    opacity: 0.6;
    cursor: default;
  }

  button:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
