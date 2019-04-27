# coding=utf-8
"""
Contains the code that generates from an input and a given correction a new input.
"""
import random
import core.Utils as Utils


class InputValue:
    """
    Generates from a given input based on operator, correcting string and the parent input a new input.
    """
    # the position at which the char subst starts
    at = 0
    # the start of the subst
    min = 0
    # the string to subst the char with
    correction = ""
    # the operator that caused the subst. strategy
    operator = ""
    # the input that was used in the execution
    inp = ""

    def __init__(self, at, min_pos, correction, operator, inp):
        self.at = at
        self.min = min_pos
        self.correction = correction
        self.operator = operator
        self.inp = inp

    def __str__(self):
        return "inp: %s, at: %d, min: %d, correction: %s, operator: %s" % (repr(self.inp), self.at, self.min, repr(self.correction), self.operator)

    def __repr__(self):
        return self.__str__()

    def get_corrections(self):
        """
        Returns the new input string based on parent and other encapsulated parameters.
        :return:
        """
        new_char = self.correction
        subst_index = self.at - self.min
        inp = self.inp
        # replace char of last comparison with new continuation
        inp = inp[:subst_index] + new_char
        # append a new char, it might be that the program expects additional input
        # also if the newchar is not a char but a string it is likely a keyword, so we better add a whitespace
        if self.operator == "strcmp":
            inp_rand_new = inp + " " + random.choice(Utils.continuations)
        elif self.operator == "strlen":
            inp_rand_new = inp
        else:
            inp_rand_new = inp + random.choice(Utils.continuations)
        return inp, inp_rand_new
