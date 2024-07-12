export function shortUuid(uuid: string) {
  // v7 UUIDs are only random at the end
  return uuid.split('-').reverse()[0];
}