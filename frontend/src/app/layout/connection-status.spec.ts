import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConnectionStatus } from './connection-status';

describe('ConnectionStatus', () => {
  let fixture: ComponentFixture<ConnectionStatus>;
  let http: HttpTestingController;

  const statusText = () =>
    (fixture.nativeElement.querySelector('[role="status"]') as HTMLElement).textContent?.trim();

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ConnectionStatus);
  });

  afterEach(() => http.verify());

  it('shows a checking state until the health call answers', async () => {
    http.expectOne('/api/health');
    await fixture.whenStable();

    expect(statusText()).toBe('Checking connection');
  });

  it('shows Connected when the database is reachable', async () => {
    http
      .expectOne('/api/health')
      .flush({ ok: true, message: 'Database connection is available', data: null });
    await fixture.whenStable();

    expect(statusText()).toBe('Connected');
  });

  it('shows Disconnected when the backend is down', async () => {
    http.expectOne('/api/health').error(new ProgressEvent('error'));
    await fixture.whenStable();

    expect(statusText()).toBe('Disconnected');
  });

  it('checks again when Recheck is pressed', async () => {
    http.expectOne('/api/health').error(new ProgressEvent('error'));
    await fixture.whenStable();

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();

    http
      .expectOne('/api/health')
      .flush({ ok: true, message: 'Database connection is available', data: null });
    await fixture.whenStable();

    expect(statusText()).toBe('Connected');
  });
});
