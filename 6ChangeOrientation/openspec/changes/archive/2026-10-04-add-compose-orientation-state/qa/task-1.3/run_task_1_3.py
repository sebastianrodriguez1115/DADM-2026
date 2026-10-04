#!/usr/bin/env python3
"""Run the established guarded full Android suite with isolated task 1.3 evidence."""
import importlib.util
import re
import sys
from pathlib import Path

if len(sys.argv) != 2 or not re.fullmatch(r"[A-Za-z0-9_-]+", sys.argv[1]):
    raise SystemExit("Usage: run_task_1_3.py <new-simple-evidence-directory>")

run_name = sys.argv[1]
evidence_root = Path(__file__).resolve().parent
output = evidence_root / run_name
if output.exists():
    raise SystemExit(f"Refusing to overwrite existing evidence: {output}")

runner = Path(__file__).resolve().parents[1] / "task-1.2" / "run_instrumentation.py"
sys.argv = [str(runner)]
spec = importlib.util.spec_from_file_location("guarded_task_1_2_runner", runner)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
module.QA_ROOT = evidence_root
module.QA = output
module.QA.mkdir(parents=True, exist_ok=False)
sys.exit(module.main())
