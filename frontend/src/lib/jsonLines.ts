/**
 * Pretty-prints JSON one line at a time, recording the RFC 6901 pointer each line belongs to.
 *
 * <p>A tree widget would be the obvious choice, but the point of this view is to show real FHIR and
 * real openEHR the way they look in a file. Keeping the pointer per line is what lets the inspector
 * highlight "this is the bit the mapping wrote" in both documents at once.
 */

export interface JsonLine {
  /** Pointer of the node this line belongs to; '' is the document root. */
  pointer: string;
  indent: number;
  /** Object key, quoted, or null inside an array and on closing lines. */
  key: string | null;
  /** The scalar or the bracket that opens or closes a container. */
  text: string;
  /** Trailing comma, kept separate so it is not part of the highlighted value. */
  comma: boolean;
  kind: 'open' | 'close' | 'scalar';
}

const ESCAPE = (segment: string): string => segment.replace(/~/g, '~0').replace(/\//g, '~1');

export function jsonLines(value: unknown): JsonLine[] {
  const lines: JsonLine[] = [];
  walk(value, '', null, 0, false);
  return lines;

  function walk(
    node: unknown,
    pointer: string,
    key: string | null,
    indent: number,
    comma: boolean,
  ): void {
    if (Array.isArray(node)) {
      if (node.length === 0) {
        lines.push({ pointer, indent, key, text: '[]', comma, kind: 'scalar' });
        return;
      }
      lines.push({ pointer, indent, key, text: '[', comma: false, kind: 'open' });
      node.forEach((item, index) =>
        walk(item, `${pointer}/${index}`, null, indent + 1, index < node.length - 1),
      );
      lines.push({ pointer, indent, key: null, text: ']', comma, kind: 'close' });
      return;
    }

    if (node !== null && typeof node === 'object') {
      const entries = Object.entries(node as Record<string, unknown>);
      if (entries.length === 0) {
        lines.push({ pointer, indent, key, text: '{}', comma, kind: 'scalar' });
        return;
      }
      lines.push({ pointer, indent, key, text: '{', comma: false, kind: 'open' });
      entries.forEach(([name, item], index) =>
        walk(
          item,
          `${pointer}/${ESCAPE(name)}`,
          `"${name}"`,
          indent + 1,
          index < entries.length - 1,
        ),
      );
      lines.push({ pointer, indent, key: null, text: '}', comma, kind: 'close' });
      return;
    }

    lines.push({
      pointer,
      indent,
      key,
      text: JSON.stringify(node) ?? 'null',
      comma,
      kind: 'scalar',
    });
  }
}

/** True when `line` sits at or below `pointer` — i.e. the highlight covers it. */
export function covers(pointer: string | null, line: JsonLine): boolean {
  if (pointer === null) return false;
  return line.pointer === pointer || line.pointer.startsWith(`${pointer}/`);
}
