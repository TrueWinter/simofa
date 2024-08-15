export function parseTime(ms: number): string {
  const seconds = Math.floor(ms / 1000);

  if (seconds < 60) {
    return `${seconds.toString()}s`;
  }

  const buildMinutes = Math.floor(seconds / 60);
  const buildSeconds = seconds - (buildMinutes * 60);

  return `${buildMinutes.toString()}m ${buildSeconds.toString()}s`;
}
