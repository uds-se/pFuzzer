#include <stdio.h>
#include <string.h>

#include "callbacks.h"
#include "operators.h"

char* tracerllvm_wrap_fgets(char* restrict str, int size, FILE* restrict stream)
{
    char* result;
    if ((result = fgets(str, size, stream)) != NULL)
    {
        tracerllvm_optOpcode(FGETS);
        tracerllvm_optInput((uintptr_t)stream, result);
    }

    tracerllvm_instructionEnd();
    return result;
}

// gets was removed in C11 so it might not be defined
#ifdef gets
char* tracerllvm_wrap_gets(char* restrict str)
{
    char* result;
    if ((result = gets(str)) != NULL)
    {
        tracerllvm_optOpcode(FGETS);
        tracerllvm_optInput((uintptr_t)stdin, result);
    }

    tracerllvm_instructionEnd();
    return result;
}
#endif
