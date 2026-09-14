import js from '@eslint/js';
import globals from 'globals';
import typescript from 'typescript-eslint';
import svelte from 'eslint-plugin-svelte';
import prettier from 'eslint-config-prettier';

export default typescript.config(
  js.configs.recommended,
  ...typescript.configs.recommended,
  ...svelte.configs['flat/recommended'],
  prettier,
  ...svelte.configs['flat/prettier'],
  {
    languageOptions: {
      globals: { ...globals.browser },
    },
  },
  {
    // .svelte.ts holds runes in a plain module, so it needs the Svelte parser too — the TypeScript
    // parser alone trips over `$state` at the top level.
    files: ['**/*.svelte', '**/*.svelte.ts'],
    languageOptions: {
      parserOptions: { parser: typescript.parser },
    },
  },
  {
    ignores: ['dist/', 'node_modules/', '.svelte-kit/'],
  },
);
