const UNITS = ['B', 'KB', 'MB', 'GB', 'TB'];

/** Formats a size in bytes for display, for example `1536` becomes `1.5 KB`. */
export function formatSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < UNITS.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${value.toFixed(1)} ${UNITS[unit]}`;
}
