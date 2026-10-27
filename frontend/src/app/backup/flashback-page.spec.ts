import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FlashbackPage } from './flashback-page';

describe('FlashbackPage', () => {
  let fixture: ComponentFixture<FlashbackPage>;
  let http: HttpTestingController;
  let root: HTMLElement;

  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };

  async function type(selector: string, value: string) {
    const element = root.querySelector(selector) as HTMLInputElement;
    element.value = value;
    element.dispatchEvent(new Event('input'));
    element.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  async function click(selector: string) {
    (root.querySelector(selector) as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  async function fillRows() {
    await type('#rows-schema', 'hr');
    await type('#rows-table', 'employees');
    await type('#rows-timestamp', '2026-10-26T10:15');
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FlashbackPage);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('restores a dropped table', async () => {
    await type('#table-schema', 'hr');
    await type('#table-table', 'employees');

    await click('.restore-table button[type="submit"]');
    const req = http.expectOne('/api/flashback/table');
    expect(req.request.body).toEqual({ schema: 'hr', table: 'employees' });
    req.flush({ ok: true, message: 'Table HR.EMPLOYEES restored from the recycle bin', data: null });
    await settle();

    expect(root.querySelector('.restore-table .success')?.textContent).toContain('restored from the recycle bin');
  });

  it('shows why a table could not be restored', async () => {
    await type('#table-schema', 'hr');
    await type('#table-table', 'employees');

    await click('.restore-table button[type="submit"]');
    http
      .expectOne('/api/flashback/table')
      .flush(
        { ok: false, message: 'Table HR.EMPLOYEES is not in the recycle bin', data: null },
        { status: 400, statusText: 'Bad Request' },
      );
    await settle();

    expect(root.querySelector('.restore-table .error')?.textContent).toContain('not in the recycle bin');
  });

  it('previews the rows as of a moment and lists them in a table', async () => {
    await fillRows();

    await click('.rows button.preview');
    const req = http.expectOne('/api/flashback/query');
    expect(req.request.body).toEqual({ schema: 'hr', table: 'employees', timestamp: '2026-10-26T10:15:00' });
    req.flush({
      ok: true,
      message: 'Rows as of 2026-10-26T10:15',
      data: [
        { ID: 1, NAME: 'Ann' },
        { ID: 2, NAME: 'Bob' },
      ],
    });
    await settle();

    const headers = Array.from(root.querySelectorAll('.rows-result th')).map((th) => th.textContent?.trim());
    expect(headers).toEqual(['ID', 'NAME']);
    expect(root.querySelectorAll('.rows-result tbody tr').length).toBe(2);
    expect(root.querySelector('.rows-result tbody')?.textContent).toContain('Bob');
  });

  it('says so when there are no rows to show', async () => {
    await fillRows();

    await click('.rows button.preview');
    http.expectOne('/api/flashback/query').flush({ ok: true, message: 'Rows as of ...', data: [] });
    await settle();

    expect(root.querySelector('.rows-result')?.textContent).toContain('No rows');
  });

  it('restores the missing rows and reports how many', async () => {
    await fillRows();

    await click('.rows button.restore');
    const req = http.expectOne('/api/flashback/restore');
    expect(req.request.body).toEqual({ schema: 'hr', table: 'employees', timestamp: '2026-10-26T10:15:00' });
    req.flush({ ok: true, message: '3 row(s) restored', data: 3 });
    await settle();

    expect(root.querySelector('.rows .success')?.textContent).toContain('3 row(s) restored');
  });

  it('does not call the API while required fields are missing', async () => {
    await type('#rows-schema', 'hr');

    await click('.rows button.preview');
    await click('.restore-table button[type="submit"]');

    http.expectNone('/api/flashback/query');
    http.expectNone('/api/flashback/table');
    expect(root.querySelector('.rows .error')?.textContent).toContain('required');
    expect(root.querySelector('.restore-table .error')?.textContent).toContain('required');
  });
});
