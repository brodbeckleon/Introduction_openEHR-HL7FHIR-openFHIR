package com.example.heartrate.trace;

/**
 * One FHIR Connect file from {@code openfhir-bootstrap/}, shipped to the UI verbatim.
 *
 * @param edited whether it still matches the copy the application was built with
 */
public record MappingSource(String file, String title, String content, boolean edited) {}
