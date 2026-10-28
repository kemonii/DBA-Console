import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SECURITY_ROUTES } from './security.routes';

describe('Security routes', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'security', children: SECURITY_ROUTES }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  it('redirects /security to the tablespaces screen', async () => {
    await harness.navigateByUrl('/security');
    await settle();
    http.expectOne('/api/security/tablespaces').flush({ ok: true, message: 'ok', data: [] });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/security/tablespaces');
    expect((harness.fixture.nativeElement as HTMLElement).textContent).toContain(
      'Create a tablespace',
    );
  });

  it('shows the sub-navigation', async () => {
    await harness.navigateByUrl('/security/tablespaces');
    await settle();
    http.expectOne('/api/security/tablespaces').flush({ ok: true, message: 'ok', data: [] });
    await settle();

    const links = Array.from(
      (harness.fixture.nativeElement as HTMLElement).querySelectorAll('nav a'),
    ).map((a) => a.textContent?.trim());
    expect(links).toEqual(['Tablespaces', 'Users', 'Roles & privileges', 'Profiles', 'Auditing']);
  });
});
