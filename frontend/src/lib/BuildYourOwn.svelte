<script lang="ts">
  import CopyButton from './CopyButton.svelte';
  import Icon from './Icon.svelte';
  import RichText from './RichText.svelte';
  import { t } from './i18n.svelte';
  import { STACK } from './stack';

  /**
   * The recipe for building the same structure around a measurement of your own.
   *
   * Every other tab explains how this application works; none says what to change, in what order,
   * to make it store body weight instead. That is the question someone who wants to build on it
   * actually has, and the answer is mostly already on screen elsewhere: each step here points at
   * the tab where the heart rate's version of it can be looked at. Installing is not repeated —
   * whoever sees this tab has it running, and the README says it once.
   */

  type Example = 'pipeline' | 'traffic' | 'aql' | 'mappings' | 'template';

  interface Props {
    onShow: (tab: Example) => void;
  }

  const { onShow }: Props = $props();

  const REPOSITORY = 'https://github.com/brodbeckleon/heartrate-monitor';

  interface Step {
    /** Message key prefix: `.title` and `.body`, and `.action` when there is a tab to show. */
    key: string;
    /** What this step changes, as paths from the repository root. */
    files?: string[];
    commands?: string[];
    show?: Example;
    link?: { href: string; label: string };
  }

  const JAVA = 'backend/src/main/java/com/example/heartrate';

  const STEPS: Step[] = [
    {
      key: 'archetype',
      show: 'template',
      link: { href: 'https://ckm.openehr.org/ckm/', label: 'Clinical Knowledge Manager' },
    },
    {
      key: 'template',
      files: [
        'openfhir-bootstrap/heartrate_monitor.opt',
        'backend/src/main/resources/application.yml',
      ],
      show: 'template',
      link: { href: 'https://tools.openehr.org/designer/', label: 'Archetype Designer' },
    },
    {
      key: 'fhir',
      show: 'pipeline',
      link: {
        href: 'https://hl7.org/fhir/R4/observation-vitalsigns.html',
        label: 'FHIR R4 Vital Signs',
      },
    },
    {
      key: 'mappings',
      files: [
        'openfhir-bootstrap/heartrate.context.yaml',
        'openfhir-bootstrap/heartrate-encounter.model.yaml',
        'openfhir-bootstrap/pulse.model.yaml',
      ],
      show: 'mappings',
      link: {
        href: 'https://open-fhir.com/documentation/latest/index.html',
        label: 'openFHIR · FHIR Connect',
      },
    },
    {
      key: 'backend',
      files: [
        `${JAVA}/fhir/PulseObservations.java`,
        `${JAVA}/fhir/HeartRateExtractor.java`,
        `${JAVA}/fhir/SampleReadings.java`,
        'backend/src/main/resources/trace-samples.json',
      ],
    },
    {
      key: 'load',
      // The last one is the heart rate's own round trip, and works as it stands: the shape to copy
      // with a Bundle and a template id of your own.
      commands: [
        'docker compose up -d --build backend',
        'curl -s http://localhost:18082/fc/model',
        "curl -s 'http://localhost:18080/fhir/Bundle/$sample?days=1' > bundle.json",
        "curl -X POST 'http://localhost:18082/openfhir/toopenehr?templateId=heartrate_monitor.v1' \\\n  -H 'Content-Type: application/fhir+json' --data-binary @bundle.json",
      ],
      show: 'traffic',
    },
    {
      key: 'query',
      files: [
        `${JAVA}/service/HeartRateService.java`,
        'backend/src/main/resources/aql-examples.json',
      ],
      show: 'aql',
    },
  ];

  /** In the order someone rebuilding this meets them. */
  const PITFALLS = [
    'template',
    'stale',
    'empty',
    'interval',
    'nulls',
    'dates',
    'versions',
    'port',
    'linux',
  ];
</script>

<section class="build">
  <h2>{t('build.title')}</h2>
  <p class="lede"><RichText text={t('build.lede')} /></p>

  <h3>{t('build.pieces.title')}</h3>
  <p class="intro">{t('build.pieces.lede')}</p>

  <ul class="pieces">
    {#each STACK as piece (piece.id)}
      <li class:structure={piece.structure}>
        <div class="piece-head">
          <h4>{t(`build.piece.${piece.id}.name`)}</h4>
          <span class="kind">
            {piece.structure ? t('build.structure') : t('build.application')}
          </span>
          <span class="port">:{piece.port}</span>
        </div>
        <!-- Joined as strings: whitespace between elements in a block is trimmed by Svelte and
             reshuffled by Prettier, and the separators were losing their spaces to both. -->
        <p class="parts"><code>{piece.parts.join(' · ')}</code></p>
        <p class="role"><RichText text={t(`build.piece.${piece.id}.role`)} /></p>
        <p class="meta">
          {t('build.pinnedIn')}
          <code>{piece.pinnedIn.join(', ')}</code>
          <span class="sep">·</span>
          <a href={piece.docs} target="_blank" rel="noreferrer"
            >{t('build.docs')}<Icon name="external" size={12} /></a
          >
        </p>
      </li>
    {/each}
  </ul>

  <h3>{t('build.steps.title')}</h3>
  <p class="intro"><RichText text={t('build.steps.lede')} /></p>

  <ol class="steps">
    {#each STEPS as step, index (step.key)}
      <li>
        <span class="marker" aria-hidden="true">{index + 1}</span>
        <div class="step">
          <h4>{t(`build.step.${step.key}.title`)}</h4>
          <p><RichText text={t(`build.step.${step.key}.body`)} /></p>

          {#if step.files}
            <ul class="files" aria-label={t('build.files')}>
              {#each step.files as file (file)}
                <li><code>{file}</code></li>
              {/each}
            </ul>
          {/if}

          {#each step.commands ?? [] as command (command)}
            <div class="command">
              <pre>{command}</pre>
              <CopyButton text={() => command} />
            </div>
          {/each}

          {#if step.show || step.link}
            <div class="actions">
              {#if step.show}
                {@const target = step.show}
                <button type="button" class="action" onclick={() => onShow(target)}>
                  {t(`build.step.${step.key}.action`)}
                  <Icon name="right" size={13} />
                </button>
              {/if}
              {#if step.link}
                <a href={step.link.href} target="_blank" rel="noreferrer"
                  >{step.link.label}<Icon name="external" size={12} /></a
                >
              {/if}
            </div>
          {/if}
        </div>
      </li>
    {/each}
  </ol>

  <h3>{t('build.pitfalls.title')}</h3>
  <p class="intro">{t('build.pitfalls.lede')}</p>

  <!-- The symptom first and the cause one click later: a moment to guess, which is what makes
       the cause stick. Native details, so it works with the keyboard and without script. -->
  <ul class="pitfalls">
    {#each PITFALLS as key (key)}
      <li>
        <details>
          <summary>{t(`build.pitfall.${key}.symptom`)}</summary>
          <p><RichText text={t(`build.pitfall.${key}.cause`)} /></p>
        </details>
      </li>
    {/each}
  </ul>

  <p class="footer">
    {t('build.footer')}
    <a href="{REPOSITORY}#running-it" target="_blank" rel="noreferrer"
      >{t('build.footer.readme')}<Icon name="external" size={12} /></a
    >
  </p>
</section>

<style>
  .build {
    padding: 4px 0 8px;
  }

  h2 {
    margin: 0 0 6px;
    font-size: 1.05rem;
  }

  h3 {
    margin: 30px 0 4px;
    font-size: 0.95rem;
  }

  h4 {
    margin: 0;
    font-size: 0.9rem;
  }

  p {
    margin: 0;
  }

  .lede,
  .intro {
    color: var(--text-secondary);
    font-size: 0.85rem;
    line-height: 1.5;
    max-width: 68ch;
  }

  .intro {
    margin-bottom: 14px;
  }

  code,
  pre {
    font-family: ui-monospace, monospace;
    font-size: 0.74rem;
  }

  a {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--series-resting);
    text-decoration: none;
  }

  a:hover {
    text-decoration: underline;
  }

  a:focus-visible,
  .action:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
    border-radius: 4px;
  }

  .sep {
    color: var(--text-muted);
  }

  /* What it is made of */

  .pieces {
    list-style: none;
    margin: 0;
    padding: 0;
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .pieces > li {
    padding: 13px 16px;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .pieces > li + li {
    border-top: 1px solid var(--gridline);
  }

  .piece-head {
    display: flex;
    align-items: baseline;
    gap: 10px;
  }

  .kind {
    font-size: 0.7rem;
    color: var(--text-muted);
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 0 8px;
  }

  .structure .kind {
    color: var(--series-resting);
    border-color: var(--series-resting);
  }

  .port {
    margin-left: auto;
    font-family: ui-monospace, monospace;
    font-size: 0.74rem;
    color: var(--text-muted);
  }

  .parts code {
    color: var(--text-primary);
    overflow-wrap: anywhere;
  }

  .role {
    font-size: 0.84rem;
    color: var(--text-secondary);
    max-width: 74ch;
  }

  .meta {
    font-size: 0.78rem;
    color: var(--text-muted);
    max-width: 74ch;
  }

  .meta code {
    color: var(--text-secondary);
  }

  /* Your own measurement */

  .steps {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .steps > li {
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

  .step {
    min-width: 0;
    flex: 1;
  }

  .step > p {
    margin-top: 4px;
    font-size: 0.84rem;
    color: var(--text-secondary);
    max-width: 74ch;
  }

  .files {
    list-style: none;
    margin: 8px 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .files code {
    color: var(--text-secondary);
    overflow-wrap: anywhere;
  }

  .command {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin-top: 8px;
    padding: 8px 10px;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: 7px;
  }

  .command pre {
    flex: 1;
    margin: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    color: var(--text-primary);
  }

  .actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
    margin-top: 10px;
    font-size: 0.82rem;
  }

  .action {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    background: transparent;
    color: var(--series-resting);
    border: 1px solid var(--border);
    border-radius: 7px;
    padding: 6px 13px;
    font-size: 0.82rem;
    cursor: pointer;
  }

  .action:hover {
    border-color: var(--series-resting);
  }

  /* Where it goes wrong */

  .pitfalls {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .pitfalls > li {
    padding: 9px 0;
    border-top: 1px solid var(--gridline);
  }

  summary {
    cursor: pointer;
    font-size: 0.86rem;
    font-weight: 600;
  }

  summary::marker {
    color: var(--text-muted);
  }

  summary:focus-visible {
    outline: 2px solid var(--series-resting);
    outline-offset: 2px;
    border-radius: 4px;
  }

  details p {
    margin: 5px 0 2px;
    font-size: 0.82rem;
    color: var(--text-secondary);
    max-width: 78ch;
  }

  .footer {
    margin-top: 26px;
    padding-top: 14px;
    border-top: 1px solid var(--gridline);
    font-size: 0.82rem;
    color: var(--text-muted);
  }
</style>
