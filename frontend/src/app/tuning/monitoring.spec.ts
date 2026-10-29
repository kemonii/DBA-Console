import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Monitoring } from './monitoring';
import { TuningApi } from './tuning-api';

describe('Monitoring', () => {
  it('requires a confirmation before terminating the exact blocking session', async () => {
    const api = {
      monitor: vi
        .fn()
        .mockImplementation((view: string) =>
          of({
            ok: true,
            message: 'ok',
            data:
              view === 'locks' ? [{ BLOCKER_SID: 42, BLOCKER_SERIAL: 123, WAITING_SID: 43 }] : [],
          }),
        ),
      kill: vi.fn().mockReturnValue(of({ ok: true, message: 'Session terminated', data: null })),
    };
    TestBed.configureTestingModule({
      imports: [Monitoring],
      providers: [{ provide: TuningApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(Monitoring);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = 'locks';
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button.danger') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(api.kill).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.confirmation').textContent).toContain('42,123');
    (
      fixture.nativeElement.querySelector('.confirmation button.danger') as HTMLButtonElement
    ).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.kill).toHaveBeenCalledExactlyOnceWith(42, 123);
    expect(fixture.nativeElement.querySelector('.confirmation')).toBeNull();
  });
});
