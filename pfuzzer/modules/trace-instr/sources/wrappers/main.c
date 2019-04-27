#include <stdlib.h>
#include <stdio.h>
#include "callbacks.h"

int _real_program_main(int argc, char* argv[]);

int main(int argc, char* argv[])
{
    atexit(tracerllvm_endTrace);

    tracerllvm_beginTrace();
    tracerllvm_argcargv(argc, argv);

    return _real_program_main(argc, argv);
}
