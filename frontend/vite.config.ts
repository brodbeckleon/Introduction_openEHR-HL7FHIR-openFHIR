import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

// This file runs in Node, where `process` exists but the browser typings used for src/ do not
// describe it. Declaring the one property used here keeps `npm run check` passing without pulling
// in @types/node for a single lookup.
declare const process: { env: Record<string, string | undefined> };

// Overridable so the dev server can reach the backend when it isn't running on
// localhost, e.g. host.docker.internal when the frontend runs in a container.
const backendUrl = process.env.VITE_BACKEND_URL ?? 'http://localhost:18080';

export default defineConfig({
  plugins: [svelte()],
  server: {
    port: 5173,
    // Keeps the browser on one origin: /api and /fhir go to the Gradle backend.
    proxy: {
      '/api': backendUrl,
      '/fhir': backendUrl,
    },
  },
});
