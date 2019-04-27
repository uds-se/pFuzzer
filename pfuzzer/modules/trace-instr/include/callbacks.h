#ifndef TRACEINSTR_CALLBACKS_H
#define TRACEINSTR_CALLBACKS_H

#include <inttypes.h>

void tracerllvm_argcargv(int argc, char** argv);

void tracerllvm_beginTrace();
void tracerllvm_endTrace();

void tracerllvm_instructionHeader(int opcode, char* funName, char* assignedReg, char* assignedRegType);
void tracerllvm_instructionEnd();

void tracerllvm_callInstruction(char* string, char* funName, char* assignedRegister, char* assignedRegType);
void tracerllvm_callOperandInt(char* string, int64_t value, char* type);
void tracerllvm_callOperandFloat(char* string, float value, char* type);
void tracerllvm_callOperandDouble(char* string, double value, char* type);
void tracerllvm_callOperandString(char* string, char* value, char* type);
void tracerllvm_callOperandFixedString(char* string, char* value, char* type, int64_t width);
void tracerllvm_callOperandVector(char* string, int64_t* value, int64_t vectorLength, char* type);
void tracerllvm_filePosition(int64_t file);
void tracerllvm_fileRawPrint(int64_t file);

void tracerllvm_optInput(uintptr_t source, char* result);
void tracerllvm_optOpcode(int opcode);
void tracerllvm_enterBasicBlock(int64_t file);

#endif
