# coding=utf-8
"""
File containing code to calculate the metric for justifying how well the valid input are.
"""
import re

from typing import Tuple, Dict, Any

num_tokens_in_chain = 2

# tinyc = [
#     r"while\s*(.*)\s*{.*}",
#     r"while\s*(.*).*;",
#     r"if\s*(.*)\s*{.*}",
#     r"if\s*(.*).*;",
#     r"if\s*(.*)\s*{.*}\s*else\s*{.*}",
#     r"if\s*(.*).*;\s*else\s*{.*}",
#     r"if\s*(.*)\s*{.*}\s*else.*;",
#     r"if\s*(.*).*;\s*else.*;",
#     r"do\s*{.*}\s*while\s*(.*)\s*;",
#     r"do.*;\s*while\s*(.*)\s*;",
#     r"[a-z]\s*=.",
#     r"[<+-]",
#     r";",
#     r"{",
#     r"\("
# ]

cjson = [
    (r"\".*?(?!=\\)\"", 'st'),
    (r"\'.*?(?!=\\)\'", 'st'),
    (r"\".*?(?!=\\)\Z", 'st'),
    (r"\'.*?(?!=\\)\Z", 'st'),
    (r"(?<![a-z])null(?![a-z])", "null"),
    (r"(?<![a-z])true(?![a-z])", "true"),
    (r"(?<![a-z])false(?![a-z])", "false"),
    (r"[0-9][0-9]*(\.[0-9]*)?((e|E)[0-9]*)?", "num"),
    (r"@", "st"),
    (r"-", "-"),
    (r",", ","),
    (r":", ":"),
    (r"\{", "{"),
    (r"\[", "["),
    (r"\}", "}"),
    (r"\]", "]")
]

csv = [
    (r",", ","),
    (r"[^,]*", "ident")
]

ini = [
    (r"\[", "["),
    (r"\]", "]"),
    (r":", ":"),
    (r"=", "="),
    (r"[^:=[]]*", "ident")
]

tinyc = [
    (r"(?<![a-z])while(?![a-z])", "while"),
    (r"(?<![a-z])if(?![a-z])", "if"),
    (r"(?<![a-z])else(?![a-z])", "else"),
    (r"(?<![a-z])do(?![a-z])", "do"),
    (r"[a-z]", "ident"),
    (r"[0-9]+", "num"),
    (r"<", "<"),
    (r"\+", "+"),
    (r"-", "-"),
    (r";", ";"),
    (r"=", "="),
    (r"\{", "{"),
    (r"\(", "("),
    (r"\}", "}"),
    (r"\)", ")")
]

mjs = [
    (r"\".*?(?!=\\)\"", 'st'),
    (r"\'.*?(?!=\\)\'", 'st'),
    (r"\".*?(?!=\\)\Z", 'st'),
    (r"\'.*?(?!=\\)\Z", 'st'),
    (r"(?<![a-zA-Z$_])JSON(?![0-9a-zA-Z$_])", "JSON"),
    (r"(?<![a-zA-Z$_])parse(?![0-9a-zA-Z$_])", "parse"),
    (r"(?<![a-zA-Z$_])stringify(?![0-9a-zA-Z$_])", "stringify"),
    (r"(?<![a-zA-Z$_])Object(?![0-9a-zA-Z$_])", "Object"),
    (r"(?<![a-zA-Z$_])create(?![0-9a-zA-Z$_])", "create"),
    (r"(?<![a-zA-Z$_])let(?![0-9a-zA-Z$_])", "let"),
    (r"(?<![a-zA-Z$_])length(?![0-9a-zA-Z$_])","length"),
    (r"(?<![a-zA-Z$_])print(?![0-9a-zA-Z$_])", "print"),
    (r"(?<![a-zA-Z$_])load(?![0-9a-zA-Z$_])", "load"),
    (r"(?<![a-zA-Z$_])die(?![0-9a-zA-Z$_])", "die"),
    (r"(?<![a-zA-Z$_])slice(?![0-9a-zA-Z$_])", "slice"),
    (r"(?<![a-zA-Z$_])at(?![0-9a-zA-Z$_])", "at"),
    (r"(?<![a-zA-Z$_])indexOf(?![0-9a-zA-Z$_])", "indexOf"),
    (r"(?<![a-zA-Z$_])chr(?![0-9a-zA-Z$_])", "chr"),
    (r"(?<![a-zA-Z$_])mkstr(?![0-9a-zA-Z$_])", "mkstr"),
    (r"(?<![a-zA-Z$_])ffi(?![0-9a-zA-Z$_])", "ffi"),
    (r"(?<![a-zA-Z$_])gc(?![0-9a-zA-Z$_])", "gc"),
    (r"(?<![a-zA-Z$_])if(?![0-9a-zA-Z$_])", "if"),
    (r"(?<![a-zA-Z$_])else(?![0-9a-zA-Z$_])", "else"),
    (r"(?<![a-zA-Z$_])do(?![0-9a-zA-Z$_])", "do"),
    (r"(?<![a-zA-Z$_])for(?![0-9a-zA-Z$_])", "for"),
    (r"(?<![a-zA-Z$_])break(?![0-9a-zA-Z$_])", "break"),
    (r"(?<![a-zA-Z$_])continue(?![0-9a-zA-Z$_])", "continue"),
    (r"(?<![a-zA-Z$_])function(?![0-9a-zA-Z$_])", "function"),
    (r"(?<![a-zA-Z$_])true(?![0-9a-zA-Z$_])", "true"),
    (r"(?<![a-zA-Z$_])false(?![0-9a-zA-Z$_])", "false"),
    (r"(?<![a-zA-Z$_])typeof(?![0-9a-zA-Z$_])", "typeof"),
    (r"(?<![a-zA-Z$_])undefined(?![0-9a-zA-Z$_])", "undefined"),
    (r"(?<![a-zA-Z$_])this(?![0-9a-zA-Z$_])", "this"),
    (r"(?<![a-zA-Z$_])case(?![0-9a-zA-Z$_])", "case"),
    (r"(?<![a-zA-Z$_])catch(?![0-9a-zA-Z$_])", "catch"),
    (r"(?<![a-zA-Z$_])debugger(?![0-9a-zA-Z$_])", "debugger"),
    (r"(?<![a-zA-Z$_])default(?![0-9a-zA-Z$_])", "default"),
    (r"(?<![a-zA-Z$_])delete(?![0-9a-zA-Z$_])", "delete"),
    (r"(?<![a-zA-Z$_])finally(?![0-9a-zA-Z$_])", "finally"),
    (r"(?<![a-zA-Z$_])in(?![0-9a-zA-Z$_])", "in"),
    (r"(?<![a-zA-Z$_])instanceof(?![0-9a-zA-Z$_])", "instanceof"),
    (r"(?<![a-zA-Z$_])null(?![0-9a-zA-Z$_])", "null"),
    (r"(?<![a-zA-Z$_])new(?![0-9a-zA-Z$_])", "new"),
    (r"(?<![a-zA-Z$_])return(?![0-9a-zA-Z$_])", "return"),
    (r"(?<![a-zA-Z$_])switch(?![0-9a-zA-Z$_])", "switch"),
    (r"(?<![a-zA-Z$_])throw(?![0-9a-zA-Z$_])", "throw"),
    (r"(?<![a-zA-Z$_])try(?![0-9a-zA-Z$_])", "try"),
    (r"(?<![a-zA-Z$_])var(?![0-9a-zA-Z$_])", "var"),
    (r"(?<![a-zA-Z$_])void(?![0-9a-zA-Z$_])", "void"),
    (r"(?<![a-zA-Z$_])while(?![0-9a-zA-Z$_])", "while"),
    (r"(?<![a-zA-Z$_])with(?![0-9a-zA-Z$_])", "with"),

    (r"[a-zA-Z$_][0-9a-zA-Z$_]*", "ident"),

    (r"[0-9][0-9]*(\.[0-9]*)?((e|E)[0-9]*)?", "num"),
    (r">>>=", ">>>="),
    (r">>>", ">>>"),
    (r">>=", ">>="),
    (r"<<=", "<<="),
    (r"===", "==="),
    (r"!==", "!=="),
    (r"<<", "<<"),
    (r">>", ">>"),
    (r"--", "--"),
    (r"\+\+", "++"),
    (r"\+=", "+="),
    (r"-=", "-="),
    (r"\*=", "*="),
    (r"/=", "/="),
    (r"&=", "&="),
    (r"\|=", "|="),
    (r"%=", "%="),
    (r"\^=", "^="),
    (r"==", "=="),
    (r"\!=", "!="),
    (r"<=", "<="),
    (r">=", ">="),
    (r"&=", "&&"),
    (r"\|\|", "||"),
    (r"<", "<"),
    (r">", ">"),
    (r"\|", "|"),
    (r"\^", "^"),
    (r"%", "%"),
    (r"\+", "+"),
    (r"-", "-"),
    (r";", ";"),
    (r"=", "="),
    (r"&", "&"),
    (r",", ","),
    (r":", ":"),
    (r"\?","?"),
    (r"~", "~"),
    (r"\*", "*"),
    (r"\\", "\\"),
    (r"\.", "."),
    (r"\!", "!"),
    (r"/", "/"),
    (r"\{", "{"),
    (r"\(", "("),
    (r"\}", "}"),
    (r"\)", ")"),
    (r"\]", "]"),
    (r"\[", "[")
]

global_dict = {"tiny.c": tinyc,
               "mjs.c": mjs,
               "cJSON.c": cjson,
               "csvparser.c": csv,
               "ini.c": ini}  # holds entries for all languages??


def create_sample():  # just for testing purposes
    """
    Creates an example to test the program.
    """
    file = open('sample.txt')  # sys.argv[0] is the name of the script
    regex = file.read().split("\n")  # caution: if file has empty line at the end, it will include '' in list...
    file.close()

    global_dict["sample"] = regex


#
# def count_regex(key, string_list, num_combinations: int):
#     """
#     :param key: The name of the subject s.t. the list of tokens can be found
#     :param string_list: The list of input strings which are searched for the given tokens
#     :param num_combinations: The number of tokens used in the chain which is used for classification
#     @:return : dict with key=regex, value=occurrences in string_list
#     """
#     tokenlist = global_dict[key]
#     for i in range(1,num_combinations):
#         tmp_list = []
#         for single_tok in global_dict[key]:
#             for el in list(tokenlist):
#                 tmp_list.append("%s\s*%s" % (el, single_tok))
#         tokenlist = tmp_list
#     my_dict = dict.fromkeys(tokenlist, 0)
#     for s in string_list:
#         s = preprocess_strings(s)
#         for reg in my_dict:
#             occurrences = re.findall(reg, s, re.DOTALL)  # pattern=regex
#             if occurrences:
#                 print("%s in %s" % (reg, s))
#                 my_dict[reg] += len(occurrences)
#     return my_dict

# TODO pre-compile patterns
def count_regex(key, string_list):
    tokenlist = global_dict[key]
    results = []
    for s in string_list:
        # s = preprocess_strings(s)
        result = []
        while s:
            token, s = next_token(s, tokenlist)
            if token:
                result.append(token)
        results.append(result)

    counter_dict = dict()
    for el in results:
        insert = ""
        while len(el) > num_tokens_in_chain - 1:
            for i in range(0, num_tokens_in_chain):
                insert += "%s " % el[i]
            if insert in counter_dict:
                counter_dict[insert] += 1
            else:
                counter_dict[insert] = 1
            insert = ""
            el = el[1:]
    token_set = set()
    for tklist in results:
        for tk in tklist:
            token_set.add(tk)
    print("#################\nTokenlist:\n")
    token_length_dict = dict()
    for tk in token_set:
        print("\t%s\n" % tk)
        tok_length = len(tk) if tk not in ["num", "ident"] else 1
        if tok_length not in token_length_dict:
            token_length_dict[tok_length] = 1
        else:
            token_length_dict[tok_length] += 1
    for toksize, counter in token_length_dict.items():
        print("\nToklength: %d\nTokCount: %d\n" % (toksize, counter))
    print("Two token size: %d\n######################\n" % len(counter_dict.keys()))
    return counter_dict


# store the chars that are not defined s.t. we can add them later if necessary
undefined_chars = set()


def next_token(inpt: str, regs: Dict[str, str]) -> Tuple[Any, str]:
    match = re.match("\s+", inpt, re.DOTALL)
    if match:
        inpt = inpt[match.end(0):]
        if not inpt:
            return "", ""
    for reg, val in regs:
        # print(reg)
        match = re.match(reg, inpt, re.DOTALL)
        if match and match.endpos:
            return val, inpt[match.end(0):]

    # in this case we found an undefined character
    if inpt[0] not in undefined_chars:
        undefined_chars.add(inpt[0])
        print("Character %s in %s is undefined.\n" % (repr(inpt[0]), repr(inpt)))
    return None, inpt[1:]


# def preprocess_strings(inpt: str):
#     tmp = re.sub(r"\".*?(?!=\\)\"", '@', inpt, flags=re.DOTALL)
#     tmp = re.sub(r"\'.*?(?!=\\)\'", '@', tmp, flags=re.DOTALL)
#     tmp = re.sub(r"\".*?(?!=\\)\Z", '@', tmp, flags=re.DOTALL)
#     tmp = re.sub(r"\'.*?(?!=\\)\Z", '@', tmp, flags=re.DOTALL)
#     return tmp


def main():
    create_sample()
    input_file = open('sample-strings.txt')
    input = input_file.read().split("\n")  # caution: if file has empty line at the end, it will include '' in list...
    input_file.close()

    my_dict = count_regex("sample", input)
    # and now do whatever you want with the dict :D


if __name__ == "__main__":
    # main()
    token_length_dict = dict()
    for el in mjs:
        tok_length = len(el[1]) if el[1] not in ["num", "ident"] else 1
        if tok_length not in token_length_dict:
            token_length_dict[tok_length] = 1
        else:
            token_length_dict[tok_length] += 1
    for toksize, counter in token_length_dict.items():
        print("\nToklength: %d\nTokCount: %d\n" % (toksize, counter))
