<script lang="ts">
  import Term from './Term.svelte';

  /**
   * Prose with glossary terms marked inline: `[[COMPOSITION]]`, or `[[template_id|Template]]` when
   * the word on screen differs from the glossary key.
   *
   * The alternative — splitting a sentence into fragments around `<Term>` elements in the markup —
   * puts the spacing and punctuation between the fragments in the *markup* rather than in the
   * translation. That breaks as soon as two languages disagree about it, and they do: English wants
   * a space after the term where German wants a comma directly against it. Keeping the sentence in
   * one string lets each translation own its own punctuation.
   */

  interface Props {
    text: string;
  }

  const { text }: Props = $props();

  type Part = { term: string; label: string } | { plain: string };

  const parts = $derived.by<Part[]>(() => {
    const out: Part[] = [];
    const pattern = /\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/g;
    let last = 0;
    let match: RegExpExecArray | null;

    while ((match = pattern.exec(text)) !== null) {
      if (match.index > last) out.push({ plain: text.slice(last, match.index) });
      out.push({ term: match[1]!, label: match[2] ?? match[1]! });
      last = match.index + match[0].length;
    }
    if (last < text.length) out.push({ plain: text.slice(last) });
    return out;
  });
</script>

<!-- prettier-ignore -->
{#each parts as part, index (index)}{#if 'term' in part}<Term term={part.term}>{part.label}</Term>{:else}{part.plain}{/if}{/each}
