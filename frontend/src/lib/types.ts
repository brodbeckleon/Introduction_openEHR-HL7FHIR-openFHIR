/** Mirrors the records the Gradle backend serialises. */

export interface DailyRestingHeartRate {
  /** ISO date, e.g. "2026-08-13". */
  date: string;
  resting: number;
}

export interface HeartRateSeries {
  from: string;
  to: string;
  days: DailyRestingHeartRate[];
}

/** One issue of a FHIR OperationOutcome — what the import endpoint answers with. */
export interface OutcomeIssue {
  severity: 'fatal' | 'error' | 'warning' | 'information';
  code: string;
  diagnostics?: string;
}

export interface OperationOutcome {
  resourceType: 'OperationOutcome';
  issue: OutcomeIssue[];
}

/* The pipeline inspector: one run of the import with every intermediate form kept. */

/** Which model a representation is written in — drives the colour it is shown in. */
export type Standard = 'none' | 'fhir' | 'openehr';

/** One field the trip through openEHR did not preserve. */
export interface RoundTripDifference {
  pointer: string;
  kind: 'lost' | 'changed' | 'added';
  before?: string;
  after?: string;
}

export interface TraceStep {
  id: string;
  title: string;
  /** Backend, openFHIR, EHRbase or Client — who produced this representation. */
  actor: string;
  standard: Standard;
  /** The HTTP call that produced it, when a server was involved. */
  call?: string;
  explanation: string;
  note?: string;
  status: 'ok' | 'error';
  json?: unknown;
  durationMs?: number;
  differences?: RoundTripDifference[];
  /** The AQL, kept beside the JSON so it can be shown as a query rather than an escaped string. */
  query?: string;
}

/**
 * One correspondence between the FHIR Bundle and the openEHR composition, with the FHIR Connect
 * rule that produced it. Pointers are RFC 6901, relative to their own document.
 */
export interface MappingLink {
  id: string;
  label: string;
  kind: 'mapped' | 'selector' | 'generated';
  explanation: string;
  fhirPointer?: string;
  openehrPointer?: string;
  mappingFile?: string;
  fromLine?: number;
  toLine?: number;
}

export interface MappingSource {
  file: string;
  title: string;
  content: string;
  /** Whether the file no longer matches the copy the application was built with. */
  edited: boolean;
}

/** What one save of a mapping did. */
export interface SaveResult {
  /** Whether openFHIR accepted the mappings after the file was written. */
  applied: boolean;
  /** openFHIR's complaint, when it did not. */
  detail?: string;
  mappings: MappingSource[];
}

export interface Trace {
  inputKind: 'bundle' | 'observation' | 'unknown';
  inputLabel: string;
  stored: boolean;
  steps: TraceStep[];
  links: MappingLink[];
  mappings: MappingSource[];
}

/** A ready-made input, each chosen to show one thing the standards do. */
export interface TraceSample {
  id: string;
  label: string;
  summary: string;
  json: unknown;
}

/**
 * One HTTP call the backend made to a standards server. No headers: EHRbase's basic auth has no
 * business in the browser, and the bodies are the part worth reading anyway.
 */
export interface TrafficEntry {
  seq: number;
  /** ISO instant. */
  at: string;
  server: 'openFHIR' | 'EHRbase';
  method: string;
  path: string;
  query?: string;
  /** Absent when the call never got an answer. */
  status?: number;
  durationMs: number;
  requestBody?: string;
  responseBody?: string;
  error?: string;
}

/* The openEHR revision history of one day — what the record holds, and what it used to hold. */

export interface DayVersion {
  versionUid: string;
  version: number;
  /** ISO instant. */
  committed?: string;
  /** openEHR's own word: creation, modification, amendment, deleted. */
  changeType?: string;
  committer?: string;
  /** The rate as this version recorded it. */
  bpm?: number;
  /** Whether this is the version in force now. */
  current: boolean;
}

export interface CompositionHistory {
  uid: string;
  versions: DayVersion[];
}

export interface DayHistory {
  date: string;
  compositions: CompositionHistory[];
}

/* The AQL playground. */

export interface AqlColumn {
  /** The AQL path this column selects — absent for an aggregate. */
  path?: string;
  name: string;
}

export interface AqlResult {
  columns: AqlColumn[];
  rows: unknown[][];
  /** How many rows the store found, which can exceed what was returned. */
  returned: number;
  truncated: boolean;
  durationMs: number;
  error?: string;
}

export interface AqlExample {
  id: string;
  label: string;
  /** The one thing this query is here to show. */
  teaches: string;
  query: string;
}

/* The template explorer: what the model allows, and how much of it the data uses. */

export interface TemplateNode {
  /** The reference model type — COMPOSITION, OBSERVATION, DV_QUANTITY. */
  rmType: string;
  /** The parent attribute this is reached through, e.g. `events`. */
  attribute?: string;
  /** The archetype node id, where there is one. */
  nodeId?: string;
  /** The archetype's own word for this node — "Rate", "Any event". */
  name?: string;
  /** Set where a new archetype begins here. */
  archetype?: string;
  /** What the template permits: `1..1`, `0..*`. */
  occurrences?: string;
  mandatory: boolean;
  /** The openEHR path, which is what AQL addresses. */
  path: string;
  /** Whether the stored data reaches this node. */
  filled: boolean;
  children: TemplateNode[];
}

export interface TemplateView {
  templateId: string;
  root: TemplateNode;
  nodes: number;
  filled: number;
  /** False when the record is empty, in which case nothing is marked. */
  hasData: boolean;
}

/** One rule read out of a FHIR Connect file: what it connects, and where it is written. */
export interface MappingRule {
  file: string;
  name: string;
  /** `correspondence` connects two paths; `constant` writes a fixed value into outgoing FHIR. */
  kind: 'correspondence' | 'constant';
  /** How deeply nested — a nested rule only applies inside its parent. */
  depth: number;
  /** The FHIR path, or the path a constant is written to. */
  fhir: string;
  /** Absent on a constant. */
  openehr?: string;
  /** Only on a constant. */
  value?: string;
  fromLine: number;
  toLine: number;
}
