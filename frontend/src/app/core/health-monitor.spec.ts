import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HealthMonitor } from './health-monitor';

describe('HealthMonitor', () => {
  let monitor: HealthMonitor;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    monitor = TestBed.inject(HealthMonitor);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts in the checking state', () => {
    expect(monitor.state()).toBe('checking');
  });

  it('is connected when the health endpoint reports ok', () => {
    monitor.check();
    http
      .expectOne('/api/health')
      .flush({ ok: true, message: 'Database connection is available', data: null });

    expect(monitor.state()).toBe('connected');
    expect(monitor.detail()).toBe('Database connection is available');
  });

  it('is disconnected with the backend message when the database is unreachable', () => {
    monitor.check();
    http
      .expectOne('/api/health')
      .flush(
        { ok: false, message: 'ORA-12541: no listener', data: null },
        { status: 500, statusText: 'Server Error' },
      );

    expect(monitor.state()).toBe('disconnected');
    expect(monitor.detail()).toBe('ORA-12541: no listener');
  });

  it('is disconnected when the backend is down', () => {
    monitor.check();
    http.expectOne('/api/health').error(new ProgressEvent('error'));

    expect(monitor.state()).toBe('disconnected');
  });
});
