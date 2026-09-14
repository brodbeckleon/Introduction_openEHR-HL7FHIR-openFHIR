<script lang="ts">
  import type { DailyRestingHeartRate, HeartRateSeries } from './types';
  import { locale, t } from './i18n.svelte';

  /**
   * Thirty days of resting heart rate.
   *
   * One series, so no legend box — the caption names it. The line breaks over gaps rather
   * than drawing a trend through days that were never recorded.
   */
  let { series }: { series: HeartRateSeries } = $props();

  const HEIGHT = 300;
  const PAD = { top: 16, right: 16, bottom: 30, left: 40 };

  let width = $state(720);
  let hovered = $state<Slot | null>(null);

  /** One calendar day, with its value if the day has one. */
  interface Slot {
    index: number;
    date: string;
    day: DailyRestingHeartRate | null;
  }

  const dayCount = $derived(daysBetween(series.from, series.to));

  const slots = $derived.by<Slot[]>(() => {
    const byDate = new Map(series.days.map((day) => [day.date, day]));
    return Array.from({ length: dayCount }, (_unused, index) => {
      const date = addDays(series.from, index);
      return { index, date, day: byDate.get(date) ?? null };
    });
  });

  const withData = $derived(
    slots.filter((slot): slot is Slot & { day: DailyRestingHeartRate } => slot.day !== null),
  );

  const bounds = $derived.by(() => {
    const values = series.days.map((day) => day.resting);
    if (values.length === 0) return { low: 40, high: 100 };
    const low = Math.floor((Math.min(...values) - 5) / 5) * 5;
    const high = Math.ceil((Math.max(...values) + 5) / 5) * 5;
    return { low: Math.max(0, low), high: Math.max(high, low + 15) };
  });

  const plot = $derived({
    left: PAD.left,
    right: width - PAD.right,
    top: PAD.top,
    bottom: HEIGHT - PAD.bottom,
  });

  const slotWidth = $derived((plot.right - plot.left) / Math.max(dayCount, 1));

  function x(index: number): number {
    return plot.left + (index + 0.5) * slotWidth;
  }

  function y(value: number): number {
    const { low, high } = bounds;
    return plot.bottom - ((value - low) / (high - low)) * (plot.bottom - plot.top);
  }

  const ticks = $derived.by<number[]>(() => {
    const { low, high } = bounds;
    const step = high - low > 40 ? 10 : 5;
    const result: number[] = [];
    for (let value = low; value <= high; value += step) result.push(value);
    return result;
  });

  /** Line segments, broken wherever a day has no value. */
  const segments = $derived.by<string[]>(() => {
    const runs: (Slot & { day: DailyRestingHeartRate })[][] = [];
    let current: (Slot & { day: DailyRestingHeartRate })[] = [];
    for (const slot of slots) {
      if (slot.day) current.push(slot as Slot & { day: DailyRestingHeartRate });
      else {
        if (current.length > 1) runs.push(current);
        current = [];
      }
    }
    if (current.length > 1) runs.push(current);
    return runs.map((run) => run.map((s) => `${x(s.index)},${y(s.day.resting)}`).join(' '));
  });

  const dateLabels = $derived.by<Slot[]>(() => {
    if (dayCount === 0) return [];
    const every = dayCount > 20 ? 7 : dayCount > 10 ? 3 : 1;
    const labels = slots.filter((slot) => slot.index % every === 0);
    const last = slots[dayCount - 1];
    if (!last) return labels;
    // Only label the final day when it will not collide with the previous tick.
    const previous = labels[labels.length - 1];
    if (!previous || last.index - previous.index >= every / 2) labels.push(last);
    return labels;
  });

  function onPointerMove(event: PointerEvent & { currentTarget: SVGSVGElement }) {
    const rect = event.currentTarget.getBoundingClientRect();
    const offset = ((event.clientX - rect.left) / rect.width) * width;
    const index = Math.floor((offset - plot.left) / slotWidth);
    hovered = slots[Math.min(Math.max(index, 0), dayCount - 1)] ?? null;
  }

  function daysBetween(from: string, to: string): number {
    return Math.round((Date.parse(to) - Date.parse(from)) / 86_400_000) + 1;
  }

  function addDays(from: string, count: number): string {
    return new Date(Date.parse(from) + count * 86_400_000).toISOString().slice(0, 10);
  }

  function shortDate(date: string): string {
    return new Date(date).toLocaleDateString(locale(), { month: 'short', day: 'numeric' });
  }

  const tooltipX = $derived(hovered ? Math.min(Math.max(x(hovered.index), 80), width - 80) : 0);
</script>

<figure class="chart">
  <figcaption>
    <h2>{t('chart.title', dayCount)}</h2>
    <p>
      {t('chart.subtitle', withData.length, dayCount)}
    </p>
  </figcaption>

  <div class="plot" bind:clientWidth={width}>
    <svg
      viewBox="0 0 {width} {HEIGHT}"
      role="img"
      aria-label={t('chart.title', dayCount)}
      onpointermove={onPointerMove}
      onpointerleave={() => (hovered = null)}
    >
      {#each ticks as tick (tick)}
        <line class="grid" x1={plot.left} x2={plot.right} y1={y(tick)} y2={y(tick)} />
        <text
          class="tick"
          x={plot.left - 8}
          y={y(tick)}
          text-anchor="end"
          dominant-baseline="middle">{tick}</text
        >
      {/each}

      <line class="axis" x1={plot.left} x2={plot.right} y1={plot.bottom} y2={plot.bottom} />

      {#each dateLabels as slot (slot.date)}
        <text class="tick" x={x(slot.index)} y={plot.bottom + 18} text-anchor="middle">
          {shortDate(slot.date)}
        </text>
      {/each}

      {#if hovered}
        <line
          class="crosshair"
          x1={x(hovered.index)}
          x2={x(hovered.index)}
          y1={plot.top}
          y2={plot.bottom}
        />
      {/if}

      {#each segments as points, index (index)}
        <polyline class="resting" {points} />
      {/each}

      {#each withData as slot (slot.date)}
        <circle
          class="point"
          cx={x(slot.index)}
          cy={y(slot.day.resting)}
          r={hovered?.date === slot.date ? 6 : 4}
        />
      {/each}
    </svg>

    {#if hovered}
      <div class="tooltip" style="left: {(tooltipX / width) * 100}%">
        <strong>{shortDate(hovered.date)}</strong>
        {#if hovered.day}
          <p>{hovered.day.resting} bpm resting</p>
        {:else}
          <p class="empty">Not recorded</p>
        {/if}
      </div>
    {/if}
  </div>
</figure>

<style>
  .chart {
    margin: 0;
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 20px;
  }

  figcaption h2 {
    font-size: 1.05rem;
  }

  figcaption p {
    margin: 4px 0 14px;
    color: var(--text-secondary);
    font-size: 0.85rem;
    max-width: 60ch;
  }

  .plot {
    position: relative;
  }

  svg {
    display: block;
    width: 100%;
    height: auto;
    touch-action: none;
  }

  .grid {
    stroke: var(--gridline);
    stroke-width: 1;
  }

  .axis {
    stroke: var(--axis);
    stroke-width: 1;
  }

  .crosshair {
    stroke: var(--axis);
    stroke-width: 1;
    stroke-dasharray: 3 3;
  }

  .tick {
    fill: var(--text-muted);
    font-size: 11px;
    font-variant-numeric: tabular-nums;
  }

  .resting {
    fill: none;
    stroke: var(--series-resting);
    stroke-width: 2;
    stroke-linejoin: round;
    stroke-linecap: round;
  }

  .point {
    fill: var(--series-resting);
    stroke: var(--surface-1);
    stroke-width: 2;
  }

  .tooltip {
    position: absolute;
    top: 8px;
    transform: translateX(-50%);
    background: var(--surface-1);
    border: 1px solid var(--border);
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    padding: 8px 10px;
    font-size: 0.8rem;
    pointer-events: none;
    white-space: nowrap;
  }

  .tooltip strong {
    display: block;
    margin-bottom: 2px;
  }

  .tooltip p {
    margin: 0;
    font-variant-numeric: tabular-nums;
  }

  .tooltip .empty {
    color: var(--text-muted);
  }
</style>
