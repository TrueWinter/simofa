/** @type {import('eslint').ESLint.ConfigData} */
module.exports = {
  root: true,
  extends: ['airbnb', 'airbnb/hooks'],
  env: {
    browser: true
  },
  ignorePatterns: [
    '**/dist/**'
  ],
  rules: {
    'comma-dangle': 'off',
    'max-len': ['warn', {
      code: 100,
      ignoreComments: true
    }],
    'import/prefer-default-export': 'off',
    'import/no-unresolved': 'off',
    'import/extensions': 'off',
    'react/react-in-jsx-scope': 'off',
    'react/jsx-filename-extension': 'off',
    'react/jsx-props-no-spreading': 'off',
    'react/jsx-closing-bracket-location': 'off',
    'react/jsx-first-prop-new-line': 'off',
    'react/jsx-max-props-per-line': 'off',
    'react-hooks/exhaustive-deps': 'off',
    'react/jsx-one-expression-per-line': 'off',
    'react/jsx-closing-tag-location': 'off',
    'react/destructuring-assignment': 'off',
    'react/require-default-props': 'off',
    'react/jsx-no-bind': 'off',
    'react/prop-types': 'off',
    'operator-linebreak': 'off',
    'object-curly-newline': 'off',
    'no-restricted-globals': 'off',
    'function-paren-newline': 'off',
    'consistent-return': 'off',
    'no-shadow': ['error', {
      allow: ['e', 'i']
    }],
    'no-nested-ternary': 'off'
  },
  plugins: [
    '@typescript-eslint'
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    sourceType: 'module',
    tsconfigRootDir: __dirname
  }
};
