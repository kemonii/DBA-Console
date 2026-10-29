import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    });
  });

  it('renders a navigation link for every module', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const links = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('nav a'),
      (a) => a.textContent?.trim(),
    );
    expect(links).toEqual(['Respaldos', 'Tunning', 'Seguridad', 'Archivos', 'Ejemplo']);
  });

  it.each([
    ['/backup', 'Respaldos y recuperación'],
    ['/tuning', 'Tunning de consultas y performance'],
    ['/security', 'Seguridad'],
    ['/files', 'Administración de archivos'],
    ['/example', 'Pantalla de ejemplo'],
  ])('opens %s', async (url, heading) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(heading);
  });

  it('redirects the root path to the backup module', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');

    expect(TestBed.inject(Router).url).toBe('/backup');
  });
});
