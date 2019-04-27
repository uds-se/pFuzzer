#include <fcntl.h>
#include <inttypes.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <json-c/json_object.h>

#include "zlib.h"
#include "callbacks.h"

static char* outputFile = "output";

// temporarily stores the fileposition for filereadcalls, which might need the position
// to reconstruct what was read, e.g.:
// fscanf reads 5 bytes, then the initial position before the read is stored, the file can be rewinded
// to the position and the 5 bytes can be read again
static int64_t filePosition = 0;

static int usedIntSize = 64;

// calls from the analysis to the instrumented standard library would cause calls back to the analysis
// therefore a bool flag has to be used to tell the analysis, that the initial call came from
// the analysis and no further analysis output has to be performed.
static bool noInstrument = false;

static FILE* taintAnalysisCallFile;

static bool traceStarted = false;
static bool ongoingInstr = false;

static json_object* currentEvent    = NULL;
static json_object* currentOperands = NULL;
static json_object* currentOpts     = NULL;

char* vectorValues(int64_t* values, int64_t size)
{
    // no call here to check whether it has to be instrumented, this is no API function

    int sizeInMemory = usedIntSize * size + size;
    char* str = malloc(sizeInMemory); // create a sufficiently large buffer to store the string temporarily, "+size" to store the spaces and /0

    // from stckoverflow.com/questions/30234363/how-can-i-store-an-int-array-into-string
    int index = snprintf(&str[0], sizeInMemory, "%" PRIu64, (uint64_t)values[0]);
    for (int i = 1; i < size; i++)
    {
        index += snprintf(&str[index], sizeInMemory - index, " %" PRIu64, (uint64_t)values[i]);
    }

    return str;
}

void tracerllvm_argcargv(int argc, char** argv)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    fflush(stdout);
    fprintf(taintAnalysisCallFile, "{\"av\":[\n");
    fflush(taintAnalysisCallFile);
    fflush(stdout);

    for (int x = 0; x < argc; ++x)
    {
        fprintf(taintAnalysisCallFile, "{\"e\":[\n");
        char* cPtr = argv[x];
        while (*cPtr)
        {
            // escape special json characters of the input
            if ((*cPtr == '"') || (*cPtr == '\\'))
            {
                fprintf(taintAnalysisCallFile, "{\"p\":\"%" PRIuPTR "\",\"c\":\"\\\%c\"}", (uintptr_t)cPtr, *cPtr);
            }
            else
            {
                fprintf(taintAnalysisCallFile, "{\"p\":\"%" PRIuPTR "\",\"c\":\"%c\"}", (uintptr_t)cPtr, *cPtr);
            }

            ++cPtr;

            if (*cPtr)
            {
                fprintf(taintAnalysisCallFile, ",\n");
            }
            else
            {
                fprintf(taintAnalysisCallFile, "\n");
            }
        }

        if (x + 1 == argc)
        {
            fprintf(taintAnalysisCallFile, "]}\n");
        }
        else
        {
            fprintf(taintAnalysisCallFile, "]},\n");
        }
    }

    fprintf(taintAnalysisCallFile, "]}\n");
    fflush(stdout);

    noInstrument = false;
}

void tracerllvm_beginTrace()
{
    if (noInstrument) { return; }

    noInstrument = true;

    if (taintAnalysisCallFile == NULL)
    {
        taintAnalysisCallFile = fopen(outputFile, "w");
        if (taintAnalysisCallFile == NULL)
        {
            printf("error: cannot open trace file '%s' for writing\n", outputFile);
            exit(1);
        }
    }

    currentEvent = NULL;
    currentOperands = NULL;
    currentOpts = NULL;

    traceStarted = true;
    ongoingInstr = false;
    noInstrument = false;
}

void tracerllvm_endTrace()
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    if (ongoingInstr)
    {
        tracerllvm_instructionEnd();
    }

    printf("Finished Calculations.\n");
    fclose(taintAnalysisCallFile);

    noInstrument = false;
}

void tracerllvm_instructionEnd()
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    json_object_object_add(currentEvent, "a", currentOperands);

    if (currentOpts != NULL)
    {
        json_object_object_add(currentEvent, "opt", currentOpts);
    }

    fprintf(taintAnalysisCallFile, "%s\n", json_object_to_json_string(currentEvent));

    json_object_put(currentEvent);
    currentEvent    = NULL;
    currentOperands = NULL;
    currentOpts     = NULL;
    ongoingInstr    = false;

    noInstrument = false;
}

void tracerllvm_callInstruction(char* string, char* funName, char* assignedRegister, char* assignedRegType)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    if (ongoingInstr)
    {
        // if necessary close the previous event
        tracerllvm_instructionEnd();
    }

    noInstrument = true;

    currentEvent    = json_object_new_object();
    currentOperands = json_object_new_array();
    currentOpts     = json_object_new_array();

    json_object_object_add(currentEvent, "i", json_object_new_string(string));
    json_object_object_add(currentEvent, "fn", json_object_new_string(funName));
    json_object_object_add(currentEvent, "ar", json_object_new_string(assignedRegister));
    json_object_object_add(currentEvent, "at", json_object_new_string(assignedRegType));

    ongoingInstr = true;
    noInstrument = false;
}

void tracerllvm_instructionHeader(int opcode, char* funName, char* assignedReg, char* assignedRegType)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    if (ongoingInstr)
    {
        // if necessary close the previous event
        tracerllvm_instructionEnd();
    }

    noInstrument = true;

    currentEvent    = json_object_new_object();
    currentOperands = json_object_new_array();

    json_object_object_add(currentEvent, "oc", json_object_new_int(opcode));
    json_object_object_add(currentEvent, "fn", json_object_new_string(funName));
    json_object_object_add(currentEvent, "ar", json_object_new_string(assignedReg));
    json_object_object_add(currentEvent, "at", json_object_new_string(assignedRegType));

    ongoingInstr = true;
    noInstrument = false;
}

void tracerllvm_callOperandInt(char* string, int64_t value, char* type)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    char* value_str;
    if (asprintf(&value_str, "%" PRIu64, (uint64_t)value) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string(string));
    json_object_object_add(operand, "v", json_object_new_string(value_str));
    json_object_object_add(operand, "t", json_object_new_string(type));
    json_object_array_add(currentOperands, operand);

    free(value_str);

    noInstrument = false;
}

void tracerllvm_callOperandFloat(char* string, float value, char* type)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    char* value_str;
    if (asprintf(&value_str, "%f", value) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string(string));
    json_object_object_add(operand, "v", json_object_new_string(value_str));
    json_object_object_add(operand, "t", json_object_new_string(type));
    json_object_array_add(currentOperands, operand);

    free(value_str);

    noInstrument = false;
}

void tracerllvm_callOperandDouble(char* string, double value, char* type)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    char* value_str;
    if (asprintf(&value_str, "%f", value) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string(string));
    json_object_object_add(operand, "v", json_object_new_string(value_str));
    json_object_object_add(operand, "t", json_object_new_string(type));
    json_object_array_add(currentOperands, operand);

    free(value_str);

    noInstrument = false;
}

void tracerllvm_callOperandString(char* string, char* value, char* type)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    char* value_str;
    if (asprintf(&value_str, "%" PRIuPTR " %s", (uintptr_t)value, value) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string(string));
    json_object_object_add(operand, "v", json_object_new_string(value_str));
    json_object_object_add(operand, "t", json_object_new_string(type));
    json_object_array_add(currentOperands, operand);

    free(value_str);

    noInstrument = false;
}

void tracerllvm_callOperandFixedString(char* string, char* value, char* type, int64_t width)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    char* value_str;
    if (asprintf(&value_str, "%" PRIuPTR " %.*s", (uintptr_t)value, (ssize_t)width < 0 ? 0 : (int)width, value) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string(string));
    json_object_object_add(operand, "v", json_object_new_string(value_str));
    json_object_object_add(operand, "t", json_object_new_string(type));
    json_object_array_add(currentOperands, operand);

    free(value_str);

    noInstrument = false;
}

void tracerllvm_callOperandVector(char* string, int64_t* value, int64_t vectorLength, char* type)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    char* tempValue = vectorValues(value, vectorLength);

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string(string));
    json_object_object_add(operand, "v", json_object_new_string(tempValue));
    json_object_object_add(operand, "t", json_object_new_string(type));
    json_object_array_add(currentOperands, operand);

    free(tempValue);

    noInstrument = false;
}

void tracerllvm_filePosition(int64_t file)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    FILE* fileObject = (FILE*)file; // sic, i64 type is just used for interfacing the object on llvm side, has to be casted back here
    filePosition = ftell(fileObject);

    char* file_str;
    if (asprintf(&file_str, "%" PRIu64, (uint64_t)file) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }

    char* position_str;
    if (asprintf(&position_str, "%" PRIi64, filePosition) <= 0)
    {
        printf("error: failed to allocate enough memory for string\n");
        return;
    }


    json_object* operandFile = json_object_new_object();
    json_object_object_add(operandFile, "o", json_object_new_string("fp"));
    json_object_object_add(operandFile, "v", json_object_new_string(file_str));
    json_object_object_add(operandFile, "t", json_object_new_string("i64"));
    json_object_array_add(currentOperands, operandFile);

    json_object* operandPosition = json_object_new_object();
    json_object_object_add(operandPosition, "o", json_object_new_string("fo"));
    json_object_object_add(operandPosition, "v", json_object_new_string(position_str));
    json_object_object_add(operandPosition, "t", json_object_new_string("i64"));
    json_object_array_add(currentOperands, operandPosition);

    free(file_str);
    free(position_str);

    noInstrument = false;
}

/**
 * Uses the old fileposition and the current fileposition to reconstruct the read bytes and print them.
 */
void tracerllvm_fileRawPrint(int64_t file)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    FILE* fileObject = (FILE*)file; // sic, i64 type is just used for interfacing the object on llvm side, has to be casted back here
    int readBytes = ftell(fileObject) - filePosition;

    // write out fileObjectPointer and after the if the actual value of the position
    // allocate more space to replace '"' with '\"'
    char readChars[readBytes + 1];
    readChars[readBytes] = '\0';
    printf("filepos: %" PRIi64 ", readbytes: %u\n", filePosition, readBytes);

    fseek(fileObject, filePosition, SEEK_SET);
    fread(readChars, readBytes, 1, fileObject);

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string("rs"));
    json_object_object_add(operand, "v", json_object_new_string(readChars));
    json_object_object_add(operand, "t", json_object_new_string("i64"));
    json_object_array_add(currentOperands, operand);

    noInstrument = false;
}

void tracerllvm_optInput(uintptr_t source, char* result)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    json_object* input = json_object_new_object();
    json_object_object_add(input, "type",   json_object_new_string("input"));
    json_object_object_add(input, "source", json_object_new_int64(source));
    json_object_object_add(input, "result", json_object_new_string(result));
    json_object_array_add(currentOpts, input);

    noInstrument = false;
}

void tracerllvm_optOpcode(int opcode)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    json_object* opt = json_object_new_object();
    json_object_object_add(opt, "type",  json_object_new_string("opcode"));
    json_object_object_add(opt, "value", json_object_new_int(opcode));
    json_object_array_add(currentOpts, opt);

    noInstrument = false;
}

void tracerllvm_enterBasicBlock(int64_t bbID)
{
    if (!traceStarted) { return; }
    if (noInstrument) { return; }

    noInstrument = true;

    json_object* operand = json_object_new_object();
    json_object_object_add(operand, "o", json_object_new_string("bb"));
    json_object_object_add(operand, "v", json_object_new_int(bbID));
    json_object_object_add(operand, "t", json_object_new_string("i64"));
    json_object_array_add(currentOperands, operand);

    noInstrument = false;
}
