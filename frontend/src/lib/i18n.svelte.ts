/**
 * English and German for everything the frontend says.
 *
 * The teaching text of the pipeline itself comes from the backend, which reads `Accept-Language`;
 * `language` below is what every request sends, so the two stay in step. Switching is instant: the
 * chosen language is a rune, so anything reading `t(...)` re-renders, and pending backend text is
 * refetched by the components that own it.
 */

export type Language = 'en' | 'de';

const STORAGE_KEY = 'hrm.language';

function initial(): Language {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'en' || stored === 'de') return stored;
  // Follow the browser on first visit: a German speaker should not have to find the switch.
  return navigator.language?.toLowerCase().startsWith('de') ? 'de' : 'en';
}

let current = $state<Language>(initial());

export function language(): Language {
  return current;
}

export function setLanguage(next: Language): void {
  current = next;
  localStorage.setItem(STORAGE_KEY, next);
  document.documentElement.lang = next;
}

type Dictionary = Record<string, string>;

const EN: Dictionary = {
  'app.title': 'Heart rate monitor',
  'app.lede':
    'Resting heart rates are exchanged as HL7 FHIR Observations, mapped by openFHIR, and stored in openEHR. The chart is the excuse; the point is what happens between those three.',
  'tab.overview': 'Overview',
  'tour.label': 'Guided tour',
  'tour.step': 'Step {0} of {1}',
  'tour.next': 'Next',
  'tour.back': 'Back',
  'tour.stop': 'Stop the tour',
  'tour.close': 'Close',
  'tour.finish': 'Done',
  'tour.start': 'Take the tour',
  'tour.restart': 'Take the tour again',
  'tour.offer':
    'New to these three standards? The tour answers the question the rest of this app assumes you already have: why one heart rate needs three of them.',

  'tour.what.title': 'What this is',
  'tour.what.body':
    'A heart rate monitor: one resting heart rate per day, thirty days of them, drawn as a line. That is the entire feature — and it is the excuse. Everything else here exists to show what it actually takes to store those numbers properly, which turns out to need three healthcare standards. That is what the rest of this tour is about.',
  'tour.number.title': 'One number: 58',
  'tour.number.body':
    'There it is, in the field below. A resting heart rate, measured on one day. Storing it is trivial — any database does that. Storing it so a different hospital, or the same one in twenty years, still knows what it means is the actual problem. That is what these three standards are for.',
  'tour.column.title': 'Why a database column is not enough',
  'tour.column.body':
    'A column `bpm INT` holds 58. It does not hold: in what unit, at rest or under exertion, measured by whom, with what device, in what care setting. A colleague ten years from now can read the number and still not know what it means — and in medicine that is not a small problem.',
  'tour.fhir.title': 'What HL7 FHIR contributes',
  'tour.fhir.body':
    'FHIR makes the number say what it is. LOINC 40443-4 means “heart rate, at rest”; UCUM /min is the unit; the category marks it a vital sign. The meaning sits in codes, which is what lets the resource cross a system boundary and be understood on the other side. Look at what the backend built.',
  'tour.openehr.title': 'What openEHR does differently',
  'tour.openehr.body':
    'The same reading, another logic — and notice the LOINC code is gone. In openEHR the meaning is not in a code but in a place: the value sits inside the pulse archetype, an internationally agreed definition of what a pulse observation is. In exchange openEHR insists on things FHIR never sent: who recorded it, in what language, in what setting.',
  'tour.lost.title': 'Two models, not one in two formats',
  'tour.lost.body':
    'Mapped back to FHIR, some of what went in does not return: the coding systems, the display names, the subject reference. openEHR kept what the archetype models, and everything beyond that had nowhere to live. This is not a bug to fix — it is what it means for two models to be genuinely different.',
  'tour.translate.title': 'So something has to translate',
  'tour.translate.body':
    'That is openFHIR, and this is how it knows what to do: declarative rules saying which FHIR path corresponds to which openEHR path, and under what conditions. Not Java. Changing how the two models correspond means editing this YAML — and you can, right here, including breaking it on purpose to see what the rules were doing.',
  'tour.template.title': 'What the model allows, and what is used',
  'tour.template.body':
    'The third thing the mappings rest on: the operational template. EHRbase validates every composition against it, openFHIR resolves paths against it — and only ten of its twenty-three nodes are ever filled. Open the pulse observation and find the second event slot, an INTERVAL_EVENT named “Maximum” with a math_function: the model has room for the minimum, maximum and mean of a day, named and typed and ready. The mapping simply never writes it. The template says what is possible; the mappings decide what happens.',
  'tour.versions.title': 'The record does not forget',
  'tour.versions.body':
    'Correct a reading and openEHR does not overwrite it — there is no overwrite. It adds a version and keeps the one before. That is why a clinical record can still answer “what did it say on the day someone acted on it?”, which an ordinary database cannot. Try it: record a day twice and open its history.',
  'tour.traffic.title': 'Watch it happen',
  'tour.traffic.body':
    'Every call this application made to the two standards servers, newest first. Open one and you see the openEHR REST API as it really is: a COMPOSITION going to POST /ehr/{id}/composition, answered with 204 and a version uid in an ETag. A correction you just made is the PUT a few rows up. None of this is a diagram of how it might work — it is what happened.',
  'tour.query.title': 'And it stays queryable',
  'tour.query.body':
    'AQL selects by archetype path, not by table column. The same query runs on any openEHR system that knows the archetype, and survives a restructuring of the database underneath. Run one — and try the example with a typo in the path, which is the failure mode worth meeting on purpose.',
  'tour.done.title': 'That is the whole idea',
  'tour.done.body':
    'FHIR carries, openEHR remembers, openFHIR translates. Everything else in this app is detail about how. Underlined words explain themselves when you hover them, and the tour is in the Start here panel whenever you want it again.',

  'tab.pipeline': 'Pipeline inspector',
  'tab.traffic': 'Standards traffic',
  'tab.aql': 'AQL playground',
  'tab.mappings': 'Mappings',

  'mappings.title': 'The FHIR Connect mappings',
  'mappings.lede':
    'Everything that decides how FHIR and openEHR correspond is in these three files. [[FHIR Connect]] is a declarative grammar, so changing the correspondence is editing YAML rather than writing code — and openFHIR re-reads it on request, with no rebuild and no restart.',
  'mappings.focus': 'Opened at the rule behind “{0}”.',
  'rules.title': 'What becomes what',
  'rules.lede':
    'Every rule these files declare, read out of them rather than written here — edit a mapping and this list follows. A [[Observation|FHIR]] path on the left, the [[archetype_node_id|openEHR path]] it corresponds to on the right. Pick one to find it in the file below.',
  'rules.sets': 'writes',
  'inspector.openRule': 'Open the rule in {0}',

  'tab.template': 'Template explorer',

  'template.title': 'Template explorer',
  'template.lede':
    'The operational template is the third thing the mappings depend on and the one the application never shows. It is what EHRbase validates a composition against and what openFHIR resolves paths against — the [[template_id|template]] narrows the [[archetype_id|archetypes]] to one specific use, and this is the result: every node a composition may have, its type, how often it may occur, and the path AQL would address it by.',
  'template.usedHeadline': 'nodes of the template are used',
  'template.usedNote':
    'The other {0} are allowed and stay empty. That gap is normal — a template describes what may be recorded, not what is.',
  'template.noData': 'Nothing is stored yet, so nothing is marked. Import a reading and come back.',
  'template.onlyFilled': 'Only what is used',
  'template.legend.filled': 'data reaches it',
  'template.legend.empty': 'allowed, never filled',
  'template.legend.occurrences': 'occurrences; blue means the model insists on it',
  'template.expand': 'Expand',
  'template.collapse': 'Collapse',
  'template.closing':
    'Worth finding: under the pulse observation there is a second event slot, an INTERVAL_EVENT called "Maximum" carrying a math_function. The model has room for the minimum, maximum and mean of a day — the mapping simply never fills it, because FHIR Connect as implemented cannot write a math_function. The template says what is possible; the mappings decide what happens.',

  'aql.title': 'AQL playground',
  'aql.lede':
    'Run a query against the record and see what openEHR answers. [[AQL]] selects by [[archetype_node_id|archetype path]] rather than by table column, so a query written against the pulse archetype runs on any openEHR system that knows that archetype. It is read-only: there is no AQL statement that changes anything.',
  'aql.queryLabel': 'AQL query',
  'aql.run': 'Run',
  'aql.running': 'Running…',
  'aql.shortcut': 'Ctrl/Cmd + Enter',
  'aql.row': '1 row',
  'aql.rows': '{0} rows',
  'aql.noRows': 'No rows. The query was valid — nothing in the record matched it.',
  'aql.truncated': 'Showing the first {0} of {1} rows.',
  'aql.nullNote':
    'Every value in at least one row is null. In AQL a path that does not exist is not an error: the query matches the observations and finds nothing at that path. This is the failure mode to know about.',

  'tour.stores.title': 'Where the rest of it lives',
  'tour.stores.body':
    'The last step showed openEHR keeping only what its archetype models. The patient is the same story: EHR_STATUS anchors a record on an identifier and has room for neither a name nor an address. So there is a second store \u2014 an ordinary FHIR server \u2014 and a patient record is assembled from both. One identifier joins them, and stopping either one leaves the other working. That is not a workaround; it is what having two models actually costs, and what it buys.',
  'diagram.open.fhir': 'Open the pipeline inspector, where the FHIR Observation is shown',
  'diagram.open.openfhir': 'Open the mappings \u2014 the FHIR Connect rules openFHIR executes',
  'diagram.open.openehr':
    'Open the template explorer \u2014 the openEHR model the record is validated against',
  'graph.title': 'How the rules nest',
  'graph.lede':
    'The same rules as the list above, with the part a list cannot show: rules contain rules. A line means the rule below it only applies inside the one above \u2014 which is what the indentation in the files means, and what decides whether a rule ever runs. Nothing here is hand-drawn: it is built from the files below, so editing a mapping redraws it. Pick a box to open its rule.',
  'graph.open': 'Open the rule \u201c{0}\u201d',
  'graph.drawing': 'Drawing\u2026',
  'tab.record': 'Two stores',
  'record.title': 'One record, two stores',
  'record.lede':
    'This is what a single patient record looks like when the clinical half lives in openEHR and the administrative half does not. The backend asks both and hands back one FHIR Bundle; neither store holds the other\u2019s part, and nothing in the Bundle says which came from where.',
  'record.fhirStore': 'FHIR store',
  'record.fhirStore.what':
    'Name, gender, birth date, address. openEHR\u2019s EHR_STATUS anchors a record on an identifier and has room for nothing else \u2014 which is the entire reason this server exists.',
  'record.openehr': 'openEHR, via openFHIR',
  'record.openehr.what':
    'The readings, stored as compositions and mapped back to FHIR on the way out. The id of each one is the openEHR versioned object uid, so no table translates between the two.',
  'record.version': 'openEHR\u2019s version of this composition, surfaced as FHIR meta.versionId',
  'record.more': 'and {0} more',
  'record.note':
    'Assembled from {0} entries. Stop the FHIR store and the chart keeps drawing \u2014 only the names go missing, because the two halves fail independently.',
  'record.loading': 'Assembling\u2026',
  'lang.label': 'Language',
  'patient.label': 'Patient',
  'patient.ehr': 'The openEHR record this patient\u2019s readings are stored in',

  'stale.title': 'The backend is running an older build than this page.',
  'stale.body':
    'Restart it with `cd backend && ./gradlew bootRun` — it runs on the host, so `docker compose` does not restart it. Until then corrections will not show up in the chart, and the pipeline inspector and traffic console stay empty. Nothing is lost in the meantime; the record has every reading you entered.',

  'start.title': 'Start here',
  'start.lede':
    'This app stores one number per day — and it takes three standards to do it. [[Observation|HL7 FHIR]] carries the reading in and out, [[openFHIR]] translates, and [[EHRbase|openEHR]] keeps the record. The chart below is only the proof that it worked; the two other tabs are where you can watch it happen.',
  'start.step1.body':
    'They arrive as a FHIR [[Bundle]] and are stored as openEHR compositions. Nothing takes a shortcut into the database.',
  'start.step2.body':
    'The pipeline inspector runs a single reading through every stage and keeps each one, so you can see a FHIR Observation turn into an openEHR [[COMPOSITION]] and back — and which rule did it.',
  'start.hint':
    'Underlined words like [[AQL]] and [[at0004]] explain themselves — hover or tap them. They are everywhere, including inside the JSON.',
  'start.step1.title': 'Get a month of readings in',
  'start.step1.done': 'Done — the chart below is reading them back out with AQL.',
  'start.step2.title': 'Watch one reading cross the border',
  'start.step2.action': 'Open the pipeline inspector',
  'start.step3.title': 'Watch the servers actually talk',
  'start.step3.body':
    'Every call to openFHIR and EHRbase, with its request and response. This is the openEHR REST API as it really is, not as a spec describes it.',
  'start.step3.action': 'Open the standards traffic',
  'start.hide': 'Hide',
  'start.show': 'Show the introduction again',
  'start.showShort': 'Introduction',
  'start.hideLabel': 'Hide this introduction',

  'stat.resting': 'Resting heart rate',
  'stat.average': '{0}-day average',
  'stat.coverage': 'Coverage',
  'stat.noReadings': 'No readings yet',
  'stat.needsTwo': 'Needs two days of readings',
  'stat.days': '/ {0} days',
  'stat.coverageNote': 'days with a recorded resting heart rate',
  'stat.delta': '{0} bpm vs. the days before',

  'chart.title': 'Resting heart rate, last {0} days',
  'chart.subtitle':
    '{0} of {1} days recorded. Stored in openEHR as openEHR-EHR-OBSERVATION.pulse.v2, exchanged as LOINC 40443-4.',
  'chart.empty':
    'Nothing stored yet. Load the sample data below, type a reading, or import a FHIR Bundle.',
  'chart.loading': 'Loading…',
  'chart.loadError': '{0} — is the backend running on :18080 and `docker compose up` healthy?',

  'manual.title': 'Record a reading',
  'manual.lede':
    'One resting heart rate for one day. It does not take a shortcut into the database: the browser builds a FHIR Observation and posts it to /fhir/Observation, so a typed-in reading travels the same road as one from an external system — through openFHIR’s mappings and into openEHR.',
  'manual.date': 'Date',
  'manual.rate': 'Resting heart rate',
  'manual.store': 'Store it',
  'manual.correct': 'Correct it',
  'manual.storing': 'Storing…',
  'manual.trace': 'Trace it instead',
  'manual.invalid': 'A date and a rate between 20 and 250 bpm.',
  'manual.nothingYet': 'Nothing recorded for that day yet.',
  'manual.existing':
    'That day already holds {0} bpm. Storing corrects it: openEHR never overwrites, so this adds a new version of that composition and the old value stays in the record.',
  'manual.stored': 'Stored {0} bpm for {1}.',
  'manual.corrected':
    '{0}: {1} bpm corrected to {2} bpm. openEHR kept the old value as the previous version of that composition — watch the PUT in the standards traffic console.',
  'manual.showResource': 'Show the resource this sends',
  'manual.hideResource': 'Hide the resource this sends',
  'manual.resourceNote':
    'Four of those fields are not decoration. status, category and the LOINC code are what pulse.model.yaml matches on to decide this is a resting heart rate at all, and the UCUM unit is what the pulse archetype constrains. Change any of them and the mapping stops recognising it — the pipeline inspector’s third sample does exactly that.',
  'manual.builtInBrowser': 'built in the browser',

  'history.show': 'Show what this day used to say',
  'history.hide': 'Hide what this day used to say',
  'history.lede':
    'openEHR has no overwrite. Correcting a reading adds a version to the same [[COMPOSITION]] and keeps the one before it, so the record can still answer "what did it say on the day someone acted on it?" — which is the whole reason a clinical data repository is not an ordinary database.',
  'history.none': 'Nothing recorded for that day.',
  'history.inForce': 'in force',
  'history.corrections':
    '{0} corrections, and every earlier value is still retrievable — the traffic console shows the PUT that made each one.',
  'history.correction':
    '1 correction, and the earlier value is still retrievable — the traffic console shows the PUT that made it.',
  'history.several':
    'This day holds {0} separate compositions rather than one with several versions — readings stored before this service corrected by versioning piled up instead of replacing each other. Each has its own history; the chart shows whichever was committed last.',
  'history.compositionOf': 'Composition {0} of {1}',

  'exchange.title': 'FHIR exchange',
  'exchange.lede.1': 'Load sample data',
  'exchange.lede.2':
    'asks the backend for a month of made-up readings as a FHIR Bundle and imports it — generated for today on every call, so it cannot go stale the way a checked-in file would. Or import a Bundle of your own. Export gives you everything stored, again as a Bundle: both directions run through the same openFHIR mappings, so what comes out is produced by the same rules that read what went in.',
  'exchange.loadSample': 'Load sample data',
  'exchange.importing': 'Importing…',
  'exchange.import': 'Import a file',
  'exchange.export': 'Export as FHIR Bundle',
  'exchange.raw': 'View the raw JSON',

  'table.show': 'Show the numbers',
  'table.hide': 'Hide the numbers',
  'table.caption': 'Resting heart rate, last {0} days',
  'table.date': 'Date',
  'table.resting': 'Resting (bpm)',
  'table.empty': 'Nothing imported yet.',

  'inspector.title': 'Pipeline inspector',
  'inspector.lede':
    'The import does this every time and throws it all away. Here nothing is thrown away: pick an input, then walk the stages to see the same heart rate as FHIR, as an openEHR COMPOSITION, and back again. Nothing is written to the record unless you ask for it.',
  'inspector.ownFile': 'Your own file…',
  'inspector.ownFileSummary': 'Any FHIR Bundle or Observation.',
  'inspector.store.title': 'Also store it in the record',
  'inspector.store.why': 'What does that change?',
  'inspector.store.body':
    'A trace is a dry run by default: it maps the reading through openFHIR and shows you the result, but writes nothing — so you can look as often as you like without filling the record with samples. Tick this and it really is stored, which adds two more stages: the write to EHRbase and the AQL read-back.',
  'inspector.running': 'Running…',
  'inspector.recognised': 'Recognised as',
  'inspector.wrote': 'This run wrote to the record.',
  'inspector.links.title': 'What became what',
  'inspector.links.lede':
    'Pick one to light it up on both sides, and to see the FHIR Connect rule that put it there.',
  'inspector.kind.mapped': 'value copied',
  'inspector.kind.selector': 'chooses a structure',
  'inspector.kind.generated': 'not from FHIR',
  'inspector.diff.title': 'What the round trip did not bring back',
  'inspector.diff.lede':
    'Every line here is a field FHIR carried that the openEHR model has no place for. Exporting gives you correct FHIR, not the same FHIR.',

  'mapping.title': 'The rule that did it',
  'mapping.lede':
    'Declarative FHIR Connect YAML from openfhir-bootstrap/ — no Java involved. Editing a file here writes the real one and makes openFHIR re-read it, so you can change how the two models correspond, or break it on purpose and watch exactly where the pipeline gives up. Everything is one Reset away from how it shipped.',
  'mapping.edit': 'Edit this mapping',
  'mapping.save': 'Save and reload openFHIR',
  'mapping.saving': 'Saving…',
  'mapping.cancel': 'Cancel',
  'mapping.reset': 'Reset to the shipped version',
  'mapping.edited': 'This file has been edited.',
  'mapping.savedOk': 'Saved, and openFHIR reloaded its mappings. Run a trace to see what changed.',
  'mapping.savedBad':
    'Saved, but openFHIR would not load it: {0}. It is still running with the mappings it read last.',
  'mapping.wasReset': 'Back to the version this application shipped with.',
  'mapping.noDetail': 'no detail given',

  'traffic.title': 'Standards traffic',
  'traffic.lede':
    'Every call this service made to the two standards servers, newest first. This is the openEHR REST API and [[openFHIR]]’s mapping API as they really are — a [[COMPOSITION]] being POSTed to /ehr/{id}/composition, an [[AQL]] query being answered, a [[template_id|template]] upload that answers 409 because the template is already there. Headers are not recorded: [[EHRbase]] runs with basic auth, and its credentials have no business in a browser panel.',
  'traffic.both': 'Both',
  'traffic.live': 'Follow live',
  'traffic.clear': 'Clear',
  'traffic.empty':
    'Nothing yet. Import a file, open the pipeline inspector, or just reload the chart — every one of those talks to a standards server, and it will show up here.',
  'traffic.request': 'Request body',
  'traffic.response': 'Response body',
  'traffic.noBody':
    'No response body — the openEHR API answers 204 to a write and puts the version uid in the ETag header.',
  'traffic.failed': 'failed',

  'diagram.alt':
    'HL7 FHIR carries the reading in, openFHIR maps it, openEHR stores it — and back out again.',
  'diagram.fhir.role': 'exchange',
  'diagram.openfhir.role': 'translation',
  'diagram.openehr.role': 'persistence · EHRbase',
  'diagram.import': 'import',
  'diagram.export': 'export',
  'diagram.store': 'store',
  'diagram.fhir.caption': 'Says what a value means,|in codes. Built to cross|a system boundary.',
  'diagram.openfhir.caption':
    'Knows both models and|nothing else. Declarative|YAML, stores no data.',
  'diagram.openehr.caption':
    'Says what a value means|by where it sits. Built to|be queried in 30 years.',

  'panel.nothing': 'Nothing to show.',
  'panel.copy': 'Copy',
  'panel.copied': 'Copied',
  'panel.copyFailed': 'Could not copy',
};

const DE: Dictionary = {
  'app.title': 'Ruhepuls-Monitor',
  'app.lede':
    'Ruhepulse werden als HL7-FHIR-Observations ausgetauscht, von openFHIR gemappt und in openEHR gespeichert. Der Chart ist nur der Vorwand; worum es geht, passiert zwischen diesen dreien.',
  'tab.overview': 'Übersicht',
  'tour.label': 'Geführte Tour',
  'tour.step': 'Schritt {0} von {1}',
  'tour.next': 'Weiter',
  'tour.back': 'Zurück',
  'tour.stop': 'Tour abbrechen',
  'tour.close': 'Schließen',
  'tour.finish': 'Fertig',
  'tour.start': 'Tour starten',
  'tour.restart': 'Tour nochmal ansehen',
  'tour.offer':
    'Zum ersten Mal mit diesen drei Standards zu tun? Die Tour beantwortet die Frage, die der Rest dieser App schon voraussetzt: warum ein einzelner Ruhepuls gleich drei davon braucht.',

  'tour.what.title': 'Worum es sich handelt',
  'tour.what.body':
    'Ein Ruhepuls-Monitor: ein Ruhepuls pro Tag, dreißig Tage davon, als Linie gezeichnet. Das ist die ganze Funktion — und sie ist der Vorwand. Alles andere hier gibt es, um zu zeigen, was es wirklich braucht, um diese Zahlen ordentlich zu speichern. Und dafür braucht es drei Standards aus dem Gesundheitswesen. Darum geht es im Rest dieser Tour.',
  'tour.number.title': 'Eine Zahl: 58',
  'tour.number.body':
    'Da steht sie, im Feld darunter. Ein Ruhepuls, an einem Tag gemessen. Ihn zu speichern ist trivial — das kann jede Datenbank. Ihn so zu speichern, dass ein anderes Krankenhaus, oder dasselbe in zwanzig Jahren, noch weiß, was er bedeutet, ist das eigentliche Problem. Dafür sind diese drei Standards da.',
  'tour.column.title': 'Warum eine Datenbankspalte nicht reicht',
  'tour.column.body':
    'Eine Spalte `bpm INT` enthält 58. Sie enthält nicht: in welcher Einheit, in Ruhe oder unter Belastung, von wem gemessen, mit welchem Gerät, in welchem Versorgungskontext. Ein Kollege kann die Zahl in zehn Jahren lesen und trotzdem nicht wissen, was sie bedeutet — und in der Medizin ist das kein kleines Problem.',
  'tour.fhir.title': 'Was HL7 FHIR beiträgt',
  'tour.fhir.body':
    'FHIR lässt die Zahl sagen, was sie ist. LOINC 40443-4 heißt „Herzfrequenz in Ruhe", UCUM /min ist die Einheit, die Kategorie macht sie zum Vitalparameter. Die Bedeutung steckt in Codes — und genau das erlaubt der Ressource, eine Systemgrenze zu überqueren und drüben verstanden zu werden. Sieh dir an, was das Backend gebaut hat.',
  'tour.openehr.title': 'Was openEHR anders macht',
  'tour.openehr.body':
    'Dieselbe Messung, andere Logik — und der LOINC-Code ist verschwunden. In openEHR steckt die Bedeutung nicht in einem Code, sondern an einem Ort: der Wert liegt im Puls-Archetyp, einer international abgestimmten Definition dessen, was eine Puls-Observation ist. Dafür verlangt openEHR Dinge, die FHIR nie mitschickte: wer hat aufgezeichnet, in welcher Sprache, in welchem Kontext.',
  'tour.lost.title': 'Zwei Modelle, nicht ein Modell in zwei Formaten',
  'tour.lost.body':
    'Zurückgemappt nach FHIR kommt nicht alles wieder: die Kodiersysteme, die Anzeigenamen, der Patientenbezug. openEHR hat behalten, was der Archetyp modelliert, und alles darüber hinaus hatte keinen Platz. Das ist kein Fehler, den man behebt — das ist, was es heißt, dass zwei Modelle wirklich verschieden sind.',
  'tour.translate.title': 'Also muss jemand übersetzen',
  'tour.translate.body':
    'Das ist openFHIR, und so weiß es, was zu tun ist: deklarative Regeln, die sagen, welcher FHIR-Pfad welchem openEHR-Pfad entspricht und unter welchen Bedingungen. Kein Java. Die Entsprechung zu ändern heißt, dieses YAML zu bearbeiten — und das kannst du hier, inklusive absichtlich kaputtmachen, um zu sehen, was die Regeln eigentlich taten.',
  'tour.template.title': 'Was das Modell erlaubt und was benutzt wird',
  'tour.template.body':
    'Das dritte, worauf die Mappings aufbauen: das operationale Template. EHRbase validiert jede Composition dagegen, openFHIR löst Pfade daran auf — und nur zehn seiner dreiundzwanzig Knoten werden je befüllt. Klapp die Puls-Observation auf und such den zweiten Ereignis-Slot, ein INTERVAL_EVENT namens „Maximum" mit einer math_function: Das Modell hat Platz für Minimum, Maximum und Mittelwert eines Tages, benannt, typisiert, fertig. Das Mapping schreibt ihn nur nie. Das Template sagt, was möglich ist; die Mappings entscheiden, was passiert.',
  'tour.versions.title': 'Die Akte vergisst nicht',
  'tour.versions.body':
    'Korrigiert man eine Messung, überschreibt openEHR sie nicht — es gibt kein Überschreiben. Es legt eine Version an und behält die davor. Deshalb kann eine klinische Akte weiterhin beantworten: „Was stand dort an dem Tag, an dem jemand danach gehandelt hat?" Eine gewöhnliche Datenbank kann das nicht. Probier es: denselben Tag zweimal erfassen und die Historie öffnen.',
  'tour.traffic.title': 'Dabei zusehen',
  'tour.traffic.body':
    'Jeder Aufruf, den diese Anwendung an die beiden Standards-Server gemacht hat, neueste zuerst. Klapp einen auf, und du siehst die openEHR-REST-API, wie sie wirklich ist: eine COMPOSITION per POST an /ehr/{id}/composition, beantwortet mit 204 und einer Version-UID im ETag. Eine Korrektur, die du gerade gemacht hast, ist das PUT ein paar Zeilen weiter oben. Nichts davon ist ein Schaubild, wie es funktionieren könnte — es ist, was passiert ist.',
  'tour.query.title': 'Und sie bleibt abfragbar',
  'tour.query.body':
    'AQL selektiert über Archetyp-Pfade, nicht über Tabellenspalten. Dieselbe Abfrage läuft auf jedem openEHR-System, das den Archetyp kennt, und übersteht einen Umbau der Datenbank darunter. Führ eine aus — und probier das Beispiel mit dem Tippfehler im Pfad, das ist der Fehlermodus, den man lieber absichtlich kennenlernt.',
  'tour.done.title': 'Das ist die ganze Idee',
  'tour.done.body':
    'FHIR transportiert, openEHR erinnert, openFHIR übersetzt. Alles andere in dieser App ist Detail dazu, wie. Unterstrichene Wörter erklären sich beim Draufzeigen, und die Tour findest du jederzeit wieder im „Hier anfangen"-Panel.',

  'tab.pipeline': 'Pipeline-Inspector',
  'tab.traffic': 'Standards-Verkehr',
  'tab.aql': 'AQL-Playground',
  'tab.mappings': 'Mappings',

  'mappings.title': 'Die FHIR-Connect-Mappings',
  'mappings.lede':
    'Alles, was darüber entscheidet, wie FHIR und openEHR einander entsprechen, steht in diesen drei Dateien. [[FHIR Connect]] ist eine deklarative Grammatik. Die Entsprechung zu ändern heißt also YAML bearbeiten statt Code schreiben — und openFHIR liest es auf Zuruf neu ein, ohne Rebuild und ohne Neustart.',
  'mappings.focus': 'Geöffnet bei der Regel hinter „{0}".',
  'rules.title': 'Was wird wozu',
  'rules.lede':
    'Jede Regel, die diese Dateien deklarieren — aus ihnen gelesen, nicht hier hingeschrieben: bearbeite ein Mapping, und die Liste folgt. Links ein [[Observation|FHIR]]-Pfad, rechts der [[archetype_node_id|openEHR-Pfad]], dem er entspricht. Wähle eine aus, um sie in der Datei darunter zu finden.',
  'rules.sets': 'schreibt',
  'inspector.openRule': 'Regel in {0} öffnen',

  'tab.template': 'Template-Explorer',

  'template.title': 'Template-Explorer',
  'template.lede':
    'Das operationale Template ist das dritte, worauf die Mappings aufbauen — und das einzige, das die Anwendung nie zeigt. Gegen es validiert EHRbase eine Composition, an ihm löst openFHIR Pfade auf: das [[template_id|Template]] verengt die [[archetype_id|Archetypen]] auf einen konkreten Einsatz, und das hier ist das Ergebnis: jeder Knoten, den eine Composition haben darf, sein Typ, wie oft er vorkommen darf, und der Pfad, über den AQL ihn ansprechen würde.',
  'template.usedHeadline': 'Knoten des Templates werden benutzt',
  'template.usedNote':
    'Die anderen {0} sind erlaubt und bleiben leer. Diese Lücke ist normal — ein Template beschreibt, was aufgezeichnet werden darf, nicht was aufgezeichnet wird.',
  'template.noData':
    'Noch nichts gespeichert, also ist nichts markiert. Importiere eine Messung und komm wieder.',
  'template.onlyFilled': 'Nur das Benutzte',
  'template.legend.filled': 'Daten erreichen ihn',
  'template.legend.empty': 'erlaubt, nie befüllt',
  'template.legend.occurrences': 'Vorkommen; blau heißt, das Modell verlangt ihn',
  'template.expand': 'Aufklappen',
  'template.collapse': 'Zuklappen',
  'template.closing':
    'Lohnt sich zu suchen: unter der Puls-Observation liegt ein zweiter Ereignis-Slot, ein INTERVAL_EVENT namens „Maximum" mit einer math_function. Das Modell hat Platz für Minimum, Maximum und Mittelwert eines Tages — das Mapping befüllt ihn nur nie, weil FHIR Connect in dieser Fassung keine math_function schreiben kann. Das Template sagt, was möglich ist; die Mappings entscheiden, was passiert.',

  'aql.title': 'AQL-Playground',
  'aql.lede':
    'Führe eine Abfrage gegen die Akte aus und sieh, was openEHR antwortet. [[AQL]] selektiert über [[archetype_node_id|Archetyp-Pfade]] statt über Tabellenspalten. Eine gegen den Puls-Archetyp geschriebene Abfrage läuft deshalb auf jedem openEHR-System, das diesen Archetyp kennt. Sie liest nur: es gibt kein AQL-Statement, das etwas verändert.',
  'aql.queryLabel': 'AQL-Abfrage',
  'aql.run': 'Ausführen',
  'aql.running': 'Läuft…',
  'aql.shortcut': 'Strg/Cmd + Enter',
  'aql.row': '1 Zeile',
  'aql.rows': '{0} Zeilen',
  'aql.noRows': 'Keine Zeilen. Die Abfrage war gültig — in der Akte passte nichts dazu.',
  'aql.truncated': 'Die ersten {0} von {1} Zeilen.',
  'aql.nullNote':
    'In mindestens einer Zeile ist jeder Wert null. Ein Pfad, den es nicht gibt, ist in AQL kein Fehler: die Abfrage trifft die Observations und findet an dieser Stelle nichts. Das ist der Fehlermodus, den man kennen sollte.',

  'tour.stores.title': 'Wo der Rest liegt',
  'tour.stores.body':
    'Der letzte Schritt hat gezeigt, dass openEHR nur beh\u00e4lt, was sein Archetyp modelliert. Beim Patienten ist es dieselbe Geschichte: EHR_STATUS verankert einen Record an einer Kennung und hat weder f\u00fcr einen Namen noch f\u00fcr eine Adresse Platz. Also gibt es einen zweiten Speicher \u2014 einen gew\u00f6hnlichen FHIR-Server \u2014 und ein Patientenrecord wird aus beiden zusammengesetzt. Eine einzige Kennung verbindet sie, und f\u00e4llt einer aus, arbeitet der andere weiter. Das ist kein Notbehelf, sondern der Preis und der Gewinn zweier Modelle.',
  'diagram.open.fhir': 'Pipeline-Inspector \u00f6ffnen, wo die FHIR-Observation zu sehen ist',
  'diagram.open.openfhir':
    'Mappings \u00f6ffnen \u2014 die FHIR-Connect-Regeln, die openFHIR ausf\u00fchrt',
  'diagram.open.openehr':
    'Template-Explorer \u00f6ffnen \u2014 das openEHR-Modell, gegen das validiert wird',
  'graph.title': 'Wie die Regeln ineinander liegen',
  'graph.lede':
    'Dieselben Regeln wie in der Liste oben, aber mit dem, was eine Liste nicht zeigen kann: Regeln enthalten Regeln. Eine Linie bedeutet, dass die untere Regel nur innerhalb der oberen gilt \u2014 genau das sagt die Einr\u00fcckung in den Dateien, und genau das entscheidet, ob eine Regel \u00fcberhaupt greift. Nichts davon ist von Hand gezeichnet: es entsteht aus den Dateien unten, eine Mapping-\u00c4nderung zeichnet es also neu. Eine Box anklicken \u00f6ffnet ihre Regel.',
  'graph.open': 'Regel \u201e{0}\u201c \u00f6ffnen',
  'graph.drawing': 'Wird gezeichnet\u2026',
  'tab.record': 'Zwei Speicher',
  'record.title': 'Ein Record, zwei Speicher',
  'record.lede':
    'So sieht ein einzelner Patientenrecord aus, wenn die klinische H\u00e4lfte in openEHR liegt und die administrative nicht. Das Backend fragt beide und liefert ein einziges FHIR-Bundle zur\u00fcck; keiner der beiden Speicher h\u00e4lt den Teil des anderen, und im Bundle steht nirgends, was woher kam.',
  'record.fhirStore': 'FHIR-Store',
  'record.fhirStore.what':
    'Name, Geschlecht, Geburtsdatum, Adresse. openEHRs EHR_STATUS verankert einen Record an einer Kennung und hat f\u00fcr nichts anderes Platz \u2014 genau daf\u00fcr gibt es diesen Server.',
  'record.openehr': 'openEHR, \u00fcber openFHIR',
  'record.openehr.what':
    'Die Messwerte, als Compositions gespeichert und beim Herausgeben nach FHIR zur\u00fcckgemappt. Die Id jeder einzelnen ist das openEHR-Objekt-uid \u2014 deshalb braucht es keine \u00dcbersetzungstabelle.',
  'record.version': 'openEHRs Version dieser Composition, sichtbar gemacht als FHIR meta.versionId',
  'record.more': 'und {0} weitere',
  'record.note':
    'Aus {0} Eintr\u00e4gen zusammengesetzt. Stoppt man den FHIR-Store, zeichnet das Chart weiter \u2014 nur die Namen fehlen, weil die beiden H\u00e4lften unabh\u00e4ngig ausfallen.',
  'record.loading': 'Wird zusammengesetzt\u2026',
  'lang.label': 'Sprache',
  'patient.label': 'Patient',
  'patient.ehr': 'Der openEHR-Record, in dem die Messwerte dieses Patienten liegen',

  'stale.title': 'Das Backend läuft mit einem älteren Stand als diese Seite.',
  'stale.body':
    'Starte es mit `cd backend && ./gradlew bootRun` neu — es läuft auf dem Host, `docker compose` startet es also nicht mit. Bis dahin erscheinen Korrekturen nicht im Chart, und Pipeline-Inspector und Verkehrskonsole bleiben leer. Verloren geht dabei nichts: die Akte hat jede Messung, die du eingegeben hast.',

  'start.title': 'Hier anfangen',
  'start.lede':
    'Diese Anwendung speichert eine Zahl pro Tag — und dafür braucht es drei Standards. [[Observation|HL7 FHIR]] bringt die Messung herein und heraus, [[openFHIR]] übersetzt, und [[EHRbase|openEHR]] führt die Akte. Der Chart unten ist nur der Beleg, dass es funktioniert hat; in den beiden anderen Tabs kannst du zusehen.',
  'start.step1.body':
    'Sie kommen als FHIR-[[Bundle]] und werden als openEHR-Compositions gespeichert. Nichts nimmt eine Abkürzung in die Datenbank.',
  'start.step2.body':
    'Der Pipeline-Inspector schickt eine einzelne Messung durch jede Stufe und behält jede einzelne, damit du siehst, wie aus einer FHIR Observation eine openEHR-[[COMPOSITION]] wird und wieder zurück — und welche Regel das getan hat.',
  'start.hint':
    'Unterstrichene Wörter wie [[AQL]] und [[at0004]] erklären sich selbst — fahre darüber oder tippe sie an. Sie stehen überall, auch mitten im JSON.',
  'start.step1.title': 'Einen Monat an Messungen hereinholen',
  'start.step1.done': 'Erledigt — der Chart unten liest sie gerade mit AQL wieder heraus.',
  'start.step2.title': 'Einer Messung über die Grenze folgen',
  'start.step2.action': 'Pipeline-Inspector öffnen',
  'start.step3.title': 'Den Servern beim Reden zuhören',
  'start.step3.body':
    'Jeder Aufruf an openFHIR und EHRbase, mit Anfrage und Antwort. Das ist die openEHR-REST-API, wie sie wirklich ist — nicht, wie eine Spezifikation sie beschreibt.',
  'start.step3.action': 'Standards-Verkehr öffnen',
  'start.hide': 'Ausblenden',
  'start.show': 'Einführung wieder einblenden',
  'start.showShort': 'Einführung',
  'start.hideLabel': 'Diese Einführung ausblenden',

  'stat.resting': 'Ruhepuls',
  'stat.average': 'Schnitt über {0} Tage',
  'stat.coverage': 'Abdeckung',
  'stat.noReadings': 'Noch keine Messungen',
  'stat.needsTwo': 'Braucht zwei Tage mit Messungen',
  'stat.days': '/ {0} Tage',
  'stat.coverageNote': 'Tage mit aufgezeichnetem Ruhepuls',
  'stat.delta': '{0} bpm gegenüber den Tagen davor',

  'chart.title': 'Ruhepuls, letzte {0} Tage',
  'chart.subtitle':
    '{0} von {1} Tagen aufgezeichnet. In openEHR gespeichert als openEHR-EHR-OBSERVATION.pulse.v2, ausgetauscht als LOINC 40443-4.',
  'chart.empty':
    'Noch nichts gespeichert. Lade unten die Beispieldaten, gib eine Messung ein oder importiere ein FHIR Bundle.',
  'chart.loading': 'Wird geladen…',
  'chart.loadError': '{0} — läuft das Backend auf :18080 und ist `docker compose up` gesund?',

  'manual.title': 'Eine Messung erfassen',
  'manual.lede':
    'Ein Ruhepuls für einen Tag. Ohne Abkürzung in die Datenbank: der Browser baut eine FHIR Observation und schickt sie an /fhir/Observation. Eine getippte Messung nimmt damit denselben Weg wie eine aus einem fremden System — durch die Mappings von openFHIR und hinein nach openEHR.',
  'manual.date': 'Datum',
  'manual.rate': 'Ruhepuls',
  'manual.store': 'Speichern',
  'manual.correct': 'Korrigieren',
  'manual.storing': 'Wird gespeichert…',
  'manual.trace': 'Stattdessen verfolgen',
  'manual.invalid': 'Ein Datum und ein Wert zwischen 20 und 250 bpm.',
  'manual.nothingYet': 'Für diesen Tag ist noch nichts aufgezeichnet.',
  'manual.existing':
    'Dieser Tag hält bereits {0} bpm. Speichern korrigiert das: openEHR überschreibt nie, es kommt also eine neue Version dieser Composition hinzu, und der alte Wert bleibt in der Akte.',
  'manual.stored': '{0} bpm für {1} gespeichert.',
  'manual.corrected':
    '{0}: {1} bpm auf {2} bpm korrigiert. openEHR hat den alten Wert als vorige Version dieser Composition behalten — sieh dir das PUT in der Verkehrskonsole an.',
  'manual.showResource': 'Die Ressource zeigen, die gesendet wird',
  'manual.hideResource': 'Die Ressource ausblenden',
  'manual.resourceNote':
    'Vier dieser Felder sind keine Zierde. status, category und der LOINC-Code sind das, worauf pulse.model.yaml prüft, um überhaupt einen Ruhepuls zu erkennen, und die UCUM-Einheit ist das, was der Puls-Archetyp einschränkt. Ändere eines davon, und das Mapping erkennt die Ressource nicht mehr — das dritte Beispiel im Pipeline-Inspector macht genau das.',
  'manual.builtInBrowser': 'im Browser gebaut',

  'history.show': 'Zeigen, was dieser Tag früher sagte',
  'history.hide': 'Frühere Stände ausblenden',
  'history.lede':
    'openEHR kennt kein Überschreiben. Eine Korrektur fügt derselben [[COMPOSITION]] eine Version hinzu und behält die davor. So kann die Akte weiterhin beantworten: „Was stand dort an dem Tag, an dem jemand danach gehandelt hat?" — und genau deshalb ist ein klinisches Repository keine gewöhnliche Datenbank.',
  'history.none': 'Für diesen Tag ist nichts aufgezeichnet.',
  'history.inForce': 'aktuell',
  'history.corrections':
    '{0} Korrekturen, und jeder frühere Wert ist weiterhin abrufbar — die Verkehrskonsole zeigt das PUT, das jede einzelne bewirkt hat.',
  'history.correction':
    '1 Korrektur, und der frühere Wert ist weiterhin abrufbar — die Verkehrskonsole zeigt das PUT, das sie bewirkt hat.',
  'history.several':
    'Dieser Tag hält {0} getrennte Compositions statt einer mit mehreren Versionen — Messungen, die vor der Versionierung gespeichert wurden, haben sich gestapelt, statt einander zu ersetzen. Jede hat ihre eigene Historie; der Chart zeigt die zuletzt committete.',
  'history.compositionOf': 'Composition {0} von {1}',

  'exchange.title': 'FHIR-Austausch',
  'exchange.lede.1': 'Beispieldaten laden',
  'exchange.lede.2':
    'holt vom Backend einen Monat erfundener Messungen als FHIR Bundle und importiert ihn — bei jedem Aufruf für heute erzeugt, damit er nicht veralten kann wie eine eingecheckte Datei. Oder importiere ein eigenes Bundle. Der Export gibt dir alles Gespeicherte, wieder als Bundle: beide Richtungen laufen durch dieselben openFHIR-Mappings, was herauskommt, wird also von denselben Regeln erzeugt, die auch das Hereinkommende gelesen haben.',
  'exchange.loadSample': 'Beispieldaten laden',
  'exchange.importing': 'Wird importiert…',
  'exchange.import': 'Datei importieren',
  'exchange.export': 'Als FHIR Bundle exportieren',
  'exchange.raw': 'Rohes JSON ansehen',

  'table.show': 'Die Zahlen zeigen',
  'table.hide': 'Die Zahlen ausblenden',
  'table.caption': 'Ruhepuls, letzte {0} Tage',
  'table.date': 'Datum',
  'table.resting': 'Ruhepuls (bpm)',
  'table.empty': 'Noch nichts importiert.',

  'inspector.title': 'Pipeline-Inspector',
  'inspector.lede':
    'Der Import macht das jedes Mal und wirft alles weg. Hier wird nichts weggeworfen: wähle eine Eingabe und gehe die Stufen durch, um denselben Puls als FHIR zu sehen, als openEHR-COMPOSITION und wieder zurück. In die Akte wird nichts geschrieben, solange du nicht darum bittest.',
  'inspector.ownFile': 'Eigene Datei…',
  'inspector.ownFileSummary': 'Ein beliebiges FHIR Bundle oder eine Observation.',
  'inspector.store.title': 'Zusätzlich in der Akte speichern',
  'inspector.store.why': 'Was ändert das?',
  'inspector.store.body':
    'Ein Trace ist standardmäßig ein Trockenlauf: er mappt die Messung durch openFHIR und zeigt dir das Ergebnis, schreibt aber nichts — du kannst also so oft hinsehen, wie du willst, ohne die Akte mit Beispielen zu füllen. Setz den Haken, und es wird wirklich gespeichert, was zwei weitere Stufen ergibt: das Schreiben nach EHRbase und das Zurücklesen mit AQL.',
  'inspector.running': 'Läuft…',
  'inspector.recognised': 'Erkannt als',
  'inspector.wrote': 'Dieser Lauf hat in die Akte geschrieben.',
  'inspector.links.title': 'Was wurde wozu',
  'inspector.links.lede':
    'Wähle eines aus, um es auf beiden Seiten aufleuchten zu lassen und die FHIR-Connect-Regel zu sehen, die es dorthin gebracht hat.',
  'inspector.kind.mapped': 'Wert kopiert',
  'inspector.kind.selector': 'wählt eine Struktur',
  'inspector.kind.generated': 'nicht aus FHIR',
  'inspector.diff.title': 'Was der Rückweg nicht zurückgebracht hat',
  'inspector.diff.lede':
    'Jede Zeile hier ist ein Feld, das FHIR mitbrachte und für das das openEHR-Modell keinen Platz hat. Der Export liefert korrektes FHIR — nicht dasselbe FHIR.',

  'mapping.title': 'Die Regel, die das getan hat',
  'mapping.lede':
    'Deklaratives FHIR-Connect-YAML aus openfhir-bootstrap/ — ohne eine Zeile Java. Eine Bearbeitung hier schreibt die echte Datei und lässt openFHIR sie neu einlesen. Du kannst also ändern, wie die beiden Modelle einander entsprechen — oder es absichtlich kaputtmachen und genau zusehen, wo die Pipeline aufgibt. Alles ist ein Zurücksetzen vom Auslieferungsstand entfernt.',
  'mapping.edit': 'Dieses Mapping bearbeiten',
  'mapping.save': 'Speichern und openFHIR neu laden',
  'mapping.saving': 'Wird gespeichert…',
  'mapping.cancel': 'Abbrechen',
  'mapping.reset': 'Auf den Auslieferungsstand zurücksetzen',
  'mapping.edited': 'Diese Datei wurde bearbeitet.',
  'mapping.savedOk':
    'Gespeichert, und openFHIR hat seine Mappings neu geladen. Führe einen Trace aus, um zu sehen, was sich geändert hat.',
  'mapping.savedBad':
    'Gespeichert, aber openFHIR wollte es nicht laden: {0}. Es läuft weiterhin mit den Mappings, die es zuletzt gelesen hat.',
  'mapping.wasReset': 'Zurück auf den Stand, mit dem diese Anwendung ausgeliefert wurde.',
  'mapping.noDetail': 'keine nähere Angabe',

  'traffic.title': 'Standards-Verkehr',
  'traffic.lede':
    'Jeder Aufruf, den dieser Dienst an die beiden Standards-Server gemacht hat, neueste zuerst. Das ist die openEHR-REST-API und die Mapping-API von [[openFHIR]], wie sie wirklich sind — eine [[COMPOSITION]], die per POST an /ehr/{id}/composition geht, eine beantwortete [[AQL]]-Abfrage, ein [[template_id|Template]]-Upload, der mit 409 antwortet, weil das Template schon da ist. Header werden nicht aufgezeichnet: [[EHRbase]] läuft mit Basic Auth, und diese Zugangsdaten haben in einem Browser-Panel nichts verloren.',
  'traffic.both': 'Beide',
  'traffic.live': 'Live mitlesen',
  'traffic.clear': 'Leeren',
  'traffic.empty':
    'Noch nichts. Importiere eine Datei, öffne den Pipeline-Inspector oder lade einfach den Chart neu — jedes davon spricht mit einem Standards-Server, und es erscheint hier.',
  'traffic.request': 'Anfrage-Rumpf',
  'traffic.response': 'Antwort-Rumpf',
  'traffic.noBody':
    'Kein Antwort-Rumpf — die openEHR-API antwortet auf einen Schreibvorgang mit 204 und legt die Version-UID in den ETag-Header.',
  'traffic.failed': 'fehlgeschlagen',

  'diagram.alt':
    'HL7 FHIR bringt die Messung herein, openFHIR mappt sie, openEHR speichert sie — und wieder hinaus.',
  'diagram.fhir.role': 'Austausch',
  'diagram.openfhir.role': 'Übersetzung',
  'diagram.openehr.role': 'Persistenz · EHRbase',
  'diagram.import': 'Import',
  'diagram.export': 'Export',
  'diagram.store': 'speichern',
  'diagram.fhir.caption':
    'Sagt in Codes, was ein|Wert bedeutet. Gebaut für|den Weg über Systemgrenzen.',
  'diagram.openfhir.caption':
    'Kennt beide Modelle und|sonst nichts. Deklaratives|YAML, speichert nichts.',
  'diagram.openehr.caption':
    'Sagt durch die Position,|was ein Wert bedeutet. Gebaut,|um in 30 Jahren abfragbar zu sein.',

  'panel.nothing': 'Nichts anzuzeigen.',
  'panel.copy': 'Kopieren',
  'panel.copied': 'Kopiert',
  'panel.copyFailed': 'Kopieren fehlgeschlagen',
};

const DICTIONARIES: Record<Language, Dictionary> = { en: EN, de: DE };

/**
 * The text for `key`, with `{0}`, `{1}`… replaced by the arguments.
 *
 * An unknown key answers with itself rather than throwing: one typo should show up as a visible
 * oddity in one place, not blank a whole page.
 */
export function t(key: string, ...args: (string | number)[]): string {
  const text = DICTIONARIES[current][key] ?? EN[key] ?? key;
  return args.reduce<string>(
    (result, value, index) => result.replaceAll(`{${index}}`, String(value)),
    text,
  );
}

/** The locale to format dates and numbers in. */
export function locale(): string {
  return current === 'de' ? 'de-CH' : 'en-GB';
}
