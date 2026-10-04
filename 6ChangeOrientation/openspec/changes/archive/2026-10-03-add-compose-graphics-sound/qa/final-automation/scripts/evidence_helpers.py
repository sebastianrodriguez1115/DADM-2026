"""Pure offline parsers/renderers; synthetic fixtures are not execution evidence."""
from datetime import datetime
import re


def select_current_timing(logcat, commands):
    runs = [c for c in commands if c['label'] == 'ImpossibleOpeningTimingTest']
    assert len(runs) == 1, 'Require exactly one timing test command in this run'
    run = runs[0]
    start = datetime.fromisoformat(run['start']).timestamp()
    finish = datetime.fromisoformat(run['finish']).timestamp()
    assert start < finish, 'Invalid timing command interval'
    pattern = re.compile(
        r'^\s*(\d+\.\d+)\s+(\d+)\s+\d+\s+I\s+Reto4Timing:\s+'
        r'sdk=35 samples_ns=([0-9,]+) first_ms=([0-9.]+) median_ms=([0-9.]+)\s*$', re.M)
    current = [m for m in pattern.finditer(logcat) if start <= float(m[1]) <= finish]
    # No latest-record fallback or tolerance: ambiguity/clock skew fails QA postprocessing.
    assert len(current) == 1, 'Require one timing log inside this command interval (check ambiguity/clock alignment)'
    m = current[0]
    ns = [int(x) for x in m[3].split(',')]
    assert len(ns) == 5, 'Require exactly five timing samples'
    assert float(m[4]) == round(ns[0] / 1e6, 3)
    assert float(m[5]) == round(sorted(ns)[2] / 1e6, 3)
    return dict(samples_ns=ns, log_epoch=float(m[1]), pid=int(m[2]),
                command_start=run['start'], command_finish=run['finish'],
                selection='Single epoch-tagged record within current timing command start/finish; no latest fallback')


def native_counts_table(inventory, summary):
    suites = summary['suites']
    indexed = {s['clazz']: s for s in suites}
    assert len(indexed) == len(suites), 'Duplicate native suite results'
    assert set(indexed) <= set(inventory['native']), 'Unexpected native suite results'
    table = ['| Clase instrumentada | PASS / esperado | Fallos | Otros/omitidos | Sin resultado | Verificado |',
             '|---|---:|---:|---:|---:|---|']
    for clazz, data in inventory['native'].items():
        suite = indexed.get(clazz, {})
        expected = data['count']
        assert suite.get('expected', expected) == expected, 'Native expected count mismatch'
        passed, failed, other = (suite.get(k, 0) for k in ('passed', 'failed', 'other_results'))
        assert 0 <= passed + failed + other <= expected, 'Invalid native result counts'
        missing = expected - passed - failed - other
        verified = 'sí' if suite.get('verified_pass', False) else 'NO'
        table.append(f"| {clazz.split('.')[-1]} | {passed}/{expected} | {failed} | {other} | {missing} | {verified} |")
    return '\n'.join(table) + '\n'


def jvm_counts_table(suites):
    table = ['| Clase JVM | PASS / total | Fallos | Errores | Omitidos |', '|---|---:|---:|---:|---:|']
    for suite in suites:
        total, failures, errors, skipped = (int(suite.get(k, 0)) for k in ('tests', 'failures', 'errors', 'skipped'))
        passed = total - failures - errors - skipped
        assert passed >= 0, 'Invalid JVM result counts'
        table.append(f"| {suite.get('name').split('.')[-1]} | {passed}/{total} | {failures} | {errors} | {skipped} |")
    return '\n'.join(table) + '\n'
