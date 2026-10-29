import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { StatisticsIndexes } from './statistics-indexes';
import { TuningApi } from './tuning-api';

describe('StatisticsIndexes', () => {
  it('uses the loaded row owner and requires confirmation to drop an index', async () => {
    const api = {
      statistics: vi.fn().mockReturnValue(of({ ok: true, message: 'ok', data: [] })),
      indexes: vi
        .fn()
        .mockReturnValue(
          of({
            ok: true,
            message: 'ok',
            data: [{ OWNER: 'HR', INDEX_NAME: 'IDX', TABLE_NAME: 'JOBS' }],
          }),
        ),
      drop: vi.fn().mockReturnValue(of({ ok: true, message: 'Index dropped', data: null })),
    };
    TestBed.configureTestingModule({
      imports: [StatisticsIndexes],
      providers: [{ provide: TuningApi, useValue: api }],
    });
    const fixture = TestBed.createComponent(StatisticsIndexes);
    fixture.detectChanges();
    const schema = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    schema.value = 'HR';
    schema.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector('form')
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
    fixture.detectChanges();
    schema.value = 'OTHER';
    schema.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.index-row button.danger') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(api.drop).not.toHaveBeenCalled();
    (
      fixture.nativeElement.querySelector('.confirmation button.danger') as HTMLButtonElement
    ).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.drop).toHaveBeenCalledExactlyOnceWith('HR', 'IDX');
  });
});
