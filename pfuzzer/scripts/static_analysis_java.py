#!/usr/bin/env python3
"""Run static analysis on the java sources of the project.

Usage: python3 static_analysis_java.py <project root>
"""

import sys
import os.path
import subprocess

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: {} <project root>".format(sys.argv[0]))
        sys.exit(1)

    pmd_path = '{}/tools/pmd/bin/run.sh'.format(sys.argv[1])
    pmd_rule = '{}/tools/pmd.xml'.format(sys.argv[1])

    if not os.path.isfile(pmd_path):
        print("warning: skipping java static analysis: pmd not found")
        sys.exit(0)

    result = subprocess.run(['bash', pmd_path, 'pmd', '-R', pmd_rule, '-d', sys.argv[1]])
    sys.exit(result.returncode)
