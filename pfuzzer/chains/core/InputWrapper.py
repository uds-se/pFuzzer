# coding=utf-8
"""
Contains the code and data of the elements to put in the prio queue.
"""
from typing import TYPE_CHECKING, List, Tuple

if TYPE_CHECKING:
    from core.InputValue import InputValue
    from core.HeuristicValue import HeuristicValue


class InputWrapper:
    """
    An element to put in the prio queue.
    """
    # the Priority value
    prio_value: "HeuristicValue" = None
    # the id of the value
    id = 0
    # the list of newly covered branches and branches that were already covered by valid inputs
    covered_branches: List[Tuple[int, int]] = None
    # the value which is stored
    val: "InputValue" = None

    def __init__(self, prio_value: "HeuristicValue", ident, covered_branches, val):
        self.prio_value = prio_value
        self.id = ident
        self.covered_branches = covered_branches
        self.val = val

    def __lt__(self, other):
        return (self.prio_value, self.id) < (other.prio_value, other.id)

    def __str__(self):
        return "val: %s, prio_value: %s, id: %d, covered_branches: %s" % (str(self.val), str(self.prio_value), self.id, str(self.covered_branches))

    def __repr__(self):
        return " val: %s, prio_value: %s, id: %d, covered_branches: %s" % (repr(self.val), repr(self.prio_value), self.id, repr(self.covered_branches))
