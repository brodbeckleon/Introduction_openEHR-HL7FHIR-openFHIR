/**
 * Which patient the page is showing.
 *
 * Every request that reads or writes clinical data names one, and the backend turns it into an EHR
 * id by asking openEHR — this app no longer carries a record id of its own. The choice is a rune,
 * so switching patients re-renders whatever reads it, and it is remembered across reloads the same
 * way the language is.
 */
import type { PatientSummary } from './types';

const STORAGE_KEY = 'hrm.patient';

let current = $state<string | null>(localStorage.getItem(STORAGE_KEY));
let known = $state<PatientSummary[]>([]);

/** Null until the directory has loaded; requests then omit the parameter and get the default. */
export function patient(): string | null {
  return current;
}

export function patients(): PatientSummary[] {
  return known;
}

export function setPatient(next: string): void {
  current = next;
  localStorage.setItem(STORAGE_KEY, next);
}

/**
 * Takes the directory the backend answered with.
 *
 * A remembered patient that is no longer configured would otherwise keep producing 404s, so it is
 * dropped in favour of the first one the backend offers.
 */
export function rememberPatients(list: PatientSummary[]): void {
  known = list;
  const first = list[0];
  if (!first) return;
  if (!current || !list.some((entry) => entry.id === current)) {
    setPatient(first.id);
  }
}
