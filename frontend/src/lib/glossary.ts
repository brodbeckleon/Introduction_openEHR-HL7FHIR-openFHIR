/**
 * The vocabulary this app puts on screen, explained where it appears.
 *
 * An openEHR composition is unreadable until someone tells you that `at0004` is a name from an
 * archetype and `DV_QUANTITY` is a type from the reference model. Rather than a glossary page
 * nobody opens, every known term is hoverable in place — in the JSON panels and in the prose.
 *
 * Keep the explanations to what someone needs *right there*: what it is, and why it is not the
 * thing they might mistake it for.
 */

import { language } from './i18n.svelte';

export interface GlossaryEntry {
  /** Heading of the tooltip; usually the term itself, spelled out where that helps. */
  title: string;
  /** Which model it belongs to — drives the colour, same as everywhere else. */
  standard: 'fhir' | 'openehr' | 'both';
  body: string;
  /** Optional second paragraph: the distinction people actually get wrong. */
  note?: string;
}

/** The German body and note for a term. Titles stay as they are — they are the spec's own words. */
interface GermanEntry {
  body: string;
  note?: string;
}

/**
 * Terms are matched case-sensitively against whole tokens, so `value` does not light up every
 * `valueQuantity`. Keys and scalar values are both looked up.
 */
const ENTRIES: Record<string, GlossaryEntry> = {
  // ── openEHR reference model: the types ────────────────────────────────────
  COMPOSITION: {
    title: 'COMPOSITION',
    standard: 'openehr',
    body:
      'The unit openEHR stores and versions — one clinical document: an encounter, a lab report, ' +
      'a discharge summary. Everything else here sits inside one.',
    note: 'It is what gets a version uid. Correcting a value adds a version of the whole composition, not of the number.',
  },
  OBSERVATION: {
    title: 'OBSERVATION',
    standard: 'openehr',
    body:
      'Something observed about the patient, as opposed to an EVALUATION (an opinion, like a ' +
      'diagnosis), an INSTRUCTION (an order) or an ACTION (what was done).',
    note: 'openEHR calls these the entry types, and choosing the right one is a modelling decision, not a formatting one.',
  },
  HISTORY: {
    title: 'HISTORY',
    standard: 'openehr',
    body:
      'The time structure every OBSERVATION carries. Even a single reading lives in a history with ' +
      'one event, because openEHR treats "when" as part of the model rather than a field.',
  },
  POINT_EVENT: {
    title: 'POINT_EVENT',
    standard: 'openehr',
    body: 'A value measured at one moment.',
    note:
      'The alternative is an INTERVAL_EVENT, which carries a math_function and means "the minimum ' +
      'over the day" or "the mean over an hour". FHIR leaves that distinction to the code; openEHR ' +
      'puts it in the structure.',
  },
  INTERVAL_EVENT: {
    title: 'INTERVAL_EVENT',
    standard: 'openehr',
    body:
      'A value that summarises a period — a minimum, maximum or mean — carrying a math_function ' +
      'saying which, and a width saying over how long.',
  },
  ITEM_TREE: {
    title: 'ITEM_TREE',
    standard: 'openehr',
    body:
      'A container for the actual data items. openEHR separates the shape of the data (this tree) ' +
      'from the values in it (the ELEMENTs below).',
  },
  ELEMENT: {
    title: 'ELEMENT',
    standard: 'openehr',
    body: 'One named leaf holding one value. The Rate element here is where the heart rate lands.',
  },
  EVENT_CONTEXT: {
    title: 'EVENT_CONTEXT',
    standard: 'openehr',
    body:
      'The circumstances of the recording: when it started, in what care setting, by whom. ' +
      'Mandatory, and nothing in a FHIR Observation is obliged to supply it — openFHIR fills it in.',
  },
  DV_QUANTITY: {
    title: 'DV_QUANTITY',
    standard: 'openehr',
    body: 'A number with a unit — here 58 and /min. The archetype constrains which units are allowed.',
    note:
      'FHIR names the unit twice (a human-readable unit and a coded one from UCUM); openEHR carries ' +
      'one units string, because the constraint lives in the model rather than the instance.',
  },
  DV_TEXT: {
    title: 'DV_TEXT',
    standard: 'openehr',
    body: 'Plain text with no code behind it.',
  },
  DV_CODED_TEXT: {
    title: 'DV_CODED_TEXT',
    standard: 'openehr',
    body:
      'Text that also carries a code from a named terminology — the openEHR counterpart to a FHIR ' +
      'Coding. Both the human-readable value and the machine-readable defining_code travel together.',
  },
  DV_DATE_TIME: {
    title: 'DV_DATE_TIME',
    standard: 'openehr',
    body: 'A date and time, ISO 8601.',
  },
  CODE_PHRASE: {
    title: 'CODE_PHRASE',
    standard: 'openehr',
    body:
      'A code plus the terminology it came from — "433" in the openehr terminology, "en" in ISO 639-1. ' +
      'A code without its terminology means nothing, so openEHR never stores one on its own.',
  },
  TERMINOLOGY_ID: {
    title: 'TERMINOLOGY_ID',
    standard: 'openehr',
    body: 'Names the terminology a code belongs to: openehr, ISO_639-1, ISO_3166-1, SNOMED-CT.',
  },
  PARTY_SELF: {
    title: 'PARTY_SELF',
    standard: 'openehr',
    body:
      'The subject of this record — "the patient whose EHR this is". No identifier is stored, ' +
      'because the EHR itself already says whose it is.',
    note: 'That is a deliberate privacy property: the clinical data carries no patient identity of its own.',
  },

  // ── openEHR: archetypes, templates, paths ─────────────────────────────────
  archetype_node_id: {
    title: 'archetype_node_id',
    standard: 'openehr',
    body:
      'Which node of the archetype this piece of data is. The ids beginning at0 are local to one ' +
      'archetype; the long ones name an archetype itself.',
    note: 'This is what AQL queries address, which is why a query survives changes that would break a SQL one.',
  },
  archetype_id: {
    title: 'archetype_id',
    standard: 'openehr',
    body:
      'The archetype this data was recorded against — a shared, versioned definition of "what a ' +
      'pulse observation looks like", agreed internationally rather than per project.',
  },
  template_id: {
    title: 'template_id',
    standard: 'openehr',
    body:
      'The template narrows archetypes for one specific use: which parts are used, which are ' +
      'mandatory, which terminology applies. EHRbase validates against it before storing.',
    note: 'Two-level modelling: the reference model is the grammar, archetypes are the words, the template is the sentence.',
  },
  rm_version: {
    title: 'rm_version',
    standard: 'openehr',
    body: 'Which version of the openEHR reference model this data is written against.',
  },
  at0001: {
    title: 'at0001',
    standard: 'openehr',
    body: 'In the pulse archetype: the item tree holding the measured values.',
  },
  at0002: {
    title: 'at0002',
    standard: 'openehr',
    body: 'In the pulse archetype: the history — the time structure around the events.',
  },
  at0003: {
    title: 'at0003',
    standard: 'openehr',
    body: 'In the pulse archetype: "Any event", the event this reading is recorded as.',
  },
  at0004: {
    title: 'at0004',
    standard: 'openehr',
    body: 'In the pulse archetype: the Rate element — the heart rate itself.',
    note: 'The id looks cryptic on purpose: it is language-independent, so the same node is "Rate", "Frequenz" or "Fréquence" depending on who is reading.',
  },
  'openEHR-EHR-OBSERVATION.pulse.v2': {
    title: 'openEHR-EHR-OBSERVATION.pulse.v2',
    standard: 'openehr',
    body:
      'The published pulse archetype, version 2. Not written for this project — it comes from the ' +
      'international Clinical Knowledge Manager, which is the point of archetypes.',
  },
  'openEHR-EHR-COMPOSITION.encounter.v1': {
    title: 'openEHR-EHR-COMPOSITION.encounter.v1',
    standard: 'openehr',
    body: 'The composition archetype for a single contact with the health system.',
  },
  composer: {
    title: 'composer',
    standard: 'openehr',
    body:
      'Who recorded this. Mandatory in openEHR and absent from a FHIR Observation, so openFHIR ' +
      'supplies it — one of the places where the two models genuinely disagree about what matters.',
  },
  territory: {
    title: 'territory',
    standard: 'openehr',
    body: 'The country this was recorded in, ISO 3166-1. Mandatory, because clinical meaning can be local.',
  },
  AQL: {
    title: 'AQL — Archetype Query Language',
    standard: 'openehr',
    body:
      'openEHR’s query language. It selects by archetype path rather than by table column, so the ' +
      'same query works against any openEHR system that knows the archetype.',
    note: 'No knowledge of the storage schema is needed or possible — which is why a query written today still runs after the database is restructured.',
  },

  // ── HL7 FHIR ──────────────────────────────────────────────────────────────
  Observation: {
    title: 'Observation',
    standard: 'fhir',
    body:
      'The FHIR resource for a measurement or finding. What it *means* is decided by its code, ' +
      'not by its position in a structure.',
  },
  Bundle: {
    title: 'Bundle',
    standard: 'fhir',
    body: 'A container of resources travelling together — a collection, a search result, a transaction.',
  },
  OperationOutcome: {
    title: 'OperationOutcome',
    standard: 'fhir',
    body:
      'How FHIR reports what happened: one issue per problem, each with a severity. An import that ' +
      'partly succeeded says so here rather than failing whole.',
  },
  resourceType: {
    title: 'resourceType',
    standard: 'fhir',
    body: 'Which FHIR resource this is. Every FHIR resource carries it; it is how a parser knows what it has.',
  },
  valueQuantity: {
    title: 'valueQuantity',
    standard: 'fhir',
    body:
      'The measured value, as a number with a unit. FHIR lets an Observation carry its value in ' +
      'several shapes — valueString, valueCodeableConcept — and the element name says which.',
  },
  effectiveDateTime: {
    title: 'effectiveDateTime',
    standard: 'fhir',
    body:
      'When the observation applies to the patient — not when it was recorded, which is `issued`. ' +
      'A reading taken this morning and typed in tonight has two different times.',
  },
  '40443-4': {
    title: 'LOINC 40443-4',
    standard: 'fhir',
    body: '"Heart rate --resting". The code that makes this a resting heart rate rather than any heart rate.',
    note:
      'The mapping keys off exactly this code. LOINC 8867-4 is also a heart rate, but not a resting ' +
      'one, and the pipeline refuses it.',
  },
  '444981005': {
    title: 'SNOMED CT 444981005',
    standard: 'fhir',
    body:
      '"Resting heart rate" — the same concept as LOINC 40443-4, in a different terminology. Both ' +
      'are correct FHIR, so both are accepted and normalised on the way in.',
  },
  'vital-signs': {
    title: 'vital-signs',
    standard: 'fhir',
    body:
      'The observation category. Together with status=final and the LOINC code, this is what the ' +
      'FHIR Connect mapping matches on to decide the resource is a pulse.',
  },
  LOINC: {
    title: 'LOINC',
    standard: 'fhir',
    body: 'The terminology for "what was measured" — lab tests and clinical measurements.',
  },
  UCUM: {
    title: 'UCUM',
    standard: 'fhir',
    body:
      'The Unified Code for Units of Measure: unambiguous machine-readable units. "/min" is UCUM; ' +
      '"bpm" is what a human writes.',
  },

  // ── The pieces of this system ─────────────────────────────────────────────
  openFHIR: {
    title: 'openFHIR',
    standard: 'both',
    body:
      'The mapping engine between the two. It stores no clinical data — it reads the FHIR Connect ' +
      'files and translates, in both directions.',
  },
  EHRbase: {
    title: 'EHRbase',
    standard: 'openehr',
    body:
      'An open-source openEHR clinical data repository: it validates compositions against the ' +
      'template, stores and versions them, and answers AQL.',
  },
  'FHIR Connect': {
    title: 'FHIR Connect',
    standard: 'both',
    body:
      'The mapping grammar openFHIR executes — declarative YAML saying which FHIR path corresponds ' +
      'to which openEHR path, and under what conditions.',
  },
};

/** Tokens that are only worth explaining when they stand alone as a value, not as a key. */
const VALUE_ONLY = new Set(['40443-4', '444981005', 'vital-signs']);

/**
 * The entry for a token, if there is one. `asKey` distinguishes `"composer":` from `"composer"`.
 *
 * A term with no German text falls back to the English one rather than vanishing — a missing
 * translation should be visible, not silently remove the explanation.
 */
export function lookup(token: string | null | undefined, asKey = false): GlossaryEntry | null {
  if (!token) return null;
  const clean = token.replace(/^"|"$/g, '');
  if (asKey && VALUE_ONLY.has(clean)) return null;

  const entry = ENTRIES[clean];
  if (!entry) return null;
  if (language() !== 'de') return entry;

  const german = GERMAN[clean];
  return german ? { ...entry, body: german.body, note: german.note } : entry;
}

export const glossary = ENTRIES;

/**
 * The German bodies, keyed the same way. Titles are not translated: `DV_QUANTITY` and `at0004` are
 * the spec's own words and the strings you see in the JSON, so translating them would break the
 * recognition the glossary exists to support.
 */
const GERMAN: Record<string, GermanEntry> = {
  COMPOSITION: {
    body:
      'Die Einheit, die openEHR speichert und versioniert — ein klinisches Dokument: ein Kontakt, ' +
      'ein Laborbefund, ein Austrittsbericht. Alles andere hier steckt in einer.',
    note: 'Sie ist es, die eine Version-UID bekommt. Eine Korrektur fügt eine Version der ganzen Composition hinzu, nicht der Zahl.',
  },
  OBSERVATION: {
    body:
      'Etwas am Patienten Beobachtetes — im Gegensatz zu einer EVALUATION (einer Einschätzung, etwa ' +
      'einer Diagnose), einer INSTRUCTION (einer Anordnung) oder einer ACTION (dem, was getan wurde).',
    note: 'openEHR nennt diese die Entry-Typen; den richtigen zu wählen ist eine Modellierungsentscheidung, keine Formatierung.',
  },
  HISTORY: {
    body:
      'Die Zeitstruktur, die jede OBSERVATION trägt. Auch eine einzelne Messung wohnt in einer ' +
      'History mit einem Ereignis, weil openEHR das „Wann" als Teil des Modells behandelt und nicht als Feld.',
  },
  POINT_EVENT: {
    body: 'Ein Wert, gemessen zu einem Zeitpunkt.',
    note:
      'Die Alternative ist ein INTERVAL_EVENT mit math_function: „das Minimum über den Tag" oder ' +
      '„der Mittelwert über eine Stunde". FHIR überlässt diese Unterscheidung dem Code, openEHR steckt sie in die Struktur.',
  },
  INTERVAL_EVENT: {
    body:
      'Ein Wert, der einen Zeitraum zusammenfasst — Minimum, Maximum oder Mittelwert — mit einer ' +
      'math_function, die sagt welches, und einer width, die sagt über wie lange.',
  },
  ITEM_TREE: {
    body:
      'Ein Behälter für die eigentlichen Datenelemente. openEHR trennt die Form der Daten (dieser ' +
      'Baum) von den Werten darin (den ELEMENTs darunter).',
  },
  ELEMENT: {
    body: 'Ein benanntes Blatt mit einem Wert. Das Rate-Element ist hier das, wo der Puls landet.',
  },
  EVENT_CONTEXT: {
    body:
      'Die Umstände der Aufzeichnung: wann sie begann, in welchem Versorgungskontext, durch wen. ' +
      'Pflicht — und nichts an einer FHIR Observation muss das liefern, also füllt openFHIR es.',
  },
  DV_QUANTITY: {
    body: 'Eine Zahl mit Einheit — hier 58 und /min. Der Archetyp schränkt ein, welche Einheiten erlaubt sind.',
    note:
      'FHIR benennt die Einheit zweimal (menschenlesbar und kodiert aus UCUM); openEHR führt eine ' +
      'units-Zeichenkette, weil die Einschränkung im Modell steckt und nicht in der Instanz.',
  },
  DV_TEXT: { body: 'Reiner Text ohne Code dahinter.' },
  DV_CODED_TEXT: {
    body:
      'Text, der zusätzlich einen Code aus einer benannten Terminologie trägt — das openEHR-Gegenstück ' +
      'zu einem FHIR-Coding. Der lesbare Wert und der maschinenlesbare defining_code reisen zusammen.',
  },
  DV_DATE_TIME: { body: 'Datum und Uhrzeit, ISO 8601.' },
  CODE_PHRASE: {
    body:
      'Ein Code samt der Terminologie, aus der er stammt — „433" in der openehr-Terminologie, „en" in ' +
      'ISO 639-1. Ein Code ohne seine Terminologie bedeutet nichts, deshalb speichert openEHR nie einen allein.',
  },
  TERMINOLOGY_ID: {
    body: 'Benennt die Terminologie, zu der ein Code gehört: openehr, ISO_639-1, ISO_3166-1, SNOMED-CT.',
  },
  PARTY_SELF: {
    body:
      'Das Subjekt dieser Akte — „der Patient, dem diese EHR gehört". Es wird kein Identifikator ' +
      'gespeichert, weil die EHR selbst schon sagt, wessen sie ist.',
    note: 'Das ist eine bewusste Datenschutz-Eigenschaft: die klinischen Daten tragen keine eigene Patientenidentität.',
  },

  archetype_node_id: {
    body:
      'Welcher Knoten des Archetyps dieses Datum ist. Die IDs, die mit at0 beginnen, sind lokal zu ' +
      'einem Archetyp; die langen benennen einen Archetyp selbst.',
    note: 'Das ist es, was AQL-Abfragen adressieren — und weshalb eine Abfrage Änderungen übersteht, an denen eine SQL-Abfrage zerbräche.',
  },
  archetype_id: {
    body:
      'Der Archetyp, gegen den diese Daten aufgezeichnet wurden — eine geteilte, versionierte ' +
      'Definition davon, „wie eine Puls-Observation aussieht", international abgestimmt statt pro Projekt.',
  },
  template_id: {
    body:
      'Das Template verengt Archetypen für einen konkreten Einsatz: welche Teile verwendet werden, ' +
      'welche Pflicht sind, welche Terminologie gilt. EHRbase validiert dagegen, bevor es speichert.',
    note: 'Zwei-Ebenen-Modellierung: das Referenzmodell ist die Grammatik, Archetypen sind die Wörter, das Template ist der Satz.',
  },
  rm_version: {
    body: 'Gegen welche Version des openEHR-Referenzmodells diese Daten geschrieben sind.',
  },
  at0001: { body: 'Im Puls-Archetyp: der Item-Tree mit den gemessenen Werten.' },
  at0002: { body: 'Im Puls-Archetyp: die History — die Zeitstruktur um die Ereignisse.' },
  at0003: {
    body: 'Im Puls-Archetyp: „Any event", das Ereignis, als das diese Messung erfasst wird.',
  },
  at0004: {
    body: 'Im Puls-Archetyp: das Rate-Element — der Puls selbst.',
    note: 'Die ID sieht absichtlich kryptisch aus: sie ist sprachunabhängig, derselbe Knoten heißt also „Rate", „Frequenz" oder „Fréquence", je nachdem, wer liest.',
  },
  'openEHR-EHR-OBSERVATION.pulse.v2': {
    body:
      'Der veröffentlichte Puls-Archetyp, Version 2. Nicht für dieses Projekt geschrieben — er kommt ' +
      'aus dem internationalen Clinical Knowledge Manager, und genau darum geht es bei Archetypen.',
  },
  'openEHR-EHR-COMPOSITION.encounter.v1': {
    body: 'Der Composition-Archetyp für einen einzelnen Kontakt mit dem Gesundheitssystem.',
  },
  composer: {
    body:
      'Wer das aufgezeichnet hat. In openEHR Pflicht und in einer FHIR Observation nicht vorhanden, ' +
      'also liefert openFHIR es — eine der Stellen, an denen die beiden Modelle wirklich uneins sind, was zählt.',
  },
  territory: {
    body: 'Das Land, in dem aufgezeichnet wurde, ISO 3166-1. Pflicht, weil klinische Bedeutung lokal sein kann.',
  },
  AQL: {
    body:
      'Die Abfragesprache von openEHR. Sie selektiert über Archetyp-Pfade statt über Tabellenspalten, ' +
      'dieselbe Abfrage funktioniert also gegen jedes openEHR-System, das den Archetyp kennt.',
    note: 'Kenntnis des Speicherschemas ist weder nötig noch möglich — deshalb läuft eine heute geschriebene Abfrage auch nach einem Umbau der Datenbank noch.',
  },

  Observation: {
    body:
      'Die FHIR-Ressource für eine Messung oder einen Befund. Was sie *bedeutet*, entscheidet ihr ' +
      'Code — nicht ihre Position in einer Struktur.',
  },
  Bundle: {
    body: 'Ein Behälter für Ressourcen, die zusammen reisen — eine Sammlung, ein Suchergebnis, eine Transaktion.',
  },
  OperationOutcome: {
    body:
      'Wie FHIR berichtet, was passiert ist: ein Issue pro Problem, jedes mit einer Schwere. Ein ' +
      'teilweise gelungener Import sagt das hier, statt ganz fehlzuschlagen.',
  },
  resourceType: {
    body: 'Welche FHIR-Ressource das ist. Jede FHIR-Ressource trägt es; daran erkennt ein Parser, was er hat.',
  },
  valueQuantity: {
    body:
      'Der gemessene Wert, als Zahl mit Einheit. FHIR lässt eine Observation ihren Wert in mehreren ' +
      'Formen tragen — valueString, valueCodeableConcept — und der Elementname sagt, in welcher.',
  },
  effectiveDateTime: {
    body:
      'Wann die Beobachtung für den Patienten gilt — nicht, wann sie erfasst wurde, das ist `issued`. ' +
      'Eine heute Morgen gemessene und abends eingetippte Messung hat zwei verschiedene Zeiten.',
  },
  '40443-4': {
    body: '„Heart rate --resting". Der Code, der daraus einen Ruhepuls macht statt irgendeiner Herzfrequenz.',
    note: 'Das Mapping prüft genau auf diesen Code. LOINC 8867-4 ist ebenfalls Herzfrequenz, aber nicht die in Ruhe — die Pipeline lehnt sie ab.',
  },
  '444981005': {
    body:
      '„Resting heart rate" — dasselbe Konzept wie LOINC 40443-4, in einer anderen Terminologie. ' +
      'Beides ist korrektes FHIR, also wird beides akzeptiert und beim Hereinkommen normalisiert.',
  },
  'vital-signs': {
    body:
      'Die Kategorie der Observation. Zusammen mit status=final und dem LOINC-Code ist das, worauf ' +
      'das FHIR-Connect-Mapping prüft, um die Ressource als Puls zu erkennen.',
  },
  LOINC: {
    body: 'Die Terminologie für „was gemessen wurde" — Labortests und klinische Messungen.',
  },
  UCUM: {
    body:
      'Der Unified Code for Units of Measure: eindeutige maschinenlesbare Einheiten. „/min" ist UCUM; ' +
      '„bpm" ist, was ein Mensch schreibt.',
  },

  openFHIR: {
    body:
      'Die Mapping-Engine zwischen beiden. Sie speichert keine klinischen Daten — sie liest die ' +
      'FHIR-Connect-Dateien und übersetzt, in beide Richtungen.',
  },
  EHRbase: {
    body:
      'Ein quelloffenes klinisches openEHR-Repository: es validiert Compositions gegen das Template, ' +
      'speichert und versioniert sie und beantwortet AQL.',
  },
  'FHIR Connect': {
    body:
      'Die Mapping-Grammatik, die openFHIR ausführt — deklaratives YAML, das sagt, welcher FHIR-Pfad ' +
      'welchem openEHR-Pfad entspricht und unter welchen Bedingungen.',
  },
};
