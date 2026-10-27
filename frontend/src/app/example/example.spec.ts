import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Example } from './example';

describe('Example', () => {
  let fixture: ComponentFixture<Example>;
  let http: HttpTestingController;
  let root: HTMLElement;

  const input = () => root.querySelector('input') as HTMLInputElement;
  // The submit action resolves through promises, so let them settle before rendering.
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };
  const send = () => (root.querySelector('button[type="submit"]') as HTMLButtonElement).click();

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Example);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('sends the default path and shows the OperationResult', async () => {
    send();
    await fixture.whenStable();

    const req = http.expectOne('/api/health');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'Database connection is available', data: null });
    await settle();

    const result = root.querySelector('.result') as HTMLElement;
    expect(result.textContent).toContain('Database connection is available');
    expect(root.querySelector('.error')).toBeNull();
  });

  it('shows the backend message when the request fails', async () => {
    send();
    await fixture.whenStable();

    http
      .expectOne('/api/health')
      .flush(
        { ok: false, message: 'ORA-12541: no listener', data: null },
        { status: 500, statusText: 'Server Error' },
      );
    await settle();

    expect(root.querySelector('.error')?.textContent).toContain('ORA-12541: no listener');
    expect(root.querySelector('.result')).toBeNull();
  });

  it('does not call the backend when the path is empty', async () => {
    input().value = '';
    input().dispatchEvent(new Event('input'));
    await fixture.whenStable();

    send();
    await fixture.whenStable();

    http.expectNone('/api/');
    expect(root.querySelector('.error')?.textContent).toContain('Enter an endpoint path');
  });
});
