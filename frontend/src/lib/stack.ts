/**
 * What the application is built from, for the Build your own tab.
 *
 * Exact versions, because the tab is for people rebuilding the same structure, and some of what it
 * teaches only holds for one version: what FHIR Connect can express depends on which openFHIR runs
 * it. A list like this goes stale the moment an image is bumped and the list is forgotten, so it is
 * not trusted — `StackListTest` in the backend checks every image `docker-compose.yml` pulls and
 * every version the Gradle build pins against the `parts` below, verbatim.
 *
 * The frontend is not listed. It is this teaching page, not part of the structure: someone building
 * the same needs no frontend at all, and listing one invites copying it.
 *
 * What each piece is for lives in the dictionaries, under `build.piece.<id>`.
 */

export type PieceId = 'ehrbase' | 'openfhir' | 'fhirStore' | 'backend';

export interface Piece {
  id: PieceId;
  /** What it is made of, the piece itself first and what runs behind it after. */
  parts: string[];
  /** The port `docker-compose.yml` publishes it on. */
  port: number;
  /**
   * Part of the structure itself — the thing to keep when building the same — rather than this
   * particular application, which is yours to replace.
   */
  structure: boolean;
  /** Where the versions are pinned: the file to change when one moves. */
  pinnedIn: string[];
  docs: string;
}

export const STACK: Piece[] = [
  {
    id: 'ehrbase',
    parts: ['ehrbase/ehrbase:2.35.1', 'ehrbase/ehrbase-v2-postgres:16.2'],
    port: 18081,
    structure: true,
    pinnedIn: ['docker-compose.yml'],
    docs: 'https://docs.ehrbase.org/',
  },
  {
    id: 'openfhir',
    parts: ['openfhir/openfhir:3.0.1', 'mongo:7'],
    port: 18082,
    structure: true,
    pinnedIn: ['docker-compose.yml'],
    docs: 'https://open-fhir.com/documentation/latest/index.html',
  },
  {
    // A database, not a server: the backend is the only thing that reads it.
    id: 'fhirStore',
    parts: ['postgres:16'],
    port: 18084,
    structure: true,
    pinnedIn: ['docker-compose.yml'],
    docs: 'https://hl7.org/fhir/R4/patient.html',
  },
  {
    id: 'backend',
    parts: ['Spring Boot 3.5.6', 'Java 21', 'HAPI FHIR 8.8.1'],
    port: 18080,
    structure: false,
    pinnedIn: ['backend/build.gradle'],
    docs: 'https://hapifhir.io/hapi-fhir/docs/',
  },
];
