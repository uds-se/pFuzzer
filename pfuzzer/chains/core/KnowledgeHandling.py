# coding=utf-8

from typing import List, Any, Tuple, Union

import core.Utils as Utils


# TODO in future the knowledgehander should encapsulate everything the subst extractor returns
class KnowledgeHandling():

    stackevents: List[Any] = None

    # contains the stacks between the last three char comparisons
    stacklists: Tuple[List, List, List] = None

    # contains the average stack size between the last three char comparisons
    averages: List[int] = None

    # contains the coverage of the top of the stack
    tos_cover_counter = 0

    # smallest stack between last two comparisons
    min_stack_list = list()

    # the branches covered on the tos
    tos_branches = set()

    def __init__(self, objs):
        self.stacklists = None
        self.tos_branches = set()
        self.min_stack_list = list
        self.tos_cover_counter = 0
        self.averages = None
        self.stackevents = None
        stackevents, averages = self._extractStackEvents(objs)
        # self._extractCharacterFunctionMapping(objs)
        # self._get_tos_coverage(objs)
        print(averages)


    def _extractStackEvents(self, objs) -> Tuple[List[Union[int, Tuple[Any, int]]], List[Union[float, Any]]]:
        """
        Get for the last three characters the average stacksize during execution until the next char comparison.
        :param objs:
        :return:
        """
        self.stackevents = list()
        lastchar = None
        lastindex = 0
        for val in objs:
            if val["type"] == "STACK_EVENT":
                self.stackevents.append(len(val["stack"]))
            elif val["type"] == "INPUT_COMPARISON" and val["operator"] != "eof":
                value = val["value"]
                index = int(val["index"][0])
                if value != lastchar:
                    self.stackevents.append((val["value"], index))
                    lastchar = value
                if index > lastindex:
                    lastindex = index
        # get the stacklist between the second last and the last character
        self.stacklists = (list(), list(), list())
        current_index = 0
        for val in self.stackevents:
            if type(val) == int:
                # print("".join([" " for _ in range(val)]) + str(val))
                character_pos = lastindex - current_index
                if character_pos in range(0, 3):
                    self.stacklists[character_pos].append(val)
            elif val[1] != current_index:
                # print("".join([str(val) for _ in range(10)]))
                current_index = val[1]
        self.averages = [sum(self.stacklists[i])/len(self.stacklists[i]) if self.stacklists[i] else 100 for i in reversed(range(0, 3))]
        return self.stackevents, self.averages

    def __str__(self):
        return str(self.min_stack_list) + "  " + str(self.tos_cover_counter)

    def __repr__(self):
        return str(self.averages)

    def _extractCharacterFunctionMapping(self, objs):
        """
        Compute the mapping for each character to the functions that are thrown off the stack after the character was compared the last time successfully.
        :param objs:
        :return:
        """
        stacklist_current_char = list()
        min_stack_list = None
        value = None
        index = -1
        for val in objs:
            if val["type"] == "INPUT_COMPARISON" and val["value"] in val["operand"]:
                value = val["value"]
                stacklist_current_char = val["stack"]
                min_stack_list = stacklist_current_char
                index = val["index"][0]
            elif val["type"] == "INPUT_COMPARISON" and index != -1 and int(val["index"][0]) != index:
                if value not in Utils.char_fun_mapping:
                    Utils.char_fun_mapping[value] = set()
                stacklist_to_add = set(stacklist_current_char) - set(min_stack_list)
                for el in stacklist_to_add:
                    Utils.char_fun_mapping[value].add(el)
                index = -1
            elif val["type"] == "STACK_EVENT" and min_stack_list and len(val["stack"]) < len(min_stack_list):
                min_stack_list = val["stack"]

    def _get_tos_coverage(self, objs):
        """
        Compute the minimal stack occuring for the last two comparisons. Based on this compute the coverage in the function on top of the minimal stack.
        This way one might get how far the fuzzer proceeded for the current grammatical element that is parsed.
        :param objs:
        :return:
        """
        self.min_stack_list = None
        index = -1
        for val in reversed(objs):
            if val["type"] == "INPUT_COMPARISON" and val["operator"] != "strlen" and index == -1:
                self.min_stack_list = val["stack"]
                index = int(val["index"][0])
            elif val["type"] == "INPUT_COMPARISON" and index != -1 and int(val["index"][0]) < index - 1:
                self._calc_tos_coverage(objs, self.min_stack_list[-1])
                return
            elif val["type"] == "STACK_EVENT" and self.min_stack_list and len(val["stack"]) < len(self.min_stack_list):
                self.min_stack_list = val["stack"]

    def _calc_tos_coverage(self, objs, tos):
        is_tos = False
        self.tos_cover_counter = 0
        last_was_eof = False
        cur_idx = -1
        tmp_events = set()
        for obj in objs:
            # consider only basic block jumps up until the last "real" comparison
            if obj["type"] == "INPUT_COMPARISON" and obj["operator"] == "eof":
                tmp_events = set()
            elif obj["type"] == "STACK_EVENT" and len(obj["stack"]) == len(self.min_stack_list) and obj["stack"][-1] == tos:
                is_tos = True
            elif obj["type"] == "STACK_EVENT" and obj["stack"] and obj["stack"][-1] != tos:
                is_tos = False
            elif Utils.is_real_input_comparison(obj, Utils.max_index - 1) and cur_idx != obj["index"][0]:
                last_was_eof = False
                self.tos_branches.update(tmp_events)
                tmp_events = set()
                cur_idx = int(obj["index"][0])
            elif obj["type"] == "INPUT_COMPARISON" and obj["operator"] == "eof" and not last_was_eof:
                last_was_eof = True
                self.tos_branches.update(tmp_events)
                tmp_events = set()
            elif is_tos and obj["type"] == "COVERAGE_EVENT":
                tmp_events.add((obj["old"], obj["new"]))
        self.tos_cover_counter = len(self.tos_branches - Utils.valid_covered.keys())

    def recalc(self):
        """
        Update self w.r.t. the new environment, i.e. the valid covered branches has changed.
        """
        self.tos_cover_counter = len(self.tos_branches - Utils.valid_covered.keys())


