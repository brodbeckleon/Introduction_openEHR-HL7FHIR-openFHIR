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
    'Resting heart rates travel as HL7 FHIR, are translated by openFHIR and kept in openEHR — while who they belong to stays in FHIR. The chart is the excuse; the point is what happens between them.',
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
    'A heart rate monitor: one resting heart rate per day — 58, in the field below. Storing it is trivial; storing it so another hospital, or this one in twenty years, still knows what it means takes three standards.',
  'tour.column.title': 'Why a database column is not enough',
  'tour.column.body':
    'A column bpm INT holds 58. It does not hold the unit, whether the patient was at rest, who measured it or with what. Ten years on, the number is still readable and its meaning is gone.',
  'tour.fhir.title': 'What HL7 FHIR contributes',
  'tour.fhir.body':
    'FHIR makes the number say what it is, in codes: LOINC 40443-4 means “heart rate, at rest”, UCUM /min is the unit, the category marks a vital sign. Codes are what let it cross a system boundary and still be understood.',
  'tour.openehr.title': 'What openEHR does differently',
  'tour.openehr.body':
    'The same reading in openEHR, and the LOINC code is gone: the meaning is now a place, inside the pulse archetype. In exchange openEHR insists on what FHIR never sent — who recorded it, in what language, in what setting.',
  'tour.lost.title': 'Two models, not one in two formats',
  'tour.lost.body':
    'Mapped back to FHIR, not everything returns. openEHR kept what the archetype models; the mapping writes the LOINC code back as a constant, but the display names are gone. Not a bug — that is what two genuinely different models cost.',
  'tour.translate.title': 'So something has to translate',
  'tour.translate.body':
    'So something has to translate: openFHIR, following declarative rules — which FHIR path corresponds to which openEHR path, and when. Not Java but YAML, and you can edit it right here, including breaking it on purpose to see what a rule was doing.',
  'tour.template.title': 'What the model allows, and what is used',
  'tour.template.body':
    'The model underneath: the operational template. EHRbase validates against it, openFHIR resolves paths in it — and only ten of its twenty-three nodes are used. The template says what is possible; the mappings decide what happens.',
  'tour.versions.title': 'The record does not forget',
  'tour.versions.body':
    'Correct a reading and openEHR does not overwrite it — it adds a version and keeps the old one. On the wire that is a PUT with If-Match, and EHRbase answers with the next version. Every call listed here really happened.',
  'tour.query.title': 'And it stays queryable',
  'tour.query.body':
    'And it stays queryable: AQL selects by archetype path, not by table column, so the same query runs on any openEHR system that knows the archetype. Try the example with a typo in the path — a wrong path is not an error.',
  'tour.done.title': 'That is the whole idea',
  'tour.done.body':
    'FHIR carries, openEHR remembers, openFHIR translates. Everything else here is detail about how. Underlined words explain themselves; the tour is in the header whenever you want it again, and Build your own has the recipe for a measurement of your own.',

  'tab.pipeline': 'Pipeline inspector',
  'tab.traffic': 'Standards traffic',
  'tab.aql': 'AQL playground',
  'tab.mappings': 'Mappings',

  'mappings.title': 'The FHIR Connect mappings',
  'mappings.lede':
    'Everything that decides how FHIR and openEHR correspond is in these three [[FHIR Connect]] files — YAML, not Java. Save a change and openFHIR maps with it from the next request; break one on purpose and watch where the pipeline gives up.',
  'mappings.focus': 'Opened at the rule behind “{0}”.',
  'rules.title': 'What becomes what',
  'rules.lede':
    'Read out of the files, so it follows every edit: a [[Observation|FHIR]] path on the left, the [[archetype_node_id|openEHR path]] on the right. Pick one to find it below.',
  'rules.sets': 'writes',
  'inspector.openRule': 'Open the rule in {0}',

  'tab.template': 'Template explorer',

  'template.title': 'Template explorer',
  'template.lede':
    'The operational [[template_id|template]]: what EHRbase validates a composition against and openFHIR resolves paths in. Every node a composition may have, its type, how often it may occur, and the path AQL would use.',
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
    'Worth finding: an INTERVAL_EVENT called “Maximum” under the pulse observation. The model has room for it; the mapping never fills it.',

  'aql.title': 'AQL playground',
  'aql.lede':
    'Run a query against the record. [[AQL]] selects by [[archetype_node_id|archetype path]], not by table column, and it is read-only: no AQL statement changes anything.',
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
    'The patient is the same story: EHR_STATUS has room for an identifier, not a name. So names live in a second store, the FHIR store, and one identifier joins the halves. Here they meet in one Bundle — and either store can fail without the other.',
  'diagram.open.fhir': 'Open the pipeline inspector, where the FHIR Observation is shown',
  'diagram.open.openfhir': 'Open the mappings \u2014 the FHIR Connect rules openFHIR executes',
  'diagram.open.openehr':
    'Open the template explorer \u2014 the openEHR model the record is validated against',
  'inspector.travel': 'Direction',
  'inspector.travel.in': 'The way in · POST',
  'inspector.travel.out': 'The way back · GET',
  'record.fhirStore': 'FHIR store',
  'record.fhirStore.what':
    'Name, gender, birth date, address — what openEHR’s EHR_STATUS has no room for. One table in the backend’s own database.',
  'record.openehr': 'openEHR, via openFHIR',
  'record.openehr.what':
    'The readings, stored as compositions and mapped back to FHIR on the way out. Each id is the openEHR versioned object uid, so no table translates.',
  'record.version': 'openEHR\u2019s version of this composition, surfaced as FHIR meta.versionId',
  'record.more': 'and {0} more',
  'record.note':
    'Stop the FHIR store’s database and the chart keeps drawing: the patients fall back to the configuration, because the two halves fail independently.',
  'lang.label': 'Language',
  'patient.label': 'Patient',
  'patient.ehr': 'The openEHR record this patient\u2019s readings are stored in',

  'stale.title': 'The backend is running an older build than this page.',
  'stale.body':
    'Rebuild it: `docker compose up -d --build backend` for the container, or restart `cd backend && ./gradlew bootRun` if it runs on the host — a plain `docker compose up` keeps the old image. Until then the pipeline inspector shows no stages and its direction tabs stay disabled, and corrections may not show up in the chart. Nothing is lost in the meantime; the record has every reading you entered.',

  'start.title': 'Start here',
  'start.lede':
    'One number per day, and it takes three standards: [[Observation|HL7 FHIR]] carries it in and out, [[openFHIR]] translates, [[EHRbase|openEHR]] keeps the record. The chart is only the proof; the tabs show how.',
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
    'Every call to openFHIR and EHRbase, with its request and response — the backend handing openFHIR its mappings included. This is the openEHR REST API as it really is, not as a spec describes it.',
  'start.step3.action': 'Open the standards traffic',
  'start.hide': 'Hide',
  'start.show': 'Show the introduction again',
  'start.showShort': 'Introduction',
  'start.hideLabel': 'Hide this introduction',

  'chart.title': 'Resting heart rate, last {0} days',
  'chart.subtitle':
    '{0} of {1} days recorded. Stored in openEHR as openEHR-EHR-OBSERVATION.pulse.v2, exchanged as LOINC 40443-4.',
  'chart.empty':
    'Nothing stored yet. Load the sample data below, type a reading, or import a FHIR Bundle.',
  'chart.loading': 'Loading…',
  'chart.loadError': '{0} — is the backend running on :18080 and `docker compose up` healthy?',

  'manual.title': 'Record a reading',
  'manual.lede':
    'One resting heart rate for one day, with no shortcut: the browser builds a FHIR Observation and posts it to /fhir/Observation, the same road a reading from another system takes.',
  'manual.date': 'Date',
  'manual.rate': 'Resting heart rate',
  'manual.store': 'Store it',
  'manual.correct': 'Correct it',
  'manual.storing': 'Storing…',
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
    'status, category and the LOINC code are what the mapping matches on; the UCUM unit is what the archetype constrains. Change one and the mapping no longer recognises the reading — the inspector’s third sample does exactly that.',
  'manual.builtInBrowser': 'built in the browser',

  'history.show': 'Show what this day used to say',
  'history.hide': 'Hide what this day used to say',
  'history.lede':
    'openEHR has no overwrite: a correction adds a version to the same [[COMPOSITION]] and keeps the one before, so the record can still answer “what did it say on the day someone acted on it?”',
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
    'asks the backend for a month of made-up readings, generated for today, and imports them as a FHIR Bundle. Export runs the same openFHIR mappings the other way.',
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
    'The import does this and throws it all away; here every stage is kept. Two journeys: a POST carries the reading in and ends at the write it would make, a GET brings back what the record holds.',
  'inspector.dry': 'Nothing was written \u2014 a trace only reads.',
  'inspector.ownFile': 'Your own file…',
  'inspector.running': 'Running…',
  'inspector.recognised': 'Recognised as',
  'inspector.links.title': 'What became what',
  'inspector.links.lede':
    'Pick one to light it up on both sides, and to see the FHIR Connect rule that put it there.',
  'inspector.kind.mapped': 'value copied',
  'inspector.kind.selector': 'chooses a structure',
  'inspector.kind.generated': 'not from FHIR',
  'inspector.diff.title': 'What the round trip did not bring back',
  'inspector.diff.lede':
    'Correct FHIR, not the same FHIR. lost marks what the mapping chose not to write back; added marks what openEHR gave the reading that FHIR never sent, such as its version.',

  'mapping.title': 'The rule that did it',
  'mapping.edit': 'Edit this mapping',
  'mapping.save': 'Save and hand it to openFHIR',
  'mapping.saving': 'Saving…',
  'mapping.cancel': 'Cancel',
  'mapping.reset': 'Reset to the shipped version',
  'mapping.edited': 'This file has been edited.',
  'mapping.savedOk': 'Saved, and openFHIR took the new version. Run a trace to see what changed.',
  'mapping.savedBad':
    'Saved, but openFHIR did not take it: {0}. It is still mapping with the version it had.',
  'mapping.wasReset': 'Back to the version this application shipped with.',
  'mapping.noDetail': 'no detail given',

  'traffic.title': 'Standards traffic',
  'traffic.lede':
    'Every call the backend made to openFHIR and [[EHRbase]], newest first — the openEHR REST API as it really is, not as a spec describes it.',
  'traffic.more':
    'A [[template_id|template]] upload that EHRbase answers with 409 means it already has it — and it keeps the old version. On every start the backend hands openFHIR the template and the mappings: a GET to find what it holds, a PUT to replace it. The patients are not here: they are the backend’s own database, and reading that is a query, not a call. Headers are not recorded — EHRbase runs with basic auth, and its credentials have no business in a browser panel.',
  'traffic.both': 'All',
  'traffic.live': 'Follow live',
  'traffic.clear': 'Clear',
  'traffic.empty':
    'Nothing yet. Import a file, open the pipeline inspector, or just reload the chart — every one of those talks to a standards server, and it will show up here.',
  'traffic.request': 'Request body',
  'traffic.response': 'Response body',
  'traffic.noBody':
    'No response body — the openEHR API answers 204 to a write and puts the version uid in the ETag header.',
  'traffic.failed': 'failed',

  'explainer.stores':
    'Three standards, two stores: the readings are openEHR compositions, the patient an ordinary FHIR resource, joined by one identifier.',
  'explainer.stores.link': 'See them meet in the pipeline',
  'explainer.backend':
    'Every arrow is the backend: it hands FHIR to openFHIR, writes the result to EHRbase and reads it back with AQL. The chart reads AQL directly; only FHIR going in or out is mapped.',
  'diagram.alt':
    'HL7 FHIR carries the reading in, openFHIR maps it, openEHR stores it — and back out again.',
  'diagram.fhir.role': 'exchange · who it is about',
  'diagram.openfhir.role': 'translation',
  'diagram.openehr.role': 'persistence · EHRbase',
  'diagram.import': 'import',
  'diagram.export': 'export',
  'diagram.write': 'backend writes',
  'diagram.read': 'backend · AQL',
  'diagram.fhir.caption':
    'Says what a value means,|in codes. Also stores who|it is about \u2014 openEHR cannot.',
  'diagram.openfhir.caption':
    'Knows both models and|nothing else. Declarative|YAML, stores no data.',
  'diagram.openehr.caption':
    'Says what a value means|by where it sits. Built to|be queried in 30 years.',

  'panel.nothing': 'Nothing to show.',
  'panel.copy': 'Copy',
  'panel.copied': 'Copied',
  'panel.copyFailed': 'Could not copy',
  more: 'More',

  'tab.build': 'Build your own',
  'build.title': 'Build your own',
  'build.lede':
    'For building the same structure around a measurement of your own: what it is made of, the order to change things in, and where it goes wrong. Every step points at its finished example here.',
  'build.pieces.title': 'What it is made of',
  'build.pieces.lede':
    'Three pieces are the structure — keep their roles and you have the same architecture. The backend is the part you write. The versions are exact, and checked against the files that pin them.',
  'build.structure': 'structure',
  'build.application': 'application',
  'build.pinnedIn': 'Pinned in',
  'build.docs': 'Documentation',
  'build.piece.ehrbase.name': 'EHRbase',
  'build.piece.ehrbase.role':
    'The openEHR clinical data repository. It validates every [[COMPOSITION]] against the template, keeps every version of it, and answers [[AQL]]. The readings live here and nowhere else.',
  'build.piece.openfhir.name': 'openFHIR',
  'build.piece.openfhir.role':
    'Translates between FHIR and openEHR by the [[FHIR Connect]] mappings the backend hands it over REST. Its version matters most: a mapping can only express what it implements — 3.0.1 cannot write a math_function.',
  'build.piece.fhirStore.name': 'FHIR store',
  'build.piece.fhirStore.role':
    'The administrative half: the patients, with the names and addresses openEHR has no room for. No readings. A database, not a server — one table in the backend’s own Postgres.',
  'build.piece.backend.name': 'Backend',
  'build.piece.backend.role':
    'The one service in the middle: it hands FHIR to openFHIR, stores the result in EHRbase, reads it back with AQL and keeps the patients. Everything that knows it is about heart rates is in here.',
  'build.steps.title': 'Your own measurement, step by step',
  'build.steps.lede':
    'Say body weight instead of a resting heart rate. The order matters: each step builds on the one before, and each has a finished example here to look at first.',
  'build.files': 'Files this step changes',
  'build.step.archetype.title': 'Find the archetype',
  'build.step.archetype.body':
    'Look your measurement up in the openEHR Clinical Knowledge Manager first: body weight is openEHR-EHR-OBSERVATION.body_weight.v2. An [[archetype_id|archetype]] is worth something because others use the same one.',
  'build.step.archetype.action': 'The pulse archetype in the template explorer',
  'build.step.template.title': 'Build a template and export it',
  'build.step.template.body':
    'In the Archetype Designer, put the archetype into a composition, constrain it to what you record, and export an operational template (ADL 1.4). Replace the one .opt below, and its [[template_id]] in application.yml.',
  'build.step.template.action': 'What a template allows, and what is used',
  'build.step.fhir.title': 'Decide what it looks like in FHIR',
  'build.step.fhir.body':
    'Which resource, which code, which unit. The FHIR vital-signs profile settles it: body weight is an [[Observation]] with [[LOINC]] 29463-7 in kg. The mapping will recognise your measurement by these codes.',
  'build.step.fhir.action': 'The heart rate as an Observation',
  'build.step.mappings.title': 'Write the mappings',
  'build.step.mappings.body':
    'Copy the three heart rate files and change them: the context names the template, the composition mapping routes Bundle entries, the model pairs FHIR paths with archetype paths and lists the codes that recognise the resource.',
  'build.step.mappings.action': 'The heart rate’s mappings',
  'build.step.backend.title': 'Teach the backend the new measurement',
  'build.step.backend.body':
    'The backend is the one piece that knows it is dealing with heart rates. Everything in these files that says pulse or 40443-4 has to change.',
  'build.step.load.title': 'Load it, and try it before storing anything',
  'build.step.load.body':
    'A backend start hands the template to EHRbase and openFHIR, and the mappings to openFHIR. The second command shows what openFHIR holds; the last sends it a Bundle directly — an empty composition means a condition did not match.',
  'build.step.load.action': 'The same calls in the traffic console',
  'build.step.query.title': 'Query it with AQL',
  'build.step.query.body':
    'Change the archetype in CONTAINS and the path in SELECT of the chart’s query. Copy the path from the template explorer — a wrong path in [[AQL]] is not an error, just a column of nulls.',
  'build.step.query.action': 'Try it in the AQL playground',
  'build.pitfalls.title': 'Where it goes wrong',
  'build.pitfalls.lede':
    'What these have in common: the symptom points somewhere other than the cause. Guess the cause before you open one.',
  'build.pitfall.template.symptom': 'A changed template has no effect',
  'build.pitfall.template.cause':
    'EHRbase keeps the first template it is given under an id and answers every later upload with 409, which the backend takes to mean “already there”. openFHIR takes the new version; EHRbase does not. Give a changed template a new [[template_id]], such as heartrate_monitor.v2.',
  'build.pitfall.stale.symptom': 'openFHIR still maps with the old version',
  'build.pitfall.stale.cause':
    'openFHIR keeps what it is given in MongoDB and never looks at the files. The backend hands them over when it starts and when the editor saves one — a file you change in an editor of your own reaches openFHIR on the next backend start, not before.',
  'build.pitfall.empty.symptom': 'openFHIR answers with an empty composition',
  'build.pitfall.empty.cause':
    'No condition in the model mapping matched. The resource is valid FHIR and openFHIR is fine; the resource just does not carry a code the mapping requires. Compare the two character by character.',
  'build.pitfall.interval.symptom': 'A minimum, maximum or mean will not map',
  'build.pitfall.interval.cause':
    'An [[INTERVAL_EVENT]] needs a math_function, and FHIR Connect as openFHIR 3.0.1 implements it cannot write one. Check what the version you run can do before designing a template around it.',
  'build.pitfall.nulls.symptom': 'Every AQL value comes back null',
  'build.pitfall.nulls.cause':
    'The path does not exist. [[AQL]] does not treat a wrong path as an error: the query matches the observations and finds nothing at that path.',
  'build.pitfall.dates.symptom': 'An AQL date filter matches nothing',
  'build.pitfall.dates.cause':
    'EHRbase compares [[DV_DATE_TIME]] bounds as text. 2026-09-08T00:00Z is valid ISO 8601 and silently matches nothing; write the seconds out, 2026-09-08T00:00:00Z.',
  'build.pitfall.versions.symptom': 'Every version in a history shows the same value',
  'build.pitfall.versions.cause':
    'The version uid belongs in the path, …/version/{uid}. Passed as ?version_uid= it is accepted, and EHRbase answers with the latest version every time.',
  'build.pitfall.port.symptom': 'Backend changes do not show up',
  'build.pitfall.port.cause':
    'An older backend still holds port 18080. ./gradlew bootRun fails with “Port 18080 was already in use”, the message scrolls past, and the old build keeps answering. This page notices when the backend is older than itself, and says so.',
  'build.pitfall.linux.symptom': 'On Linux, a container cannot reach the host',
  'build.pitfall.linux.cause':
    'host.docker.internal resolves on its own only in Docker Desktop. On Linux the service needs extra_hosts: host.docker.internal:host-gateway, as the frontend has in docker-compose.yml.',
  'build.footer':
    'Installing it, the ports and what your machine needs are in the README. It is MIT-licensed: take it apart and build on it.',
  'build.footer.readme': 'README on GitHub',
};

const DE: Dictionary = {
  'app.title': 'Ruhepuls-Monitor',
  'app.lede':
    'Ruhepulse reisen als HL7 FHIR, werden von openFHIR übersetzt und in openEHR gespeichert — während in FHIR bleibt, zu wem sie gehören. Der Chart ist nur der Vorwand; worum es geht, passiert dazwischen.',
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
    'Ein Ruhepuls-Monitor: ein Ruhepuls pro Tag — 58, im Feld darunter. Ihn zu speichern ist trivial; ihn so zu speichern, dass ein anderes Krankenhaus oder dasselbe in zwanzig Jahren noch weiß, was er bedeutet, braucht drei Standards.',
  'tour.column.title': 'Warum eine Datenbankspalte nicht reicht',
  'tour.column.body':
    'Eine Spalte bpm INT enthält 58. Sie enthält nicht die Einheit, ob in Ruhe gemessen wurde, von wem und womit. Zehn Jahre später ist die Zahl noch lesbar und ihre Bedeutung weg.',
  'tour.fhir.title': 'Was HL7 FHIR beiträgt',
  'tour.fhir.body':
    'FHIR lässt die Zahl in Codes sagen, was sie ist: LOINC 40443-4 heißt „Herzfrequenz in Ruhe“, UCUM /min ist die Einheit, die Kategorie macht sie zum Vitalparameter. Codes lassen sie eine Systemgrenze überqueren und drüben verstanden werden.',
  'tour.openehr.title': 'Was openEHR anders macht',
  'tour.openehr.body':
    'Dieselbe Messung in openEHR, und der LOINC-Code ist weg: Die Bedeutung ist jetzt ein Ort, im Puls-Archetyp. Dafür verlangt openEHR, was FHIR nie geschickt hat — wer es erfasst hat, in welcher Sprache, in welchem Kontext.',
  'tour.lost.title': 'Zwei Modelle, nicht ein Modell in zwei Formaten',
  'tour.lost.body':
    'Zurück nach FHIR gemappt, kommt nicht alles wieder. openEHR hat behalten, was der Archetyp modelliert; das Mapping schreibt den LOINC-Code als Konstante zurück, die Anzeigenamen aber fehlen. Kein Fehler — das kosten zwei wirklich verschiedene Modelle.',
  'tour.translate.title': 'Also muss jemand übersetzen',
  'tour.translate.body':
    'Also muss jemand übersetzen: openFHIR, nach deklarativen Regeln — welcher FHIR-Pfad welchem openEHR-Pfad entspricht, und wann. Kein Java, sondern YAML, und du kannst es hier bearbeiten, auch absichtlich kaputt, um zu sehen, was eine Regel getan hat.',
  'tour.template.title': 'Was das Modell erlaubt und was benutzt wird',
  'tour.template.body':
    'Das Modell darunter: das Operational Template. EHRbase prüft dagegen, openFHIR löst Pfade darin auf — und nur zehn seiner dreiundzwanzig Knoten werden genutzt. Das Template sagt, was möglich ist; die Mappings entscheiden, was passiert.',
  'tour.versions.title': 'Die Akte vergisst nicht',
  'tour.versions.body':
    'Korrigiert man eine Messung, überschreibt openEHR sie nicht — es legt eine Version an und behält die alte. Auf der Leitung ist das ein PUT mit If-Match, und EHRbase antwortet mit der nächsten Version. Jeder Aufruf hier ist wirklich passiert.',
  'tour.query.title': 'Und sie bleibt abfragbar',
  'tour.query.body':
    'Und sie bleibt abfragbar: AQL selektiert über Archetyp-Pfade statt über Tabellenspalten, deshalb läuft dieselbe Abfrage auf jedem openEHR-System, das den Archetyp kennt. Probier das Beispiel mit dem Tippfehler im Pfad — ein falscher Pfad ist kein Fehler.',
  'tour.done.title': 'Das ist die ganze Idee',
  'tour.done.body':
    'FHIR transportiert, openEHR erinnert, openFHIR übersetzt. Alles andere hier ist Detail dazu, wie. Unterstrichene Wörter erklären sich selbst; die Tour findest du jederzeit oben im Header, und unter „Nachbauen“ steht das Rezept für einen eigenen Messwert.',

  'tab.pipeline': 'Pipeline-Inspector',
  'tab.traffic': 'Standards-Verkehr',
  'tab.aql': 'AQL-Playground',
  'tab.mappings': 'Mappings',

  'mappings.title': 'Die FHIR-Connect-Mappings',
  'mappings.lede':
    'Alles, was entscheidet, wie FHIR und openEHR einander entsprechen, steht in diesen drei [[FHIR Connect]]-Dateien — YAML, kein Java. Speicherst du eine Änderung, mappt openFHIR ab der nächsten Anfrage damit; mach eine absichtlich kaputt und sieh zu, wo die Pipeline aufgibt.',
  'mappings.focus': 'Geöffnet bei der Regel hinter „{0}".',
  'rules.title': 'Was wird wozu',
  'rules.lede':
    'Aus den Dateien gelesen, folgt also jeder Bearbeitung: links ein [[Observation|FHIR]]-Pfad, rechts der [[archetype_node_id|openEHR-Pfad]]. Wähle einen aus, um ihn unten zu finden.',
  'rules.sets': 'schreibt',
  'inspector.openRule': 'Regel in {0} öffnen',

  'tab.template': 'Template-Explorer',

  'template.title': 'Template-Explorer',
  'template.lede':
    'Das operationale [[template_id|Template]]: wogegen EHRbase eine Composition prüft und worin openFHIR Pfade auflöst. Jeder Knoten, den eine Composition haben darf, sein Typ, wie oft er vorkommen darf, und der Pfad, den AQL nehmen würde.',
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
    'Lohnt sich zu suchen: ein INTERVAL_EVENT namens „Maximum“ unter der Puls-Observation. Das Modell hat Platz dafür; das Mapping befüllt es nie.',

  'aql.title': 'AQL-Playground',
  'aql.lede':
    'Führe eine Abfrage gegen die Akte aus. [[AQL]] selektiert über [[archetype_node_id|Archetyp-Pfade]] statt über Tabellenspalten, und es liest nur: Kein AQL-Statement verändert etwas.',
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
    'Beim Patienten ist es dieselbe Geschichte: EHR_STATUS hat Platz für eine Kennung, nicht für einen Namen. Namen liegen deshalb in einem zweiten Speicher, dem FHIR-Store, und eine Kennung verbindet die Hälften. Hier treffen sie sich in einem Bundle — und jeder Speicher kann ausfallen, ohne den anderen mitzunehmen.',
  'diagram.open.fhir': 'Pipeline-Inspector \u00f6ffnen, wo die FHIR-Observation zu sehen ist',
  'diagram.open.openfhir':
    'Mappings \u00f6ffnen \u2014 die FHIR-Connect-Regeln, die openFHIR ausf\u00fchrt',
  'diagram.open.openehr':
    'Template-Explorer \u00f6ffnen \u2014 das openEHR-Modell, gegen das validiert wird',
  'inspector.travel': 'Richtung',
  'inspector.travel.in': 'Hinweg · POST',
  'inspector.travel.out': 'R\u00fcckweg · GET',
  'record.fhirStore': 'FHIR-Store',
  'record.fhirStore.what':
    'Name, Geschlecht, Geburtsdatum, Adresse — wofür EHR_STATUS in openEHR keinen Platz hat. Eine Tabelle in der eigenen Datenbank des Backends.',
  'record.openehr': 'openEHR, \u00fcber openFHIR',
  'record.openehr.what':
    'Die Messwerte, als Compositions gespeichert und beim Herausgeben nach FHIR zurückgemappt. Jede Id ist die UID des openEHR-Objekts, deshalb übersetzt keine Tabelle.',
  'record.version': 'openEHRs Version dieser Composition, sichtbar gemacht als FHIR meta.versionId',
  'record.more': 'und {0} weitere',
  'record.note':
    'Stoppt man die Datenbank des FHIR-Stores, zeichnet der Chart weiter: Die Patienten fallen auf die Konfiguration zurück, weil die beiden Hälften unabhängig ausfallen.',
  'lang.label': 'Sprache',
  'patient.label': 'Patient',
  'patient.ehr': 'Der openEHR-Record, in dem die Messwerte dieses Patienten liegen',

  'stale.title': 'Das Backend läuft mit einem älteren Stand als diese Seite.',
  'stale.body':
    'Baue es neu: `docker compose up -d --build backend` für den Container, oder starte `cd backend && ./gradlew bootRun` neu, wenn es auf dem Host läuft — ein bloßes `docker compose up` behält das alte Image. Bis dahin zeigt der Pipeline-Inspector keine Stufen und seine Richtungs-Tabs bleiben ausgegraut, und Korrekturen tauchen womöglich nicht im Diagramm auf. Verloren geht derweil nichts; der Record hat jede Messung, die du eingegeben hast.',

  'start.title': 'Hier anfangen',
  'start.lede':
    'Eine Zahl pro Tag, und dafür braucht es drei Standards: [[Observation|HL7 FHIR]] bringt sie herein und heraus, [[openFHIR]] übersetzt, [[EHRbase|openEHR]] führt die Akte. Der Chart ist nur der Beleg; die Tabs zeigen, wie.',
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
    'Jeder Aufruf an openFHIR und EHRbase, mit Anfrage und Antwort — auch wie das Backend openFHIR seine Mappings übergibt. Das ist die openEHR-REST-API, wie sie wirklich ist — nicht, wie eine Spezifikation sie beschreibt.',
  'start.step3.action': 'Standards-Verkehr öffnen',
  'start.hide': 'Ausblenden',
  'start.show': 'Einführung wieder einblenden',
  'start.showShort': 'Einführung',
  'start.hideLabel': 'Diese Einführung ausblenden',

  'chart.title': 'Ruhepuls, letzte {0} Tage',
  'chart.subtitle':
    '{0} von {1} Tagen aufgezeichnet. In openEHR gespeichert als openEHR-EHR-OBSERVATION.pulse.v2, ausgetauscht als LOINC 40443-4.',
  'chart.empty':
    'Noch nichts gespeichert. Lade unten die Beispieldaten, gib eine Messung ein oder importiere ein FHIR Bundle.',
  'chart.loading': 'Wird geladen…',
  'chart.loadError': '{0} — läuft das Backend auf :18080 und ist `docker compose up` gesund?',

  'manual.title': 'Eine Messung erfassen',
  'manual.lede':
    'Ein Ruhepuls für einen Tag, ohne Abkürzung: Der Browser baut eine FHIR Observation und schickt sie an /fhir/Observation — derselbe Weg, den eine Messung aus einem fremden System nimmt.',
  'manual.date': 'Datum',
  'manual.rate': 'Ruhepuls',
  'manual.store': 'Speichern',
  'manual.correct': 'Korrigieren',
  'manual.storing': 'Wird gespeichert…',
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
    'status, category und der LOINC-Code sind das, worauf das Mapping prüft; die UCUM-Einheit ist das, was der Archetyp einschränkt. Ändere eines davon, und das Mapping erkennt die Messung nicht mehr — das dritte Beispiel im Inspector macht genau das.',
  'manual.builtInBrowser': 'im Browser gebaut',

  'history.show': 'Zeigen, was dieser Tag früher sagte',
  'history.hide': 'Frühere Stände ausblenden',
  'history.lede':
    'openEHR kennt kein Überschreiben: Eine Korrektur fügt derselben [[COMPOSITION]] eine Version hinzu und behält die davor. So kann die Akte weiterhin beantworten: „Was stand dort an dem Tag, an dem jemand danach gehandelt hat?“',
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
    'holt vom Backend einen Monat erfundener Messungen, für heute erzeugt, und importiert sie als FHIR Bundle. Der Export nimmt dieselben openFHIR-Mappings in die andere Richtung.',
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
    'Der Import macht das und wirft alles weg; hier bleibt jede Stufe erhalten. Zwei Wege: Ein POST trägt die Messung hinein und endet beim Schreibzugriff, den er machen würde, ein GET bringt zurück, was die Akte hält.',
  'inspector.dry': 'Geschrieben wurde nichts \u2014 ein Trace liest nur.',
  'inspector.ownFile': 'Eigene Datei…',
  'inspector.running': 'Läuft…',
  'inspector.recognised': 'Erkannt als',
  'inspector.links.title': 'Was wurde wozu',
  'inspector.links.lede':
    'Wähle eines aus, um es auf beiden Seiten aufleuchten zu lassen und die FHIR-Connect-Regel zu sehen, die es dorthin gebracht hat.',
  'inspector.kind.mapped': 'Wert kopiert',
  'inspector.kind.selector': 'wählt eine Struktur',
  'inspector.kind.generated': 'nicht aus FHIR',
  'inspector.diff.title': 'Was der Rückweg nicht zurückgebracht hat',
  'inspector.diff.lede':
    'Korrektes FHIR, nicht dasselbe FHIR. „lost“ markiert, was das Mapping absichtlich nicht zurückschreibt; „added“, was openEHR der Messung mitgegeben hat und FHIR nie geschickt hatte, etwa ihre Version.',

  'mapping.title': 'Die Regel, die das getan hat',
  'mapping.edit': 'Dieses Mapping bearbeiten',
  'mapping.save': 'Speichern und an openFHIR übergeben',
  'mapping.saving': 'Wird gespeichert…',
  'mapping.cancel': 'Abbrechen',
  'mapping.reset': 'Auf den Auslieferungsstand zurücksetzen',
  'mapping.edited': 'Diese Datei wurde bearbeitet.',
  'mapping.savedOk':
    'Gespeichert, und openFHIR hat die neue Version übernommen. Führe einen Trace aus, um zu sehen, was sich geändert hat.',
  'mapping.savedBad':
    'Gespeichert, aber openFHIR hat es nicht übernommen: {0}. Es mappt weiterhin mit der Version, die es hatte.',
  'mapping.wasReset': 'Zurück auf den Stand, mit dem diese Anwendung ausgeliefert wurde.',
  'mapping.noDetail': 'keine nähere Angabe',

  'traffic.title': 'Standards-Verkehr',
  'traffic.lede':
    'Jeder Aufruf des Backends an openFHIR und [[EHRbase]], neueste zuerst — die openEHR-REST-API, wie sie wirklich ist, nicht wie eine Spezifikation sie beschreibt.',
  'traffic.more':
    'Ein [[template_id|Template]]-Upload, den EHRbase mit 409 beantwortet, heißt: Es hat das Template schon — und behält die alte Version. Bei jedem Start übergibt das Backend openFHIR Template und Mappings: ein GET, um zu sehen, was es hält, ein PUT, um es zu ersetzen. Die Patienten stehen nicht hier: Sie liegen in der eigenen Datenbank des Backends, und die zu lesen ist eine Abfrage, kein Aufruf. Header werden nicht aufgezeichnet — EHRbase läuft mit Basic Auth, und diese Zugangsdaten haben in einem Browser-Panel nichts verloren.',
  'traffic.both': 'Alle',
  'traffic.live': 'Live mitlesen',
  'traffic.clear': 'Leeren',
  'traffic.empty':
    'Noch nichts. Importiere eine Datei, öffne den Pipeline-Inspector oder lade einfach den Chart neu — jedes davon spricht mit einem Standards-Server, und es erscheint hier.',
  'traffic.request': 'Anfrage-Rumpf',
  'traffic.response': 'Antwort-Rumpf',
  'traffic.noBody':
    'Kein Antwort-Rumpf — die openEHR-API antwortet auf einen Schreibvorgang mit 204 und legt die Version-UID in den ETag-Header.',
  'traffic.failed': 'fehlgeschlagen',

  'explainer.stores':
    'Drei Standards, zwei Speicher: Die Messwerte sind openEHR-Compositions, der Patient eine gewöhnliche FHIR-Ressource, verbunden durch eine Kennung.',
  'explainer.stores.link': 'Im Pipeline-Inspector ansehen',
  'explainer.backend':
    'Jeder Pfeil ist das Backend: Es gibt FHIR an openFHIR, schreibt das Ergebnis nach EHRbase und liest es per AQL zurück. Der Chart liest AQL direkt; gemappt wird nur FHIR, das hinein- oder hinausgeht.',
  'diagram.alt':
    'HL7 FHIR bringt die Messung herein, openFHIR mappt sie, openEHR speichert sie — und wieder hinaus.',
  'diagram.fhir.role': 'Austausch · um wen es geht',
  'diagram.openfhir.role': 'Übersetzung',
  'diagram.openehr.role': 'Persistenz · EHRbase',
  'diagram.import': 'Import',
  'diagram.export': 'Export',
  'diagram.write': 'Backend schreibt',
  'diagram.read': 'Backend · AQL',
  'diagram.fhir.caption':
    'Sagt in Codes, was ein Wert|bedeutet. Speichert auch, zu|wem \u2014 openEHR kann das nicht.',
  'diagram.openfhir.caption':
    'Kennt beide Modelle und|sonst nichts. Deklaratives|YAML, speichert nichts.',
  'diagram.openehr.caption':
    'Sagt durch die Position,|was ein Wert bedeutet. Gebaut,|um in 30 Jahren abfragbar zu sein.',

  'panel.nothing': 'Nichts anzuzeigen.',
  'panel.copy': 'Kopieren',
  'panel.copied': 'Kopiert',
  'panel.copyFailed': 'Kopieren fehlgeschlagen',
  more: 'Mehr dazu',

  'tab.build': 'Nachbauen',
  'build.title': 'Selbst nachbauen',
  'build.lede':
    'Für dieselbe Struktur um einen eigenen Messwert herum: woraus sie besteht, in welcher Reihenfolge du was änderst und wo es schiefgeht. Jeder Schritt zeigt auf sein fertiges Beispiel hier.',
  'build.pieces.title': 'Woraus es besteht',
  'build.pieces.lede':
    'Drei Teile sind die Struktur — behältst du ihre Rollen, hast du dieselbe Architektur. Das Backend ist der Teil, den du schreibst. Die Versionen sind exakt und gegen die Dateien geprüft, in denen sie festgelegt sind.',
  'build.structure': 'Struktur',
  'build.application': 'Anwendung',
  'build.pinnedIn': 'Festgelegt in',
  'build.docs': 'Dokumentation',
  'build.piece.ehrbase.name': 'EHRbase',
  'build.piece.ehrbase.role':
    'Das openEHR-Repository für klinische Daten. Es prüft jede [[COMPOSITION]] gegen das Template, behält jede Version davon und beantwortet [[AQL]]. Die Messwerte liegen hier und sonst nirgends.',
  'build.piece.openfhir.name': 'openFHIR',
  'build.piece.openfhir.role':
    'Übersetzt zwischen FHIR und openEHR nach den [[FHIR Connect]]-Mappings, die ihm das Backend per REST übergibt. Seine Version zählt am meisten: Ein Mapping kann nur ausdrücken, was sie umsetzt — 3.0.1 kann keine math_function schreiben.',
  'build.piece.fhirStore.name': 'FHIR-Store',
  'build.piece.fhirStore.role':
    'Die administrative Hälfte: die Patienten, mit den Namen und Adressen, für die openEHR keinen Platz hat. Keine Messwerte. Eine Datenbank, kein Server — eine Tabelle in der eigenen Postgres des Backends.',
  'build.piece.backend.name': 'Backend',
  'build.piece.backend.role':
    'Der eine Dienst in der Mitte: Er gibt FHIR an openFHIR, speichert das Ergebnis in EHRbase, liest es per AQL zurück und hält die Patienten. Alles, was weiß, dass es um Ruhepulse geht, steckt hier.',
  'build.steps.title': 'Ein eigener Messwert, Schritt für Schritt',
  'build.steps.lede':
    'Sagen wir: Körpergewicht statt Ruhepuls. Die Reihenfolge zählt: Jeder Schritt baut auf dem vorigen auf, und zu jedem gibt es hier ein fertiges Beispiel, das du dir zuerst ansehen kannst.',
  'build.files': 'Dateien, die dieser Schritt ändert',
  'build.step.archetype.title': 'Den Archetyp finden',
  'build.step.archetype.body':
    'Schlag deinen Messwert zuerst im openEHR Clinical Knowledge Manager nach: Körpergewicht ist openEHR-EHR-OBSERVATION.body_weight.v2. Ein [[archetype_id|Archetyp]] ist etwas wert, weil andere denselben verwenden.',
  'build.step.archetype.action': 'Der Puls-Archetyp im Template-Explorer',
  'build.step.template.title': 'Ein Template bauen und exportieren',
  'build.step.template.body':
    'Setz den Archetyp im Archetype Designer in eine Composition, schränk ihn auf das ein, was du erfasst, und exportier ein Operational Template (ADL 1.4). Ersetz die eine .opt unten und ihre [[template_id]] in application.yml.',
  'build.step.template.action': 'Was ein Template erlaubt und was genutzt wird',
  'build.step.fhir.title': 'Festlegen, wie es in FHIR aussieht',
  'build.step.fhir.body':
    'Welche Ressource, welcher Code, welche Einheit. Das FHIR-Vital-Signs-Profil legt es fest: Körpergewicht ist eine [[Observation]] mit [[LOINC]] 29463-7 in kg. An diesen Codes erkennt das Mapping deinen Messwert.',
  'build.step.fhir.action': 'Der Ruhepuls als Observation',
  'build.step.mappings.title': 'Die Mappings schreiben',
  'build.step.mappings.body':
    'Kopier die drei Ruhepuls-Dateien und pass sie an: Der Kontext nennt das Template, das Composition-Mapping verteilt die Bundle-Einträge, das Modell ordnet FHIR-Pfade Archetyp-Pfaden zu und listet die Codes, an denen es die Ressource erkennt.',
  'build.step.mappings.action': 'Die Mappings des Ruhepulses',
  'build.step.backend.title': 'Dem Backend den neuen Messwert beibringen',
  'build.step.backend.body':
    'Das Backend ist das einzige Teil, das weiß, dass es um Ruhepulse geht. Alles in diesen Dateien, was pulse oder 40443-4 sagt, muss sich ändern.',
  'build.step.load.title': 'Laden, und ausprobieren, bevor etwas gespeichert wird',
  'build.step.load.body':
    'Ein Start des Backends übergibt das Template an EHRbase und openFHIR und die Mappings an openFHIR. Der zweite Befehl zeigt, was openFHIR hält; der letzte schickt ihm direkt ein Bundle — eine leere Composition heißt: Eine Bedingung hat nicht gepasst.',
  'build.step.load.action': 'Dieselben Aufrufe im Standards-Verkehr',
  'build.step.query.title': 'Mit AQL abfragen',
  'build.step.query.body':
    'Ändere den Archetyp im CONTAINS und den Pfad im SELECT der Chart-Abfrage. Kopier den Pfad aus dem Template-Explorer — ein falscher Pfad ist in [[AQL]] kein Fehler, nur eine Spalte voller null.',
  'build.step.query.action': 'Im AQL-Playground ausprobieren',
  'build.pitfalls.title': 'Wo es schiefgeht',
  'build.pitfalls.lede':
    'Was sie gemeinsam haben: Das Symptom zeigt woanders hin als die Ursache. Rate die Ursache, bevor du einen aufklappst.',
  'build.pitfall.template.symptom': 'Ein geändertes Template wirkt nicht',
  'build.pitfall.template.cause':
    'EHRbase behält das erste Template, das es unter einer ID bekommt, und beantwortet jeden späteren Upload mit 409 — was das Backend als „schon da“ versteht. openFHIR übernimmt die neue Version, EHRbase nicht. Gib einem geänderten Template eine neue [[template_id]], etwa heartrate_monitor.v2.',
  'build.pitfall.stale.symptom': 'openFHIR mappt noch mit der alten Version',
  'build.pitfall.stale.cause':
    'openFHIR behält, was es bekommt, in MongoDB und schaut nie in die Dateien. Das Backend übergibt sie beim Start und wenn der Editor eine speichert — eine Datei, die du in einem eigenen Editor änderst, kommt erst beim nächsten Start des Backends bei openFHIR an.',
  'build.pitfall.empty.symptom': 'openFHIR antwortet mit einer leeren Composition',
  'build.pitfall.empty.cause':
    'Keine Bedingung im Model-Mapping hat gepasst. Die Ressource ist gültiges FHIR, und openFHIR läuft; der Ressource fehlt bloß ein Code, den das Mapping verlangt. Vergleich die beiden Zeichen für Zeichen.',
  'build.pitfall.interval.symptom': 'Minimum, Maximum oder Mittelwert lassen sich nicht mappen',
  'build.pitfall.interval.cause':
    'Ein [[INTERVAL_EVENT]] braucht eine math_function, und FHIR Connect, wie openFHIR 3.0.1 es umsetzt, kann keine schreiben. Prüf, was deine Version kann, bevor du ein Template darum herum entwirfst.',
  'build.pitfall.nulls.symptom': 'Jeder AQL-Wert kommt als null zurück',
  'build.pitfall.nulls.cause':
    'Den Pfad gibt es nicht. [[AQL]] behandelt einen falschen Pfad nicht als Fehler: Die Abfrage findet die Observations und an diesem Pfad nichts.',
  'build.pitfall.dates.symptom': 'Ein AQL-Datumsfilter findet nichts',
  'build.pitfall.dates.cause':
    'EHRbase vergleicht [[DV_DATE_TIME]]-Grenzen als Text. 2026-09-08T00:00Z ist gültiges ISO 8601 und findet trotzdem stillschweigend nichts; schreib die Sekunden aus, 2026-09-08T00:00:00Z.',
  'build.pitfall.versions.symptom': 'Jede Version in einer Historie zeigt denselben Wert',
  'build.pitfall.versions.cause':
    'Die Version-UID gehört in den Pfad, …/version/{uid}. Als ?version_uid= wird sie angenommen, und EHRbase antwortet jedes Mal mit der neuesten Version.',
  'build.pitfall.port.symptom': 'Änderungen am Backend kommen nicht an',
  'build.pitfall.port.cause':
    'Ein älteres Backend hält noch Port 18080. ./gradlew bootRun scheitert mit „Port 18080 was already in use“, die Meldung scrollt vorbei, und der alte Build antwortet weiter. Diese Seite merkt, wenn das Backend älter ist als sie selbst, und sagt es.',
  'build.pitfall.linux.symptom': 'Unter Linux erreicht ein Container den Host nicht',
  'build.pitfall.linux.cause':
    'host.docker.internal löst sich nur in Docker Desktop von selbst auf. Unter Linux braucht der Service extra_hosts: host.docker.internal:host-gateway, wie ihn das Frontend in docker-compose.yml hat.',
  'build.footer':
    'Wie man es installiert, die Ports und was dein Rechner braucht, stehen in der README. Es steht unter der MIT-Lizenz: Nimm es auseinander und bau darauf auf.',
  'build.footer.readme': 'README auf GitHub',
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
