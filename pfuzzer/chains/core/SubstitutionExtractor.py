# coding=utf-8
"""
Contains the code which extracts the next substitutions based on an execution log.
"""
import json
import string
import random
from typing import Set, Tuple, Any, List, Dict

from core.HeuristicValue import HeuristicValue
from core.InputValue import InputValue
import core.Pruning as Pruning
import core.Utils as Utils
from core.KnowledgeHandling import KnowledgeHandling
import core.ConversionHandler as ConversionHandler


def get_substitution(taints: str, inpt: str, parent_stack: List[str], dfs: bool = True) -> \
Tuple[HeuristicValue, Set[Tuple[Any, Any]], List[InputValue], KnowledgeHandling, Set[Tuple[Any, Any]]]:
    """
    Returns the next position to substitute and the possible substitutions.
    :param covered_branches_parent:
    :param taints: the taints as json string
    :param inpt: the used input
    :param h_value_parent: the heuristic value of the current input
    :param dfs: the flag to show if dfs or bfs is used
    :return:
    """
    objs = []
    with open(taints, 'r+') as f:
        for i in f.readlines():
            if i.strip()[0] == '#':
                continue
            if i.strip() == '':
                continue
            v = json.loads(i)
            objs.append(v)
    nxt = _process(objs, inpt, dfs)
    knowledgehandler = KnowledgeHandling(objs)
    new_h_value, new_covered, all_covered = HeuristicValue.calc_cov_heuristic(objs, parent_stack, inpt, knowledgehandler)
    return new_h_value, new_covered, nxt, knowledgehandler, all_covered


def _construct_correction_list(corrections, dfs, inpt, last_index):
    """
    Creates the correction list based on if dfs or bfs is used
    :param corrections: All possible subsitutions
    :param dfs: dfs or bfs? True for dfs
    :param inpt: The input that was used to generate the comparisons
    :param last_char_idx: the index of the character that needs to be substituted
    :return:
    """
    # if a flag is used the position of the index shifts, the min_index is not correct anymore and needs to be fixed
    # i.e. the flag is also checked at some point, so the min_index is the value point the flag starts rather than the point
    # at which the actual input starts, this is corrected here
    if not(Utils.g_flag == ""):
        Utils.min_index += len(Utils.g_flag) + 1
    if dfs:
        choice = random.choice(corrections)
        return [InputValue(choice[2], Utils.min_index, choice[0], choice[1], inpt)]
    else:
        # add a random char and a random number to corrections as it often happens that there is no concrete check for those (i.e. everything except the chars that were found is accepted)
        corrections.append(('H', 'random', last_index))
        corrections.append(('5', 'random', last_index))
        return [InputValue(cor[2], Utils.min_index, cor[0], cor[1], inpt) for cor in corrections]


def _get_comparisons_on_idx(objs: List[Dict[str, Any]], char_idx: int):
    """
    Returns the comparisons made on the character with the given index.
    :param objs: The list of comparisons made during the execution.
    :param char_idx: The character index for which the comparisons should be extracted.
    :return:
    """
    Utils.min_index = min([idx["index"][0] for idx in objs])
    return [c for c in objs if (char_idx in c['index']) or (c['operator'] == "strlen" and c['index'] and c['index'][0] + int(c['operand'][0]) >= char_idx) or (c['operator'] == "strcmp" and c['index'][0] + len(c['operand'][0]) >= char_idx)]


def _get_corrections(cmp_stack: List[Any], local_continuations: List[str]):
    """
    Returns the possible substitutions for the character at index at_idx
    :param cmp_stack: The comparisons made on the last character.
    :param local_continuations: The possible characters to insert if there is no useful continuation
    :return:
    """
    chars = set()
    for char in cmp_stack:
        if char["operator"] == "switch":
            chars |= set((el, char["operator"], char["index"][0]) for el in char["operand"])
        elif char["operator"] == "strlen":
            length = int(char["operand"][0])
            chars |= {("".join(["a" for _ in range(0, length)]), char["operator"], char["index"][0])}
        elif char["operator"] == "conversion":
            pos_subst = ConversionHandler.getPossibleSubstitutions(char["operand"][0])
            chars |= set((el, char["operator"], char["index"][0]) for el in pos_subst)
        else:
            chars |= {(random.choice(char["operand"]), char["operator"], char["index"][0])}
    if not chars:
        return list(set(local_continuations))
    return list(chars)


def _get_last_comparison(objs):
    """
    Get the last comparison from the object list.
    :param objs:
    :return:
    """
    for val in reversed(objs):
        if val["type"] == "INPUT_COMPARISON" and val["operator"] != "eof" and val["operator"] != "strlen":
            return val


def _process(objs: List[Dict[str, Any]], inpt: str, dfs: bool = False) -> List[InputValue]:
    """
    Returns the next position to substitute and the possible substitutions based on a list of comparisons
    :param dfs:
    :param objs: The list of comparisons made during the execution.
    :param inpt: The input used for the execution.
    :return:
    """
    # the sole input comparisons for those functions that need it
    input_comparisons = [obj for obj in objs if obj["type"] == "INPUT_COMPARISON"]

    if not objs:
        # FIXME return proper list of corrections
        return [InputValue(0, 0, random.choice(string.printable), "undef", inpt)]

    # get the last comparison made, then extract the character index.
    last_comparison = _get_last_comparison(objs)
    if not last_comparison:
        # raise ValueError("The extraction of the last made comparison returned incorrectly.")
        return []
        # return _construct_correction_list([random.choice(Utils.continuations)], dfs, inpt, Utils.min_index + len(inpt) - 1)
    input_len = last_comparison["length"]
    Utils.max_index = last_comparison['index'][0]
    # if we can prune this branch of the search space, we do so
    if Pruning.is_pruned(objs, input_comparisons, Utils.max_index):
        return []
    if input_len <= Utils.max_index:  # EOF -- generate more data
        # FIXME return proper list of corrections
        return [InputValue(Utils.max_index, Utils.min_index, random.choice(string.printable), "undef", inpt)]

    # First, get all the comparisons on the index of last character
    # comparison
    cmps = _get_comparisons_on_idx(input_comparisons, Utils.max_index)

    corrections = _get_corrections(cmps, Utils.continuations)

    return _construct_correction_list(corrections, dfs, inpt, Utils.max_index)
