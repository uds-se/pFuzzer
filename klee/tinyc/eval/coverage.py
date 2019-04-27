# coding=utf-8
# !/usr/bin/env python3
import os
import random
import shutil
import subprocess
from ast import literal_eval
import metric


def run_and_trace(inpt: str, as_arg: bool):
    """
    Executes the C code and runs the tracing on it.
    :param inpt: The input to use to run the program.
    :param as_arg: Defines if the input is given as argument or via stdin.
    """
    global po
    run_subject = "%s.cov" % subject

    print(repr(inpt) + "\n")
    try:
        if as_arg:
            subprocess.call([run_subject, "-e", inpt], timeout=10)
        else:
            ps = subprocess.Popen(('printf', '%s', inpt), stdout=subprocess.PIPE)
            subprocess.call(run_subject, timeout=10, stdin=ps.stdout)
    except subprocess.TimeoutExpired:
        print("Timeout: %s" % repr(inpt))
        pass


def run_loop(parentdir, arg: bool, subject: str):
    """
    Runs the program under test several times with different inputs inferred thorugh the tainting data gained of previous
    program runs.
    :param parentdir: The folder in which the program under test lies in.
    :param arg: The flag to indicate if the value is given to the program as arg or via stdin (true for as arg)
    :param dfs: The search strategy, by default bfs is used (dfs is false).
    :return:
    """
    valid_inputs = list()
    with open(os.path.join(parentdir, "valid_inputs.txt"), "r") as valid_inputs_file:
        cov_info = None
        valid_cov_increase_counter = 0
        for line in valid_inputs_file:
            if line.startswith("test"):
                command = "ktest-tool --trim-zeros ../klee-out-0/" + line[:-1]
                klee_out = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE).stdout.read()
                # print(klee_out)
                try:
                    klee_out = klee_out.decode("utf-8")
                    if "0: data: \'" in klee_out:
                        klee_out = klee_out.split("data: \'")[1].split("\'\nobject")[0]
                    elif "0: data: \"" in klee_out:
                        klee_out = klee_out.split("data: \"")[1].split("\"\nobject")[0]
                    else:
                        raise ValueError("Cannot be parsed: %s" % str(f))
                except Exception as e:
                    # sometimes the file is empty, then no test was created? we will ignore those cases
                    print(e)
                    continue
                # print(klee_out)
                input = bytes(klee_out.split("\\x00")[0], "ascii").decode("unicode-escape")
                valid_inputs.append(input)
                run_and_trace(input, arg)
            tmp_cov_info = subprocess.check_output(["gcov", "-c", "-b", subject], encoding="utf-8")
            tmp_cov_info = "\n" + "".join([line for line in tmp_cov_info.split("\n") if line.startswith("Taken")])
            print(tmp_cov_info+"\n")
            if tmp_cov_info != cov_info:
                valid_cov_increase_counter += 1
                cov_info = tmp_cov_info
    print("\nValid inputs that increase coverage: %d\n" % valid_cov_increase_counter)
    os.system("gcovr -r . --branches --html --html-details -o coverage.html")
    for key, val in metric.count_regex(os.path.basename(subject), valid_inputs).items():
        if val > 0:
            print("\n\t%s: %s" % (key, val))
    # os.system("grep '#####' *.gcov")


if __name__ == '__main__':
    import argparse

    rseed = int(os.environ.get('RSEED', '0'))
    random.seed(rseed)
    parser = argparse.ArgumentParser(description="Pychains")
    parser.add_argument('-p', "--program", metavar="subject", type=str,
                        help="The path to the subject as c file to run on.", required=True)
    parser.add_argument('-a', "--arg", metavar="arg", default="False", type=str,
                        help="Defines if the input is given as argument or via stdin.", required=True)
    args = parser.parse_args()
    subject = os.path.abspath(args.program)
    g_arg = args.arg.lower() == "true"
    g_parentdir = os.path.abspath(os.path.join(subject, os.pardir))
    os.chdir(g_parentdir)
    os.system("rm *.gcda *.gcno *.cov *.gcov")

    # instrument program
    os.system("gcc -fprofile-arcs -ftest-coverage %s -o %s.cov -ldl" % (subject, subject))

    run_loop(g_parentdir, g_arg, subject)

