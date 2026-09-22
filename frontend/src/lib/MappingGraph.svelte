<script lang="ts">
  import { buildGraph } from './mappingGraph';
  import { t } from './i18n.svelte';
  import type { MappingRule } from './types';

  interface Props {
    rules: MappingRule[];
    /** Opens the rule in the editor below, the same way the list does. */
    onselect: (rule: MappingRule) => void;
  }

  const { rules, onselect }: Props = $props();

  let container = $state<HTMLDivElement | null>(null);
  let error = $state<string | null>(null);
  let drawing = $state(true);

  /** Bumped by the colour-scheme listener; reading it in the effect is what redraws on a theme change. */
  let scheme = $state(0);

  $effect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const follow = () => (scheme += 1);
    media.addEventListener('change', follow);
    return () => media.removeEventListener('change', follow);
  });

  $effect(() => {
    // Both are dependencies: the rules change when a mapping is edited, the scheme when the theme
    // is. The generation goes into the diagram id below, which both reads the rune and keeps
    // mermaid from being handed an id it has already rendered.
    const current = rules;
    const generation = scheme;
    const target = container;
    if (!target) return;

    let cancelled = false;
    void (async () => {
      drawing = true;
      try {
        // Lazily: mermaid is by far the largest thing this app depends on, and it is only needed by
        // whoever opens this one view.
        const mermaid = (await import('mermaid')).default;
        const css = getComputedStyle(document.documentElement);
        const read = (name: string) => css.getPropertyValue(name).trim();

        mermaid.initialize({
          startOnLoad: false,
          // Strict on purpose. The labels below come from YAML files this app lets you edit, so
          // 'loose' — which is what mermaid's own click directives need — would turn the mapping
          // editor into a way to run scripts.
          securityLevel: 'strict',
          // Named explicitly: mermaid 12 otherwise reaches for elk, whose layout engine is 1.4 MB
          // on its own and is not needed to lay out nine rules.
          layout: 'dagre',
          theme: 'base',
          themeVariables: {
            background: read('--page'),
            primaryColor: read('--surface-1'),
            primaryTextColor: read('--text-primary'),
            primaryBorderColor: read('--border'),
            lineColor: read('--text-muted'),
            secondaryColor: read('--band-fill'),
            tertiaryColor: read('--page'),
            fontFamily: read('--font') || 'system-ui, sans-serif',
            fontSize: '13px',
          },
        });

        const { source, nodes } = buildGraph(current);
        const { svg } = await mermaid.render(`mapping-graph-${generation}`, source);
        if (cancelled || !target) return;
        target.innerHTML = svg;

        // Clicks are wired here rather than with mermaid's `click` directive, which needs the loose
        // security level. The rendered id is `<diagramId>-flowchart-<nodeId>-<n>`, so the node id is
        // read back out of it rather than the prefix being assumed — that shape is mermaid's to
        // change, and guessing it wrong fails silently as a diagram nobody can click.
        const byId = new Map(nodes.map(({ id, rule }) => [id, rule]));
        let wired = 0;
        for (const element of target.querySelectorAll<SVGGElement>('g.node')) {
          // Greedy up to the trailing counter, and the map decides whether it is one of ours: a
          // narrower pattern is one more thing to keep in step with how ids are generated.
          const nodeId = /-flowchart-(.+)-\d+$/.exec(element.id)?.[1];
          const rule = nodeId === undefined ? undefined : byId.get(nodeId);
          if (!rule) continue;
          element.style.cursor = 'pointer';
          element.setAttribute('role', 'button');
          element.setAttribute('tabindex', '0');
          element.setAttribute('aria-label', t('graph.open', rule.name));
          element.addEventListener('click', () => onselect(rule));
          element.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              onselect(rule);
            }
          });
          wired += 1;
        }
        if (wired === 0 && nodes.length > 0) {
          // Loud rather than a diagram that quietly does nothing when clicked.
          console.warn(
            '[MappingGraph] no nodes could be wired — mermaid id shape may have changed',
          );
        }
        error = null;
      } catch (cause) {
        error = cause instanceof Error ? cause.message : String(cause);
      } finally {
        if (!cancelled) drawing = false;
      }
    })();

    return () => {
      cancelled = true;
    };
  });
</script>

<section class="graph">
  <h3>{t('graph.title')}</h3>
  <p class="meta">{t('graph.lede')}</p>

  {#if error}
    <p class="error" role="alert">{error}</p>
  {:else if drawing}
    <p class="meta">{t('graph.drawing')}</p>
  {/if}

  <div class="canvas" bind:this={container}></div>
</section>

<style>
  .graph {
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 14px 16px;
  }

  h3 {
    margin: 0 0 4px;
    font-size: 0.95rem;
  }

  .meta {
    margin: 0 0 12px;
    max-width: 62ch;
    color: var(--text-secondary);
    font-size: 0.85rem;
    line-height: 1.5;
  }

  /* A wide graph scrolls rather than shrinking the labels past reading. */
  .canvas {
    overflow-x: auto;
  }

  .canvas :global(svg) {
    max-width: none;
    height: auto;
  }

  .canvas :global(g.node:hover rect),
  .canvas :global(g.node:focus-visible rect) {
    stroke: var(--series-resting);
    stroke-width: 2;
  }

  .error {
    color: var(--critical);
    font-size: 0.85rem;
  }
</style>
