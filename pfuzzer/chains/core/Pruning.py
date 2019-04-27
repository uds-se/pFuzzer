# coding=utf-8
"""
Contains the code for pruning values.
"""
from typing import List, Dict, Any
import core.Utils as Utils


def is_pruned(objs: List[Dict[str, Any]], input_comparisons, last_index: int) -> bool:
    """
    Check if the input can be pruned.
    :param input_comparisons:
    :param objs:
    :param last_index:
    :return:
    """
    return (check_last_chars_same_comp(last_index, input_comparisons) or
            check_no_new_branches(objs))


def check_last_chars_same_comp(last_index, objs):
    """
    Check if on the 5 last chars the same comparisons were performed.
    :param last_index:
    :param objs:
    :return:
    """
    reference = [el for dc in objs for el in dc['operand'] if last_index in dc['index']]
    # check if the characters which are used for the comparisons of the last three comparisons are the same
    # as the comparisons on the last char that was used in a comparison
    for ind in range(last_index - 4, last_index - 1):
        check = [el for dc in objs for el in dc['operand'] if ind in dc['index']]
        if check != reference:
            return False
    return True


def check_no_new_branches(objs):
    """
    Checks if a list of 11 comparisons on different characters has no coverage event in between.
    If this happens the generator is stuck in a loop which is not able to proceed usefully.
    :param objs:
    """
    seen_indexes = set()
    for obj in objs:
        if Utils.is_real_input_comparison(obj, Utils.max_index - 1):
            seen_indexes.add(tuple(obj["index"]))
            if len(seen_indexes) >= 10:
                return True
        else:
            seen_indexes = set()
    return False
