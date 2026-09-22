/**
 * The guided tour: the argument the rest of the application assumes you already accept.
 *
 * Every other tab explains *how* the three standards work. None of them answers why one heart rate
 * needs three standards at all, and that is the question someone meeting openEHR for the first time
 * actually has. The tour is that answer, told as one story, using the tabs as its stage.
 *
 * Each step names the tab it belongs on and, where there is one, a selector to draw the eye to. The
 * text lives in the dictionaries like everything else.
 */

export interface TourStep {
  /** The tab this step is told on. */
  tab: 'overview' | 'pipeline' | 'traffic' | 'aql' | 'mappings' | 'template' | 'record';
  /** Message key prefix; `<key>.title` and `<key>.body` must exist in both languages. */
  key: string;
  /** Optional element to ring and scroll to — skipped silently when it is not on screen. */
  focus?: string;
  /**
   * A stage of the pipeline inspector to select first, by its id. Only meaningful on the pipeline
   * tab, and the reason the two openEHR steps can point at the right JSON.
   */
  stage?: string;
}

export const TOUR: TourStep[] = [
  { tab: 'overview', key: 'tour.what', focus: '.chart' },
  // The manual entry form, not the statistics card: the form defaults to 58, so the number the
  // step names is literally on screen. The card shows whatever happens to be stored.
  { tab: 'overview', key: 'tour.number', focus: '.manual' },
  { tab: 'overview', key: 'tour.column', focus: '.explainer' },
  { tab: 'pipeline', key: 'tour.fhir', stage: 'observation', focus: '.panels' },
  { tab: 'pipeline', key: 'tour.openehr', stage: 'composition', focus: '.panels' },
  { tab: 'pipeline', key: 'tour.lost', stage: 'roundtrip', focus: '.differences' },
  // Straight after 'lost': that step has just shown openEHR dropping what its archetype does not
  // model. This is the other half of the same observation — some of what it drops has to live
  // somewhere, and that somewhere is a second store.
  { tab: 'record', key: 'tour.stores', focus: '.columns' },
  { tab: 'mappings', key: 'tour.translate', focus: '.mapping' },
  // After the mappings, never before: the point of the template is the contrast with them, and
  // that only lands once the reader knows what a mapping is.
  { tab: 'template', key: 'tour.template', focus: '.summary' },
  { tab: 'overview', key: 'tour.versions', focus: '.manual' },
  // Straight after versioning: this is where the PUT that step just described is visible.
  { tab: 'traffic', key: 'tour.traffic', focus: '.calls' },
  { tab: 'aql', key: 'tour.query', focus: '.examples' },
  { tab: 'overview', key: 'tour.done' },
];

const STORAGE_KEY = 'hrm.tour.seen';

export function tourSeen(): boolean {
  return localStorage.getItem(STORAGE_KEY) === 'true';
}

export function markTourSeen(): void {
  localStorage.setItem(STORAGE_KEY, 'true');
}
