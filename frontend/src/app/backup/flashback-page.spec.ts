import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BackupApiService } from './backup-api.service';
import { FlashbackPage } from './flashback-page';

describe('FlashbackPage', () => {
  const api = { flashbackTable: vi.fn(), flashbackQuery: vi.fn(), flashbackRestore: vi.fn() };
  let fixture: ComponentFixture<FlashbackPage>;
  let page: HTMLElement;

  function type(selector: string, value: string) {
    const element = page.querySelector(selector) as HTMLInputElement;
    element.value = value;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  async function click(selector: string) {
    (page.querySelector(selector) as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    vi.resetAllMocks();
    await TestBed.configureTestingModule({
      imports: [FlashbackPage],
      providers: [{ provide: BackupApiService, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(FlashbackPage);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  it('restores a dropped table', async () => {
    api.flashbackTable.mockReturnValue(
      of({ ok: true, message: 'Table HR.EMPLOYEES restored from the recycle bin', data: null }),
    );
    type('.restore-table [name="schema"]', 'hr');
    type('.restore-table [name="table"]', 'employees');

    await click('.restore-table button[type="submit"]');

    expect(api.flashbackTable).toHaveBeenCalledWith({ schema: 'hr', table: 'employees' });
    expect(page.querySelector('.restore-table .success')?.textContent).toContain('restored from the recycle bin');
  });

  it('shows why a table could not be restored', async () => {
    api.flashbackTable.mockReturnValue(
      of({ ok: false, message: 'Table HR.EMPLOYEES is not in the recycle bin', data: null }),
    );
    type('.restore-table [name="schema"]', 'hr');
    type('.restore-table [name="table"]', 'employees');

    await click('.restore-table button[type="submit"]');

    expect(page.querySelector('.restore-table .error')?.textContent).toContain('not in the recycle bin');
  });

  it('previews the rows as of a moment and lists them in a table', async () => {
    api.flashbackQuery.mockReturnValue(
      of({ ok: true, message: 'Rows as of 2026-10-26T10:15', data: [{ ID: 1, NAME: 'Ann' }, { ID: 2, NAME: 'Bob' }] }),
    );
    type('.rows [name="schema"]', 'hr');
    type('.rows [name="table"]', 'employees');
    type('.rows [name="timestamp"]', '2026-10-26T10:15');

    await click('.rows button.preview');

    expect(api.flashbackQuery).toHaveBeenCalledWith({
      schema: 'hr',
      table: 'employees',
      timestamp: '2026-10-26T10:15:00',
    });
    const headers = Array.from(page.querySelectorAll('.rows-result th')).map((th) => th.textContent?.trim());
    expect(headers).toEqual(['ID', 'NAME']);
    expect(page.querySelectorAll('.rows-result tbody tr').length).toBe(2);
    expect(page.querySelector('.rows-result tbody')?.textContent).toContain('Bob');
  });

  it('says so when there are no rows to show', async () => {
    api.flashbackQuery.mockReturnValue(of({ ok: true, message: 'Rows as of ...', data: [] }));
    type('.rows [name="schema"]', 'hr');
    type('.rows [name="table"]', 'employees');
    type('.rows [name="timestamp"]', '2026-10-26T10:15');

    await click('.rows button.preview');

    expect(page.querySelector('.rows-result')?.textContent).toContain('No rows');
  });

  it('restores the missing rows and reports how many', async () => {
    api.flashbackRestore.mockReturnValue(of({ ok: true, message: '3 row(s) restored', data: 3 }));
    type('.rows [name="schema"]', 'hr');
    type('.rows [name="table"]', 'employees');
    type('.rows [name="timestamp"]', '2026-10-26T10:15');

    await click('.rows button.restore');

    expect(api.flashbackRestore).toHaveBeenCalledWith({
      schema: 'hr',
      table: 'employees',
      timestamp: '2026-10-26T10:15:00',
    });
    expect(page.querySelector('.rows .success')?.textContent).toContain('3 row(s) restored');
  });

  it('does not call the API while required fields are missing', async () => {
    type('.rows [name="schema"]', 'hr');

    await click('.rows button.preview');
    await click('.restore-table button[type="submit"]');

    expect(api.flashbackQuery).not.toHaveBeenCalled();
    expect(api.flashbackTable).not.toHaveBeenCalled();
    expect(page.querySelector('.rows .error')?.textContent).toContain('required');
    expect(page.querySelector('.restore-table .error')?.textContent).toContain('required');
  });
});
