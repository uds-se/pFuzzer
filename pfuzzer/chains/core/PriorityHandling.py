# coding=utf-8
"""
Contains the priority value code.
"""
import string
from typing import List
from typing import TYPE_CHECKING

import core.Utils as Utils
from core.Heap import Heap
from core.HeuristicValue import HeuristicValue
from core.InputWrapper import InputWrapper

if TYPE_CHECKING:
    from core.InputValue import InputValue


def _char_stacksize_mapping(old_stack: int, new_stack: int, correction: str):
    """
    Calculates for the correction if the stacksize increased or decreased and puts it in the stacksize map
    :param new_stack:
    :param old_stack:
    :param correction
    :return:
    """
    stack_change = -old_stack / new_stack if old_stack > new_stack else new_stack / old_stack
    stack_change = 0 if old_stack == new_stack else stack_change/new_stack
    if Utils.map_char_stack.get(correction):
        Utils.map_char_stack[correction] += stack_change
    else:
        Utils.map_char_stack[correction] = stack_change
    Utils.map_char_stack[correction] /= 2
    # print(Utils.map_char_stack)


def add_values_to_prioqueue(h_value: "HeuristicValue", inpt_counter, new_covered, values: List["InputValue"], parent_input: "InputWrapper"):
    """
    Adds the values with their heuristic value, the  to the given priority queue inputs.
    :param parent_input: The input used in the execution which lead to the new input.
    :param h_value: The heuristic value of the input that was just uesd for running the program under test.
    :param inpt_counter: The counter containing the number of already generated inputs.
    :param new_covered: The branches covered by the input that was just used for running the program under test.
    :param values: The dicts describing the substitutions to generate new values.
    :return: The value for the generated input counter.
    """
    if parent_input is not None:
        # +1 to avoid division by 0
        _char_stacksize_mapping(len(parent_input.prio_value.stack) + 1, len(h_value.stack) + 1, parent_input.val.correction)
    return _add_values_to_queue(h_value, inpt_counter, new_covered, values, parent_input)


def _add_values_to_queue(h_value, inpt_counter, new_covered, values, parent_input: "InputWrapper"):
    """
    The actual addition code.
    :param h_value:
    :param inpt_counter:
    :param new_covered:
    :param values:
    :return:
    """
    if h_value.cover_counter[0] >= 0:
        for val in values:
            # delete possible random continuations which are possibly part of the program input
            if val.correction in Utils.continuations:
                Utils.continuations.remove(val.correction)
            # TODO for the moment refill continuations if it runs empty, in future use a wider range of cont.
            if not Utils.continuations:
                Utils.continuations = [i for i in string.printable]
            inpt_counter += 1
            # add for each substitution and element to the prio queue which will be inserted based on the
            # heuristic value of the substitution
            new_h_value = h_value.clone()
            new_h_value.set_input_counter_adapt_value(parent_input.prio_value.input_counter + 1 if parent_input else 0)
            new_h_value.adjust_value(len(val.correction) * 2)
            Utils.inputs.push(InputWrapper(new_h_value, inpt_counter, new_covered, val))
    else:
        for val in values:
            if val.correction in Utils.continuations:
                Utils.continuations.remove(val.correction)
            # TODO for the moment refill continuations if it runs empty, in future use a wider range of cont.
            if not Utils.continuations:
                Utils.continuations = [i for i in string.printable]
            inpt_counter += 1
            # add for each substitution and element to the prio queue which will be inserted based on the
            # heuristic value of the substitution
            stack_val = Utils.map_char_stack.get(val.correction)
            if stack_val is not None:
                new_h_value = h_value.clone()
                new_h_value.adjust_value(-stack_val * len(val.inp))
                Utils.inputs.push(InputWrapper(new_h_value, inpt_counter, new_covered, val))
            else:
                Utils.inputs.push(InputWrapper(h_value.clone(), inpt_counter, new_covered, val))
    return inpt_counter


def re_evaluate_queue():
    """
    If a valid input is found the whole queue has to be sorted again as all values have a new heuristic value at this point.
    :param valid_covered: The branches covered by valid inputs at this point.
    """
    old_queue = Utils.inputs
    Utils.inputs = Heap()
    for el in old_queue:
        cover_counter = 0
        new_covered = 0
        val: InputWrapper = el.val
        for el_cov in el.covered_branches:
            if el_cov not in Utils.valid_covered:
                cover_counter += 2
                new_covered += 1
            else:
                cover_counter += 1 / Utils.valid_covered[el_cov]
        el.prio_value.knowledge.recalc()
        new_h_value = HeuristicValue((new_covered, cover_counter), el.prio_value.stack, el.prio_value.parent_stack, el.prio_value.inp, el.prio_value.same_path_taken, el.prio_value.knowledge)
        # when the value is re-added to the queue the input_counter value gets increased by one
        el.prio_value.input_counter -= 1
        _add_values_to_queue(new_h_value, el.id - 1, el.covered_branches, [val], el)
