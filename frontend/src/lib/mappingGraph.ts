/**
 * Turns the FHIR Connect rules into mermaid source.
 *
 * Kept out of the component so it can be reasoned about — and so the escaping below is in one
 * place. The rules are read from YAML files this app lets you edit, so every path in them is
 * untrusted text as far as the diagram is concerned.
 */
import type { MappingRule } from './types';

/** A node in the built diagram, and the rule that put it there. */
export interface GraphNode {
  /** The mermaid id — generated, never derived from a path. */
  id: string;
  rule: MappingRule;
}

export interface BuiltGraph {
  source: string;
  /** Mermaid id → the rule to open when that node is chosen. */
  nodes: GraphNode[];
}

/**
 * Shortens an openEHR path to its ends.
 *
 * A full archetype path is 90 characters of brackets and would make every box the width of the
 * screen. The first segment says which artefact, the last two say where in it — the middle is
 * structure the reader can take on trust and read in full in the file below.
 */
export function shorten(path: string): string {
  const parts = path.split('/').filter(Boolean);
  if (parts.length <= 3) return path;
  return `${parts[0]}/…/${parts.slice(-2).join('/')}`;
}

/**
 * Makes a string safe inside a mermaid label.
 *
 * Mermaid takes quoted labels but has no escape for a quote inside one; its own convention is the
 * HTML entity. Square brackets and braces end a node's shape when they arrive unquoted, and
 * archetype paths are full of them.
 */
function label(text: string): string {
  return text
    .replaceAll('"', '#quot;')
    .replaceAll('[', '#91;')
    .replaceAll(']', '#93;')
    .replaceAll('{', '#123;')
    .replaceAll('}', '#125;');
}

/**
 * Builds the diagram: one node per rule, nested the way the files nest them.
 *
 * The first version of this drew each rule as a FHIR box, an openEHR box and a line between — nine
 * parallel pairs, which is the list above redrawn as boxes and says nothing it does not. The
 * structure worth seeing is the one a flat list hides: rules contain rules. `pulseParent` holds
 * `iteratePulse` holds `pulseSlot`; the three constants hang off `hardcodedMappings`. That nesting
 * is what decides when a rule applies at all, and it is right there in the files as indentation.
 */
export function buildGraph(rules: MappingRule[]): BuiltGraph {
  const lines: string[] = ['flowchart TB'];
  const nodes: GraphNode[] = [];

  if (rules.length === 0) {
    return { source: 'flowchart TB\n  empty["no rules"]', nodes };
  }

  const files = [...new Set(rules.map((rule) => rule.file))];

  files.forEach((file, fileIndex) => {
    const inFile = rules.filter((rule) => rule.file === file);
    lines.push(`  subgraph g${fileIndex}["${label(file)}"]`);
    lines.push('    direction TB');

    inFile.forEach((rule, index) => {
      const id = `n${fileIndex}_${index}`;
      lines.push(`    ${id}["${label(describe(rule))}"]`);
      nodes.push({ id, rule });
    });

    // A rule's parent is the nearest one before it that sits shallower — which is exactly what the
    // indentation in the YAML means.
    inFile.forEach((rule, index) => {
      for (let before = index - 1; before >= 0; before--) {
        const candidate = inFile[before];
        if (candidate && candidate.depth < rule.depth) {
          lines.push(`    n${fileIndex}_${before} --> n${fileIndex}_${index}`);
          break;
        }
      }
    });

    lines.push('  end');
  });

  return { source: lines.join('\n'), nodes };
}

/** A rule as one line: what it is called, and what it connects or writes. */
function describe(rule: MappingRule): string {
  if (rule.kind === 'constant') {
    return `${rule.name}<br/>${rule.fhir} = ${rule.value ?? ''}`;
  }
  const openehr = rule.openehr ? shorten(rule.openehr) : '?';
  return `${rule.name}<br/>${shorten(rule.fhir)} ↔ ${openehr}`;
}
