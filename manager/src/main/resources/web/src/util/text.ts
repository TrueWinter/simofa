export function upperCaseFirstLetter(str: string) {
  const firstLetter = str.charAt(0).toUpperCase();
  const restOfWord = str.slice(1, str.length);

  return firstLetter + restOfWord;
}

export function getCommitSubject(commit: string) {
  return commit.split('\n')[0];
} 
