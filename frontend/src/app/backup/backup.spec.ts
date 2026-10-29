import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Backup } from './backup';

describe('Backup', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  it('titles the module and links to its screens', async () => {
    const fixture = TestBed.createComponent(Backup);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('h1')?.textContent).toContain('Respaldos y recuperación');
    const links = Array.from(root.querySelectorAll('nav a')).map((a) => a.textContent?.trim());
    expect(links).toEqual(['Respaldo', 'Recuperación', 'Flashback']);
  });

  it('has a place for the routed screens', async () => {
    const fixture = TestBed.createComponent(Backup);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('router-outlet')).not.toBeNull();
  });
});
