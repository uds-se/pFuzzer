#!/usr/bin/env python3

import re

def main(argv):
    with open(argv[1], "r") as infile, open(argv[2], "w") as outfile:
        for line in infile.readlines():
            if not '@WRAPPERCALL@' in line:
                outfile.write(line)
                continue

            wrapline = re.compile('^\s*@WRAPPERCALL@\((.*)\)')
            result = wrapline.match(line)
            if not result:
                print('error: invalid call to @WRAPPERCALL@')
                sys.exit(1)

            name = result.groups()[0].split(',')[0].strip()
            type = result.groups()[0].split(',')[1].strip()
            args = result.groups()[0].split(',', 2)[2].strip()

            outfile.write('{} tracerllvm_wrap_{}({});\n'.format(type, name, args))
            outfile.write('#undef {}\n'.format(name))
            outfile.write('#define {}(...) tracerllvm_wrap_{}(__VA_ARGS__)\n'.format(name, name))

if __name__ == '__main__':
    import sys

    if len(sys.argv) != 3:
        print('usage: {} <infile> <outfile>'.format(sys.argv[0]))
        sys.exit(0)

    main(sys.argv)
