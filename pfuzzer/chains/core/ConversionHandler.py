# coding=utf-8

from typing import List


randomDict = {
    "strtod": ["1.0", "3.0E2", "4.5e2", "-1.0", "-3.0E2", "-4.5e2"],
    "strtoul": ["-1", "1", "0x1F", "0XAB", "-0x1F", "-0XAB"]
}


# TODO could also return just one alternative, will be seen empirically which is better
def getPossibleSubstitutions(function: str) -> List[str]:
    """
    Returns for a conversion function the possible values it can convert.
    :param function: The name of the function used for conversion.
    :return:
    """
    fun = ""
    for c in iter(function):  # some function have some platform specific names with special chars, those are deleted here
        if c.isalnum():
            fun += c
    if fun in randomDict:
        return randomDict.get(fun)
    else:
        return []
