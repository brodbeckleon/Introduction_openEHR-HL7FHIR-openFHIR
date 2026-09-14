<script lang="ts">
  import Icon from './Icon.svelte';
  import { resetMapping, saveMapping } from './api';
  import CopyButton from './CopyButton.svelte';
  import type { MappingLink, MappingSource } from './types';
  import { t } from './i18n.svelte';

  interface Props {
    mappings: MappingSource[];
    /** The link whose rule should be shown and highlighted, if one is selected. */
    link: MappingLink | null;
    /** Called after a save or reset, so the trace can be run again against the new rules. */
    onchanged?: (mappings: MappingSource[]) => void;
  }

  const { mappings, link, onchanged }: Props = $props();

  /** Show the file the selected link lives in; otherwise the one worth reading first. */
  let chosen = $state<string | null>(null);
  const file = $derived(
    mappings.find((m) => m.file === (chosen ?? link?.mappingFile)) ?? mappings[mappings.length - 1],
  );
  const lines = $derived(file ? file.content.replace(/\n$/, '').split('\n') : []);

  /** Only highlight when the rule actually lives in the file on screen. */
  const range = $derived(
    link && file && link.mappingFile === file.file && link.fromLine && link.toLine
      ? { from: link.fromLine, to: link.toLine }
      : null,
  );

  let container = $state<HTMLDivElement | null>(null);
  $effect(() => {
    if (!range || editing || !container) return;
    container.querySelector('.line.lit')?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  });

  let editing = $state(false);
  let draft = $state('');
  let busy = $state(false);
  let outcome = $state<{ ok: boolean; message: string } | null>(null);

  function edit(): void {
    if (!file) return;
    draft = file.content;
    outcome = null;
    editing = true;
  }

  function cancel(): void {
    editing = false;
    outcome = null;
  }

  async function save(): Promise<void> {
    if (!file) return;
    busy = true;
    try {
      const result = await saveMapping(file.file, draft);
      outcome = result.applied
        ? {
            ok: true,
            message: t('mapping.savedOk'),
          }
        : {
            ok: false,
            message: t('mapping.savedBad', result.detail ?? t('mapping.noDetail')),
          };
      editing = false;
      onchanged?.(result.mappings);
    } catch (cause) {
      outcome = { ok: false, message: cause instanceof Error ? cause.message : String(cause) };
    } finally {
      busy = false;
    }
  }

  async function restore(): Promise<void> {
    if (!file) return;
    busy = true;
    try {
      const result = await resetMapping(file.file);
      outcome = { ok: true, message: t('mapping.wasReset') };
      editing = false;
      onchanged?.(result.mappings);
    } catch (cause) {
      outcome = { ok: false, message: cause instanceof Error ? cause.message : String(cause) };
    } finally {
      busy = false;
    }
  }
</script>

<section class="mapping">
  <header>
    <div>
      <h3>{t('mapping.title')}</h3>
      <p class="meta">
        {t('mapping.lede')}
      </p>
    </div>
  </header>

  <div class="files" role="tablist" aria-label="Mapping files">
    {#each mappings as mapping (mapping.file)}
      <button
        type="button"
        role="tab"
        aria-selected={file?.file === mapping.file}
        class:active={file?.file === mapping.file}
        onclick={() => {
          chosen = mapping.file;
          editing = false;
          outcome = null;
        }}
      >
        {mapping.file}{#if mapping.edited}<span class="dot" title={t('mapping.edited')}>●</span
          >{/if}
      </button>
    {/each}
  </div>

  {#if file}
    <p class="meta description">{file.title}</p>

    <div class="actions">
      {#if editing}
        <button type="button" class="primary" disabled={busy} onclick={save}>
          <Icon name="check" />
          {busy ? t('mapping.saving') : t('mapping.save')}
        </button>
        <button type="button" class="secondary" disabled={busy} onclick={cancel}>
          <Icon name="close" />
          {t('mapping.cancel')}
        </button>
      {:else}
        <button type="button" class="secondary" onclick={edit}>
          <Icon name="pencil" />
          {t('mapping.edit')}
        </button>
      {/if}
      <CopyButton text={() => (editing ? draft : (file?.content ?? ''))} />
      {#if file.edited}
        <button type="button" class="secondary" disabled={busy} onclick={restore}>
          <Icon name="reset" />
          {t('mapping.reset')}
        </button>
        <span class="edited-note">{t('mapping.edited')}</span>
      {/if}
    </div>

    {#if outcome}
      <p class="outcome" class:bad={!outcome.ok} role="status">{outcome.message}</p>
    {/if}

    {#if editing}
      <textarea
        bind:value={draft}
        spellcheck="false"
        autocomplete="off"
        aria-label="{file.file} content"></textarea>
    {:else}
      <div class="code" bind:this={container}>
        {#each lines as line, index (index)}
          <div
            class="line"
            class:lit={range !== null && index + 1 >= range.from && index + 1 <= range.to}
          >
            <span class="gutter">{index + 1}</span><span class="content">{line || ' '}</span>
          </div>
        {/each}
      </div>
    {/if}
  {/if}
</section>

<style>
  .mapping {
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
    max-width: 78ch;
  }

  .description {
    margin: 10px 0 0;
    font-style: italic;
  }

  .files {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 14px;
  }

  .files button {
    background: transparent;
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 4px 12px;
    font-size: 0.76rem;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    color: var(--text-secondary);
    cursor: pointer;
  }

  .files button.active {
    border-color: var(--series-reading);
    color: var(--series-reading);
  }

  .dot {
    margin-left: 6px;
    color: var(--series-reading);
    font-size: 0.7em;
    vertical-align: middle;
  }

  .actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 9px;
    margin-top: 12px;
  }

  .actions button {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    border-radius: 7px;
    padding: 6px 14px;
    font-size: 0.82rem;
    font-weight: 500;
    cursor: pointer;
  }

  .primary {
    background: var(--series-reading);
    color: #fff;
    border: none;
  }

  .secondary {
    background: transparent;
    color: var(--text-secondary);
    border: 1px solid var(--border);
  }

  button:disabled {
    opacity: 0.6;
    cursor: default;
  }

  .edited-note {
    font-size: 0.78rem;
    color: var(--series-reading);
  }

  .outcome {
    margin: 10px 0 0;
    font-size: 0.82rem;
    color: var(--success);
    max-width: 78ch;
  }

  .outcome.bad {
    color: var(--critical);
  }

  .code,
  textarea {
    margin-top: 12px;
    max-height: 340px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.74rem;
    line-height: 1.55;
    border: 1px solid var(--border);
    border-radius: 7px;
  }

  .code {
    overflow: auto;
    padding: 8px 0;
  }

  textarea {
    display: block;
    width: 100%;
    height: 340px;
    padding: 10px 12px;
    resize: vertical;
    background: var(--page);
    color: var(--text-primary);
    /* Tabs are not YAML indentation; spaces keep a pasted block from breaking the file. */
    tab-size: 2;
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

  button:focus-visible,
  textarea:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
  }
</style>
