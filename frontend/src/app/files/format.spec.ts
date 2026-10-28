import { formatSize } from './format';

describe('formatSize', () => {
  it('shows bytes below 1 KB', () => {
    expect(formatSize(0)).toBe('0 B');
    expect(formatSize(1023)).toBe('1023 B');
  });

  it('shows KB and MB with one decimal', () => {
    expect(formatSize(1024)).toBe('1.0 KB');
    expect(formatSize(1536)).toBe('1.5 KB');
    expect(formatSize(5 * 1024 * 1024)).toBe('5.0 MB');
  });

  it('shows GB for very large files', () => {
    expect(formatSize(3 * 1024 ** 3)).toBe('3.0 GB');
  });
});
