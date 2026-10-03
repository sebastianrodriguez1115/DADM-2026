#!/usr/bin/env python3
"""Offline synthetic parser fixtures; NOT Android/JVM product execution evidence."""
import unittest
import xml.etree.ElementTree as ET
from evidence_helpers import jvm_counts_table, native_counts_table, select_current_timing


class EvidenceHelpersTest(unittest.TestCase):
    def setUp(self):
        self.commands = [dict(label='ImpossibleOpeningTimingTest', start='2026-10-03T16:01:19+00:00',
                              finish='2026-10-03T16:01:22+00:00')]
        self.old = '1791043270.000 111 222 I Reto4Timing: sdk=35 samples_ns=1000000,2000000,3000000,4000000,5000000 first_ms=1.000 median_ms=3.000\n'
        self.current = '1791043280.712 333 444 I Reto4Timing: sdk=35 samples_ns=6000000,7000000,8000000,9000000,10000000 first_ms=6.000 median_ms=8.000\n'

    def test_history_duplicates_and_later_record_do_not_contaminate_current(self):
        future = self.current.replace('1791043280.712', '1791043290.000')
        result = select_current_timing(self.old + self.old + self.current + future, self.commands)
        self.assertEqual(result['pid'], 333)
        self.assertEqual(result['samples_ns'], [6000000, 7000000, 8000000, 9000000, 10000000])

    def test_duplicate_in_current_interval_is_rejected(self):
        with self.assertRaises(AssertionError):
            select_current_timing(self.current + self.current, self.commands)

    def test_history_only_is_rejected_not_used_as_latest(self):
        with self.assertRaises(AssertionError):
            select_current_timing(self.old + self.old, self.commands)

    def test_duplicate_command_interval_is_rejected(self):
        with self.assertRaises(AssertionError):
            select_current_timing(self.current, self.commands * 2)

    def test_failed_native_counts_are_actual_not_expected_pass(self):
        inventory = dict(native={'example.Failing': dict(count=4), 'example.NotRun': dict(count=2)})
        summary = dict(suites=[dict(clazz='example.Failing', expected=4, passed=1, failed=1,
                                   other_results=1, verified_pass=False)])
        table = native_counts_table(inventory, summary)
        self.assertIn('| Failing | 1/4 | 1 | 1 | 1 | NO |', table)
        self.assertIn('| NotRun | 0/2 | 0 | 0 | 2 | NO |', table)
        self.assertNotIn('4/4', table)

    def test_jvm_fail_error_skip_are_subtracted_from_pass(self):
        suite = ET.fromstring('<testsuite name="example.Mixed" tests="7" failures="2" errors="1" skipped="1"/>')
        self.assertIn('| Mixed | 3/7 | 2 | 1 | 1 |', jvm_counts_table([suite]))


if __name__ == '__main__':
    unittest.main(verbosity=2)
