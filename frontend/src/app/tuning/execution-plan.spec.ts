import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ExecutionPlan } from './execution-plan';
import { TuningApi } from './tuning-api';

describe('ExecutionPlan', () => {
  const api = { explain: vi.fn() };
  beforeEach(() => {
    api.explain.mockReset();
    TestBed.configureTestingModule({
      imports: [ExecutionPlan],
      providers: [{ provide: TuningApi, useValue: api }],
    });
  });
  it('indents the hierarchy and highlights full scans', async () => {
    api.explain.mockReturnValue(
      of({
        ok: true,
        message: 'ok',
        data: [
          { id: 0, depth: 0, operation: 'SELECT STATEMENT', fullTableScan: false },
          {
            id: 1,
            depth: 1,
            operation: 'TABLE ACCESS',
            options: 'FULL',
            objectName: 'JOBS',
            fullTableScan: true,
          },
        ],
      }),
    );
    const fixture = TestBed.createComponent(ExecutionPlan);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    input.value = 'SELECT * FROM HR.JOBS';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.explain).toHaveBeenCalledWith('SELECT * FROM HR.JOBS');
    expect(fixture.nativeElement.querySelectorAll('tr.full-scan').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('(full scan)');
  });
  it('shows API errors without retaining an old plan', async () => {
    api.explain.mockReturnValue(throwError(() => new Error('ORA-00942')));
    const fixture = TestBed.createComponent(ExecutionPlan);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    input.value = 'SELECT * FROM MISSING';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'ORA-00942',
    );
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });
});
