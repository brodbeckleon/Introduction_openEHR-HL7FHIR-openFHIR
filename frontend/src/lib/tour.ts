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
  tab: 'overview' | 'pipeline' | 'traffic' | 'aql' | 'mappings' | 'template';
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
  { tab: 'overview', key: 'tour.number', focus: '.stats' },
  { tab: 'overview', key: 'tour.column', focus: '.explainer' },
  { tab: 'pipeline', key: 'tour.fhir', stage: 'observation', focus: '.panels' },
  { tab: 'pipeline', key: 'tour.openehr', stage: 'composition', focus: '.panels' },
  { tab: 'pipeline', key: 'tour.lost', stage: 'roundtrip', focus: '.differences' },
  { tab: 'mappings', key: 'tour.translate', focus: '.mapping' },
  { tab: 'overview', key: 'tour.versions', focus: '.manual' },
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
