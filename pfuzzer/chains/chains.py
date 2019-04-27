# coding=utf-8
# !/usr/bin/env python3
"""
The main script of pFuzzer.
"""
import os
import random
import shutil
import subprocess
import time

import core.PriorityHandling as PriorityHandling
import core.Utils as Utils
from core.InputWrapper import InputWrapper
from core.SubstitutionExtractor import get_substitution


def run_and_trace(parentdir, inpt: str, as_arg: bool, trace: bool = True) -> int:
    """
    Executes the C code and runs the tracing on it.
    :param parentdir: The directory in which the program under test lies in.
    :param inpt: The input to use to run the program.
    :param as_arg: Defines if the input is given as argument or via stdin.
    :param trace: Check if tracing should be performed or running only
    """
    global po
    if trace:
        run_subject = "%s.instrumented" % Utils.subject
    else:
        run_subject = "%s.uninstrumented" % Utils.subject

    if as_arg:
        try:
            exit_code = subprocess.call([run_subject, Utils.g_flag, inpt], timeout=10)
        except subprocess.TimeoutExpired:
            exit_code = 0
    else:
        ps = subprocess.Popen(('printf', '%s', inpt), stdout=subprocess.PIPE)
        try:
            exit_code = subprocess.call(run_subject, timeout=10, stdin=ps.stdout)
        except subprocess.TimeoutExpired:
            print("Timed out.")
            exit_code = 0

    if trace:
        output = os.path.join(parentdir, "output")
        if os.path.exists("output"):
            shutil.move("output", output)
            outputgz = os.path.join(parentdir, "output.gz")
            os.system("rm -f %s && gzip %s" % (outputgz, output))
            me = os.path.join(parentdir, "metadata")
            po = os.path.join(parentdir, "pyg.txt")
            t = os.path.join(parentdir, "output.gz")
            os.system("%s/../install/bin/trace-taint -me %s -po %s -t %s" % (Utils.cwd, me, po, t))
        else:
            pass

    if exit_code == 0:
        if not trace:
            # run again with tracing if tracing was not done
            run_and_trace(parentdir, inpt, as_arg)

    return exit_code


def run_loop(parentdir, arg: bool, dfs: bool = False):
    """
    Runs the program under test several times with different inputs inferred through the tainting data gained of previous
    program runs.
    :param parentdir: The folder in which the program under test lies in.
    :param arg: The flag to indicate if the value is given to the program as arg or via stdin (true for as arg)
    :param dfs: The search strategy, by default bfs is used (dfs is false).
    :return:
    """
    from core.qfuzzer import Predictor, Executor
    # save time when execution started to later report when the inputs were generated
    starttime = time.time()
    # set of already generated inputs
    already_generated = set()
    # run program with starting random input
    # inp = '{"":-X'
    # inp = "{aaaaaa"
    inp = random.choice(Utils.continuations)
    inp_rand_new = inp
    already_generated.add(inp)
    Utils.all_exec = 1
    # the counter for the generated inputs
    inpt_counter = 0
    # hold a list of possible substitutions, take always the first element of the list and use this for continuation
    parent_input = None
    # predictor = Predictor(Executor(parentdir))
    # var to define if qfuzzer successfully found something
    success = True
    with open(os.path.join(parentdir, "valid_inputs.txt"), "w") as valid_inputs_file:
        with open(os.path.join(parentdir, "crashes.txt"), "w") as error_inputs_file:
            with open(os.path.join(parentdir, "stats.txt"), "w") as Utils.stats_file:
                exit_code = run_and_trace(parentdir, inp, arg)
                (h_value, new_covered, values, knowledge, all_covered) = get_substitution(os.path.join(parentdir, "pyg.txt"),
                                                                             inp_rand_new,
                                                                             parent_input.prio_value.stack if parent_input else [],
                                                                             dfs)
                Utils.current_iteration = 0
                while True:
                    # keep track of how many input are executed since the last valid input
                    # check if the previous input was accepted or the input is larger than X chars, if so restart
                    # if h_value.cover_counter[0] > 0 and success and exit_code == 1:
                    #     (h_value, new_covered, values, knowledge, all_covered, inp_rand_new, success) = predictor.process(inp_rand_new[:-1])
                    #     if success:
                    #         exit_code = 0
                    #     else:
                    #         exit_code = 1
                    if not exit_code == 0 and len(inp) < 200:
                        # in this case some special error occurred and we add the value to the error file
                        if exit_code != 1:
                            error_inputs_file.write("Time used until input was generated: %f. Crash with exit code %d\n" % (time.time() - starttime, exit_code))
                            error_inputs_file.write(repr(inp_rand_new) + "\n\n")
                            error_inputs_file.flush()
                        print("\n\nH_Value parent: %s" % str(parent_input.prio_value if parent_input else "None"))
                        print("\nH_Value executed: %s\n\tfrom: %s\n\t#new values:%d\n\n" % (str(h_value), repr(inp_rand_new), len(values)))
                        inpt_counter = PriorityHandling.add_values_to_prioqueue(h_value, inpt_counter, new_covered, values, parent_input)
                    elif exit_code == 0:
                        success = True
                        # if successful restart but do not consider
                        print("\n\nH_Value parent: %s" % str(parent_input.prio_value if parent_input else "None"))
                        print("\nH_Value executed: %s\n\tfrom: %s\n\t#new values:%d\n\n" % (str(h_value), repr(inp_rand_new), len(values)))
                        # we check if a new branch was covered and only report those inputs, otw. just go on with the search
                        # this lowers quantity a lot but improves quality significantly
                        if all_covered.issubset(Utils.valid_covered.keys()):
                            # combine covered lines of valid inputs with the ones covered by the current input to make sure that the
                            # heuristic value gets better only if new branches are covered
                            inpt_counter = PriorityHandling.add_values_to_prioqueue(h_value, inpt_counter, new_covered, values, parent_input)
                        else:
                            # if the input was valid and covered new branches, we report it and restart the search but this time only inputs that cover branches that were not
                            # covered by valid inputs get a better heuristic value
                            valid_inputs_file.write("Time used until input was generated: %f\n" % (time.time() - starttime))
                            valid_inputs_file.write(repr(inp_rand_new) + "\n\n")
                            valid_inputs_file.flush()
                            # store which branches are covered additionally by the valid inputs
                            for key in all_covered:
                                if key in Utils.valid_covered:
                                    Utils.valid_covered[key] += 1
                                else:
                                    Utils.valid_covered[key] = 1
                            # set the currently covered branches to the ones covered by the valid inputs, hence the new heuristic value is based on the
                            # branches not seen before for the next round
                            Utils.valid_found.add(inp_rand_new)
                            inpt_counter = PriorityHandling.add_values_to_prioqueue(h_value, inpt_counter, new_covered, values, parent_input)
                            PriorityHandling.re_evaluate_queue()
                            Utils.current_iteration = 0

                    while inp in already_generated:
                        if Utils.inputs.empty():
                            print("[info] Nothing more to explore, search space is exhausted. Try new random value.")
                            # raise RuntimeError("Search space exhausted. Giving up exploration.")
                            inp = random.choice(Utils.continuations)
                            inp_rand_new = random.choice(Utils.continuations) + random.choice(Utils.continuations)
                            parent_input = None
                            # TODO May be necessary to populate the initial input space with all ascii chars as otw. some possible values may be missed
                            # # reset heuristic value, prio queue and already generated set to start with next round
                            # Utils.inputs = queue.PriorityQueue()
                            # # reset the currently covered branches to the ones covered by the valid inputs, hence the new heuristic value is based on the
                            # # branches not seen before for the next round
                            # already_generated = set()
                            # # also reset the covered paths as otw. the search space might get exhausted too early after restarting
                            # Utils.covered_paths.clear()
                            # # start with a new random input for the next round
                            # inp = random.choice(Utils.continuations)
                            # inp_rand_new = inp
                            # break
                        else:
                            parent_input: InputWrapper = Utils.inputs.pop()
                            inp, inp_rand_new = parent_input.val.get_corrections()

                    already_generated.add(inp)
                    Utils.current_iteration += 1

                    print("Number of executions: %d\nApprox. Search Space Size: %d\nExecuted since last found: %d\n" % (Utils.all_exec, len(Utils.inputs), Utils.current_iteration))
                    print_stats_file()
                    print("Smallest in Heap: %s\n\tValue: %s\n" % (str(Utils.inputs.heap[0].prio_value), Utils.inputs.heap[0].val) if Utils.inputs.heap else ("PrioQueue is currently empty.", ""))
                    print(repr(inp) + "\n")
                    exit_code = run_and_trace(parentdir, inp, arg, False)
                    Utils.all_exec += 1
                    if exit_code == 0:
                        Utils.all_exec += 1  # this was again executed as the first run of inp is w/o instrumentation
                        (h_value, new_covered, values, knowledge, all_covered) = get_substitution(os.path.join(parentdir, "pyg.txt"),
                                                                                     inp,
                                                                                     parent_input.prio_value.stack if parent_input else [],
                                                                                     dfs)
                        if not all_covered.issubset(Utils.valid_covered.keys()):
                            inp_rand_new = inp
                            continue
                    already_generated.add(inp_rand_new)
                    Utils.current_iteration += 1
                    print(repr(inp_rand_new) + "\n")
                    exit_code = run_and_trace(parentdir, inp_rand_new, arg)
                    Utils.all_exec += 1
                    (h_value, new_covered, values, knowledge, all_covered) = get_substitution(os.path.join(parentdir, "pyg.txt"),
                                                                                 inp_rand_new,
                                                                                 parent_input.prio_value.stack if parent_input else [],
                                                                                 dfs)


def print_stats_file():
    Utils.stats_file.seek(0)
    Utils.stats_file.write("Number of executions: %d\nApprox. Search Space Size: %d\nExecuted since last found: %d\n" % (Utils.all_exec, len(Utils.inputs), Utils.current_iteration))
    Utils.stats_file.flush()

if __name__ == '__main__':
    import argparse

    rseed = int(os.environ.get('RSEED', '0'))
    random.seed(rseed)
    parser = argparse.ArgumentParser(description="pFuzzer")
    parser.add_argument('-p', "--program", metavar="subject", type=str,
                        help="The path to the subject as c file to run on.", required=True)
    parser.add_argument('-a', "--arg", metavar="arg", default="False", type=str,
                        help="Defines if the input is given as argument or via stdin.", required=True)
    parser.add_argument('-i', "--instrument", metavar="arg", default="True", type=str,
                        help="Defines if the program under test gets instrumented.", required=True)
    parser.add_argument('-f', "--flag", metavar="arg", default="", type=str,
                        help="Defines the flag that is used if the program gets the argument over the command line.", required=False)
    args = parser.parse_args()
    Utils.subject = os.path.abspath(args.program)
    g_arg = args.arg.lower() == "true"
    instrument = args.instrument.lower() == "true"

    # instrument program
    Utils.cwd = os.getcwd()
    if instrument:
        os.system("%s/../install/bin/trace-instr %s %s/../samples/excluded_functions" % (Utils.cwd, Utils.subject, Utils.cwd))
    Utils.g_flag = ""
    if args.flag:
        Utils.g_flag = "-%s" % args.flag
    g_parentdir = os.path.abspath(os.path.join(Utils.subject, os.pardir))

    os.chdir(g_parentdir)

    run_loop(g_parentdir, g_arg)
