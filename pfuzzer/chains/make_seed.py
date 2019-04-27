# coding=utf-8
# !/usr/bin/env python3
import os
import random
import shutil
import subprocess
from ast import literal_eval


def run_loop(parentdir):
    """
    Runs the program under test several times with different inputs inferred thorugh the tainting data gained of previous
    program runs.
    :param parentdir: The folder in which the program under test lies in.
    :param arg: The flag to indicate if the value is given to the program as arg or via stdin (true for as arg)
    :param dfs: The search strategy, by default bfs is used (dfs is false).
    :return:
    """
    os.mkdir("seed")
    counter = 0
    with open(os.path.join(parentdir, "valid_inputs.txt"), "r") as valid_inputs_file:
        for line in valid_inputs_file:
            if line.startswith("'"):
                line = literal_eval(line.strip())
                with open("seed/tmp%d.js" % counter, "w") as tmpfile:
                    tmpfile.write(line)
            counter += 1


if __name__ == '__main__':
    import argparse

    rseed = int(os.environ.get('RSEED', '0'))
    random.seed(rseed)
    parser = argparse.ArgumentParser(description="Pychains")
    parser.add_argument('-f', "--file", metavar="subject", type=str,
                        help="The path to the valid_inputs.txt file (e.g. /home/valid_inputs.txt).", required=True)
    args = parser.parse_args()
    subject = os.path.abspath(args.file)
    g_parentdir = os.path.abspath(os.path.join(subject, os.pardir))
    os.chdir(g_parentdir)

    run_loop(g_parentdir)
