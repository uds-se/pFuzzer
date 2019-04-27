#include <iostream>
#include <fstream>
#include <map>
#include <set>
#include <thread>

#include <llvm/Pass.h>
#include <llvm/IR/Type.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/IntrinsicInst.h>
#include <llvm/IR/InstIterator.h>
#include <llvm/IR/Function.h>
#include <llvm/Support/raw_ostream.h>

#include "operators.h"

using namespace llvm;

#define DEBUG_TYPE "traceplugin"

cl::opt<std::string> InputFilename("exclude_functions",
                                   cl::desc("file containing the names of functions that need to be skipped"),
                                   cl::value_desc("filename"));

struct foperator {
    unsigned int opcode;
    bool         move_stream;
    bool         use_string;

    foperator(unsigned int opcode, bool move_stream, bool use_string)
        : opcode(opcode)
        , move_stream(move_stream)
        , use_string(use_string)
    {
        // Intentionally left blank
    }
};

//counter which is used to assign for each basic block a unique ID
int bbIDCounter = 0;

class Worker
{
private:
    // contains a map from operator name to operator opcode
    std::map<std::string, struct foperator*> additionalOperators;
    std::map<std::string, Value*>& globalStrings;
    std::mutex& builderMutex;

    llvm::Module& M;

public:

    explicit Worker() = delete;

    Worker(Module& M, std::map<std::string, Value*>& globalStrings, std::mutex& builderMutex)
        : additionalOperators()
        , globalStrings(globalStrings)
        , builderMutex(builderMutex)
        , M(M)
    {
        additionalOperators["methodEntry"]     = new foperator(METHODENTRY,  false, false);
        additionalOperators["filePosition"]    = new foperator(FILEPOSITION, true,  false);

        additionalOperators["__isoc99_fscanf"] = new foperator(FSCANF,       true,  true);
        additionalOperators["fscanf"]          = new foperator(FSCANF,       true,  true);

        additionalOperators["fgetc"]           = new foperator(FGETC,        true,  true);
        additionalOperators["fread"]           = new foperator(FREAD,        true,  true);

        additionalOperators["strtol"]          = new foperator(STRTO,        false, true);
        additionalOperators["strtoll"]         = new foperator(STRTO,        false, true);
        additionalOperators["strtoimax"]       = new foperator(STRTO,        false, true);
        additionalOperators["strtoq"]          = new foperator(STRTO,        false, true);
        additionalOperators["strtoul"]         = new foperator(STRTO,        false, true);
        additionalOperators["strtoull"]        = new foperator(STRTO,        false, true);
        additionalOperators["strtoumax"]       = new foperator(STRTO,        false, true);
        additionalOperators["strtouq"]         = new foperator(STRTO,        false, true);
        additionalOperators["strtod"]          = new foperator(STRTO,        false, true);
        additionalOperators["strtof"]          = new foperator(STRTO,        false, true);
        additionalOperators["strtold"]         = new foperator(STRTO,        false, true);

        additionalOperators["\01_strtol"]      = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtoll"]     = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtoimax"]   = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtoq"]      = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtoul"]     = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtoull"]    = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtoumax"]   = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtouq"]     = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtod"]      = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtof"]      = new foperator(STRTO,        false, true);
        additionalOperators["\01_strtold"]     = new foperator(STRTO,        false, true);

        additionalOperators["realloc"]         = new foperator(REALLOC,      false, false);
        additionalOperators["malloc"]          = new foperator(ALLOC,        false, false);
        additionalOperators["calloc"]          = new foperator(ALLOC,        false, false);

        additionalOperators["__strndup"]       = new foperator(STRNDUP,      false, true);
        additionalOperators["strndup"]         = new foperator(STRNDUP,      false, true);

        additionalOperators["__isoc99_sscanf"] = new foperator(SSCANF,       false, true);
        additionalOperators["sscanf"]          = new foperator(SSCANF,       false, true);

        additionalOperators["sprintf"]         = new foperator(SPRINTF,      false, true);
        additionalOperators["read"]            = new foperator(READ,         false, true);

        additionalOperators["strcmp"]          = new foperator(STRCMP,       false, true);
        additionalOperators["strncmp"]         = new foperator(STRCMP,       false, true);

        additionalOperators["getchar"]         = new foperator(GETCHAR,      false, true);
        additionalOperators["bbenter"]         = new foperator(BBENTER,      false, true);
        additionalOperators["strchr"]         = new foperator(STRCHR,      false, true);
    }

    void instrumentFunctions(std::vector<Function*> functions)
    {
        for (auto F: functions)
        {
            builderMutex.lock();
            std::cout << "[INFO] in thread " << std::this_thread::get_id() << ": "
                      << "instrumenting function: " << F->getName().data()
                      << std::endl;
            builderMutex.unlock();

            instrumentFunction(*F);
        }
    }

    /**
     * Get a tracer function by name
     */
    Function* tracer(const std::string& name) const
    {
        return M.getFunction("tracerllvm_" + name);
    }

    std::string toString(const Value* v)
    {
        std::string str;
        llvm::raw_string_ostream rso(str);

        builderMutex.lock();
        v->print(rso);
        builderMutex.unlock();

        return rso.str();
    }

    std::string toString(const Type* t)
    {
        std::string str;
        llvm::raw_string_ostream rso(str);

        builderMutex.lock();
        t->print(rso);
        builderMutex.unlock();

        return rso.str();
    }

    /**
     * Looks up if the global string was created some time before.
     * If yes it uses the reference to the already created string, if not it creates one and stores it in a map for
     * later reuse.
     * @param builder
     * @param str
     * @return
     */
    Value* getGlobalString(IRBuilder<>* builder, const std::string& str)
    {
        builderMutex.lock();
        Value* globstring;
        auto itr = globalStrings.find(str);
        if (itr == globalStrings.end())
        {
            globstring = builder->CreateGlobalStringPtr(str);
            globalStrings.insert(std::pair<std::string, Value*>(str, globstring));
        }
        else
        {
            globstring = itr->second;
        }
        builderMutex.unlock();

        return globstring;
    }

    /**
     * Creates an global string pointer with the name of the operand.
     */
    Value* getOperandName(IRBuilder<>* builder, Value* operand)
    {
        auto name = "Constant";
        if (operand->hasName())
        {
            name = operand->getValueName()->getKeyData();
        }

        return getGlobalString(builder, name);
    }

    /**
     * Creates an global string pointer with the type of the operand.
     */
    Value* getOperandType(IRBuilder<>* builder, Value* operand)
    {
        return getGlobalString(builder, toString(operand->getType()));
    }

    /**
     * Creates a Int64 array out of a vector.
     */
    Value* createArrayFromVector(IRBuilder<>* builder, Value* operand)
    {
        VectorType* vecOperand = cast<llvm::VectorType>(operand->getType());
        // create a sufficiently large array
        builderMutex.lock();
        auto size = builder->getInt64(vecOperand->getVectorNumElements());
        auto temp = builder->CreateAlloca(builder->getInt64Ty(), size);
        builderMutex.unlock();
        for (unsigned int x = 0; x < vecOperand->getVectorNumElements(); ++x)
        {
            // extract one value after another from the vector
            builderMutex.lock();
            auto tempInt = builder->CreateExtractElement(operand, x);
            builderMutex.unlock();

            // convert it to 64 bit
            // output of double and similar currently not supported
            Value* tempInt64;
            if (vecOperand->getElementType()->isIntegerTy())
            {
                builderMutex.lock();
                tempInt64 = builder->CreateZExt(tempInt, builder->getInt64Ty());
                builderMutex.unlock();
            }
            else
            {
                if (vecOperand->getElementType()->getTypeID() == llvm::Type::PointerTyID)
                {
                    PointerType* pTy = dyn_cast<PointerType>(vecOperand->getElementType());
                    if (pTy->getPointerElementType()->isFunctionTy())
                    {
                        builderMutex.lock();
                        tempInt64 = builder->getInt64(-42);
                        builderMutex.unlock();
                    }
                    else
                    {
                        // the operand could be a constant expression (inlined gep for example)
                        // maybe this needs to be handled differently regarding the result
                        builderMutex.lock();
                        tempInt64 = builder->CreatePtrToInt(tempInt, builder->getInt64Ty());
                        builderMutex.unlock();
                    }
                }
                else
                {
                    builderMutex.lock();
                    tempInt64 = builder->getInt64(-42);
                    builderMutex.unlock();
                }
            }

            // store to the array
            builderMutex.lock();
            auto position = builder->CreateGEP(builder->getInt64Ty(), temp, builder->getInt64(x));
            builder->CreateStore(tempInt64, position, false);
            builderMutex.unlock();
        }

        return temp;
    }

    /**
     * Creates converts the operand type to a 64-bit integer.
     */
    Value* getOperandValue(IRBuilder<>* builder, Value* operand)
    {
        // print integer
        if (operand->getType()->isIntegerTy())
        {
            builderMutex.lock();
            auto tmp = builder->CreateZExt(operand, builder->getInt64Ty()); // extend all integers to 64 bit
            builderMutex.unlock();
            return tmp;
        }
        else if (operand->getType()->getTypeID() == llvm::Type::PointerTyID)
        {
            PointerType* pTy = dyn_cast<PointerType>(operand->getType());
            if (pTy->getPointerElementType()->isFunctionTy())
            {
                builderMutex.lock();
                auto tmp = builder->getInt64(-42);
                builderMutex.unlock();
                return tmp;
            }

            builderMutex.lock();
            auto tmp = builder->CreatePtrToInt(operand, builder->getInt64Ty());
            builderMutex.unlock();
            return tmp;
        }
        else if (operand->getType()->getTypeID() == llvm::Type::VectorTyID)
        {
            return createArrayFromVector(builder, operand);
        }
        else
        {
            // print generic value -42
            builderMutex.lock();
            auto tmp = builder->getInt64(-42);
            builderMutex.unlock();
            return tmp;
        }
    }

    /**
     * Creates a Int64 array out of one value.
     */
    Value* createArrayFromOneValue(IRBuilder<>* builder, Value* operand)
    {
        // create a sufficiently large array
        builderMutex.lock();
        auto size = builder->getInt64(1);
        auto temp = builder->CreateAlloca(builder->getInt64Ty(), size);
        builderMutex.unlock();
        // get value for operand
        auto tempInt64 = getOperandValue(builder, operand);
        // store to the array
        builderMutex.lock();
        auto position = builder->CreateGEP(builder->getInt64Ty(), temp, builder->getInt64(0));
        builder->CreateStore(tempInt64, position, false);
        builderMutex.unlock();
        return temp;
    }

    /**
     * Fills the args array with the needed information: Opcode, function name, assigned Register.
     */
    void createStaticArgList(IRBuilder<>* builder, std::vector<Value*>& args, Instruction* instr)
    {
        Value* funName = getGlobalString(builder, instr->getFunction()->getName());
        Value* retName = getGlobalString(builder, instr->getName());
        Value* retType = getGlobalString(builder, toString(instr->getType()));

        builderMutex.lock();
        args.push_back(builder->getInt64(instr->getOpcode()));
        builderMutex.unlock();

        args.push_back(funName);
        args.push_back(retName);
        args.push_back(retType);
    }

    /**
     * Writes operand information to the array at the given position. Fills 3 cells in the array.
     */
    void fillArrayForOperand(IRBuilder<>* builder, std::vector<Value*>& args, Value* operand)
    {
        // operand name
        args.push_back(getOperandName(builder, operand));

        // operand value
        args.push_back(getOperandValue(builder, operand));

        // operand type
        args.push_back(getOperandType(builder, operand));
    }

    /**
     * Writes operand information to the array at the given position. Fills 3 cells in the array.
     */
    void fillArrayForStringOperand(IRBuilder<>* builder, std::vector<Value*>& args, Value* operand)
    {
        // operand name
        args.push_back(getOperandName(builder, operand));

        // string value
        args.push_back(operand);

        // operand type
        args.push_back(getOperandType(builder, operand));
    }

    /**
     * Writes operand information to the array at the given position for vector operand. Fills 4 cells in the array.
     */
    void fillArrayForVectorOperand(IRBuilder<>* builder, std::vector<Value*>& args, Value* operand)
    {
        // operand name
        args.push_back(getOperandName(builder, operand));

        if (operand->getType()->isVectorTy())
        {
            // operand value
            args.push_back(createArrayFromVector(builder, operand));

            // value length
            VectorType* vecOperand = cast<llvm::VectorType>(operand->getType());
            builderMutex.lock();
            args.push_back(builder->getInt64(vecOperand->getVectorNumElements()));
            builderMutex.unlock();
        }
        else
        {
            // operand value
            args.push_back(createArrayFromOneValue(builder, operand));

            // value length
            builderMutex.lock();
            args.push_back(builder->getInt64(1));
            builderMutex.unlock();
        }

        // operand type
        args.push_back(getOperandType(builder, operand));
    }

    /**
     * Checks if the instruction contains an operand with a vector.
     */
    int containsVectorType(Instruction* instr)
    {
        int ret = 0;
        for (unsigned int x = 0; x < instr->getNumOperands(); ++x)
        {
            ret |= instr->getOperand(x)->getType()->isVectorTy();
        }

        return ret;
    }

    // ExtractValueInst and InsertValueInst have indices which are operands but not stored
    // as operands in the llvm class so they must be handled separately.
    void createIndicesCalls(IRBuilder<>* builder, ArrayRef<unsigned> indices)
    {
        for (unsigned int index : indices)
        {
            std::vector<Value*> args;

            builderMutex.lock();
            auto indexValue = builder->getInt64(index);
            builderMutex.unlock();

            fillArrayForOperand(builder, args, indexValue);

            builderMutex.lock();
            builder->CreateCall(tracer("callOperandInt"), args);
            builderMutex.unlock();
        }
    }

    // comparison (ICMP, FCMP) have a third operand which is the type of comparison (predicate)
    // it is not stored as operand in the llvm class so it must be handled separately
    void createPredicatesCalls(IRBuilder<>* builder, CmpInst::Predicate predicate)
    {
        std::vector<Value*> args;

        builderMutex.lock();
        auto predicateValue = builder->getInt64(predicate);
        builderMutex.unlock();

        fillArrayForOperand(builder, args, predicateValue);

        builderMutex.lock();
        builder->CreateCall(tracer("callOperandInt"), args);
        builderMutex.unlock();
    }

    /**
     * Generates analyzer calls to print information for a function call, the function Name has to be given since some functions
     * do not provide a name.
     */
    void printFunctionCall(IRBuilder<>* builder, Instruction* instr)
    {
        Value* instructionString = getGlobalString(builder, toString(instr));
        Value* funName = getGlobalString(builder, instr->getFunction()->getName());
        Value* retName = getGlobalString(builder, instr->getName());
        Value* retType = getGlobalString(builder, toString(instr->getType()));

        // print the string representation of the instruction
        builderMutex.lock();
        builder->CreateCall(tracer("callInstruction"), {instructionString, funName, retName, retType});
        builderMutex.unlock();

        // operands
        for (unsigned int x = 0; x < instr->getNumOperands(); ++x)
        {
            std::vector<Value*> args;
            auto operand = instr->getOperand(x);
            if (containsVectorType(instr))
            {
                fillArrayForVectorOperand(builder, args, operand);

                instr->dump();
                builderMutex.lock();
                builder->CreateCall(tracer("callOperandVector"), args);
                builderMutex.unlock();
            }
            else
            {
                fillArrayForOperand(builder, args, operand);

                builderMutex.lock();
                builder->CreateCall(tracer("callOperandInt"), args);
                builderMutex.unlock();
            }
        }

        // NOTE: We don't close the event with instructionEnd() because
        //       it might be a call to a wrapped function. The wrapper
        //       will want to add extra data to this call event.
    }

    /**
     * Creates a call which prints the current position of the file in bytes.
     */
    void createFilePositionCall(IRBuilder<>* builder, Instruction* instr)
    {
        Value* funName = getGlobalString(builder, instr->getFunction()->getName());
        Value* retName = getGlobalString(builder, instr->getName());
        Value* retType = getGlobalString(builder, toString(instr->getType()));

        builderMutex.lock();
        auto opcode = builder->getInt64(additionalOperators.at("filePosition")->opcode);
        builder->CreateCall(tracer("instructionHeader"), {opcode, funName, retName, retType});
        builderMutex.unlock();

        // look for the function pointer in the operands
        for (unsigned int x = 0; x < instr->getNumOperands(); ++x)
        {
            auto operand = instr->getOperand(x);

            // check if type is file pointer
            std::string operandType = toString(operand->getType());
            if (operandType == "%struct._IO_FILE*" || operandType == "%struct.__sFILE*")
            {
                builderMutex.lock();
                auto ptr = builder->CreatePtrToInt(operand, builder->getInt64Ty());
                builder->CreateCall(tracer("filePosition"), {ptr});
                builderMutex.unlock();
                break;
            }
        }

        // call to print instruction end, again the function version is sufficient here
        builderMutex.lock();
        builder->CreateCall(tracer("instructionEnd"), {});
        builderMutex.unlock();
    }

    /**
     * Creates calls for I/O and string manipulation. The calls are put behind the instruction.
     * When printStrings is true, i8* are assumed to be strings and the strings are printed in the trace.
     */
    void createFileHandlingCalls(IRBuilder<>* builder, Instruction* instr, const std::string& funNameString, bool printStrings)
    {
        Value* funName = getGlobalString(builder, instr->getFunction()->getName());
        Value* retName = getGlobalString(builder, instr->getName());
        Value* retType = getGlobalString(builder, toString(instr->getType()));

        builderMutex.lock();
        auto opcode = builder->getInt64(additionalOperators.at(funNameString)->opcode);
        builder->CreateCall(tracer("instructionHeader"), {opcode, funName, retName, retType});
        builderMutex.unlock();

        PointerType* pTy = dyn_cast<PointerType>(instr->getType());
        if (printStrings && pTy != nullptr && pTy->getElementType()->isIntegerTy() && pTy->getElementType()->getPrimitiveSizeInBits() == 8)
        {
            std::vector<Value*> args;
            fillArrayForStringOperand(builder, args, instr);
            builderMutex.lock();
            builder->CreateCall(tracer("callOperandString"), args);
            builderMutex.unlock();
        }
        else
        {
            std::vector<Value*> args;
            fillArrayForOperand(builder, args, instr);
            builderMutex.lock();
            builder->CreateCall(tracer("callOperandInt"), args);
            builderMutex.unlock();
        }

        // operands, the function calls can be used here, since they provide for operands the functionality that is needed
        for (unsigned int x = 0; x < instr->getNumOperands(); ++x)
        {
            std::vector<Value*> args;
            auto operand = instr->getOperand(x);
            PointerType* pTy = dyn_cast<PointerType>(operand->getType());
            if (printStrings && pTy != nullptr && pTy->getElementType()->isIntegerTy() && pTy->getElementType()->getPrimitiveSizeInBits() == 8)
            {
                fillArrayForStringOperand(builder, args, operand);

                if (funNameString == "read")
                {
                    args.push_back(getOperandValue(builder, instr));
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandFixedString"), args);
                    builderMutex.unlock();
                }
                else if (funNameString == "strncmp")
                {
                    args.push_back(getOperandValue(builder, instr->getOperand(2)));
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandFixedString"), args);
                    builderMutex.unlock();
                }
                else
                {
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandString"), args);
                    builderMutex.unlock();
                }
            }
            else
            {
                fillArrayForOperand(builder, args, operand);
                if (operand->getType()->isFloatTy())
                {
                    args[1] = operand;
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandFloat"), args);
                    builderMutex.unlock();
                }
                else if (operand->getType()->isDoubleTy())
                {
                    args[1] = operand;
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandDouble"), args);
                    builderMutex.unlock();
                }
                else
                {
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandInt"), args);
                    builderMutex.unlock();
                }

                // check if type is file pointer
                std::string operandType = toString(operand->getType());
                if (operandType == "%struct._IO_FILE*" || operandType == "%struct.__sFILE*")
                {
                    builderMutex.lock();
                    auto ptr = builder->CreatePtrToInt(operand, builder->getInt64Ty());
                    builderMutex.unlock();
                    if (funNameString == "__isoc99_fscanf" || funNameString == "fscanf")
                    {
                        std::cout << "print rawstring for fscanf\n";
                        builderMutex.lock();
                        builder->CreateCall(tracer("fileRawPrint"), {ptr});
                        builderMutex.unlock();
                    }

                    builderMutex.lock();
                    builder->CreateCall(tracer("filePosition"), {ptr});
                    builderMutex.unlock();
                }
            }
        }

        // call to print instruction end, again the function version is sufficient here
        builderMutex.lock();
        builder->CreateCall(tracer("instructionEnd"), {});
        builderMutex.unlock();
    }

    bool instrumentWrappedFunction(IRBuilder<>* builder, IRBuilder<>* nextInstructionBuilder, Instruction* instr, const std::string& funNameString)
    {
        auto funOperator = additionalOperators.find(funNameString);
        if (funOperator == additionalOperators.end())
        {
            // not a wrapped function -> nothing to do
            return false;
        }

        if (funOperator->second->move_stream)
        {
            createFilePositionCall(builder, instr);
        }

        createFileHandlingCalls(nextInstructionBuilder, instr, funNameString, funOperator->second->use_string);
        return true;
    }

    void instrumentInstr(Instruction* instr, IRBuilder<>* builder, IRBuilder<>* nextInstructionBuilder)
    {
        // Call instructions are handled differently
        if (CallInst* callinst = dyn_cast<CallInst>(instr))
        {
            Function* fun = callinst->getCalledFunction();
            if (fun != nullptr && fun->isIntrinsic() && !dyn_cast<MemCpyInst>(callinst))
            {
                // skip llvm intrinsic functions other than llvm.memcpy
                return;
            }

            std::string calledFunctionName;
            if (fun != nullptr)
            {
                calledFunctionName = fun->getName().str();
            }
            else
            {
                // called function might be unknown when using casts or indirect calls (through pointer)

                // try harder to find the function name by removing casts and aliases
                calledFunctionName = callinst->getCalledValue()->stripPointerCasts()->getName();
                if (calledFunctionName.empty())
                {
                    // as a last resort use 'null' for the name
                    calledFunctionName = "null";
                }
            }

            if (instrumentWrappedFunction(builder, nextInstructionBuilder, instr, calledFunctionName))
            {
                return;
            }

            printFunctionCall(builder, instr);
        }
        else
        {
            std::vector<Value*> args;
            createStaticArgList(builder, args, instr);
            builderMutex.lock();
            builder->CreateCall(tracer("instructionHeader"), args);
            builderMutex.unlock();

            for (Value* operand : instr->operands())
            {
                std::vector<Value*> args;
                if (operand->getType()->isVectorTy())
                {
                    fillArrayForVectorOperand(builder, args, operand);
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandVector"), args);
                    builderMutex.unlock();
                }
                else
                {
                    fillArrayForOperand(builder, args, operand);
                    builderMutex.lock();
                    builder->CreateCall(tracer("callOperandInt"), args);
                    builderMutex.unlock();
                }
            }

            if (InsertValueInst* insertinst = dyn_cast<InsertValueInst>(instr))
            {
                createIndicesCalls(builder, insertinst->getIndices());
            }

            if (ExtractValueInst* extractinst = dyn_cast<ExtractValueInst>(instr))
            {
                createIndicesCalls(builder, extractinst->getIndices());
            }

            if (CmpInst* cmpinst = dyn_cast<CmpInst>(instr))
            {
                createPredicatesCalls(builder, cmpinst->getPredicate());
            }

            builderMutex.lock();
            builder->CreateCall(tracer("instructionEnd"), {});
            builderMutex.unlock();
            return;
        }
    }

    /**
     * Use an arbitrary instruction and just change the opcode, this way no additional code has to be written and
     * the already existing call generation can be used.
     */
    void createMethodEntryCall(IRBuilder<>* builder, unsigned int opcode, Instruction* instr)
    {
        std::vector<Value*> args;

        createStaticArgList(builder, args, instr);

        builderMutex.lock();
        args[0] = builder->getInt64(opcode);

        builder->CreateCall(tracer("instructionHeader"), args);
        builder->CreateCall(tracer("instructionEnd"), {});
        builderMutex.unlock();
    }

    bool instrumentFunction(Function& F)
    {
        std::vector<Instruction*> toInstrument;
        for (BasicBlock& bb : F)
        {
            auto first_insertion_point = bb.getFirstInsertionPt();

            IRBuilder<> builder(&bb, first_insertion_point);

            std::vector<Value*> iHeaderArgs;
            createStaticArgList(&builder, iHeaderArgs, &*first_insertion_point);
            iHeaderArgs[0] = builder.getInt64(additionalOperators.at("bbenter")->opcode);
            builderMutex.lock();
            builder.CreateCall(tracer("instructionHeader"), iHeaderArgs);
            std::vector<Value*> bbEnterArgs;
            bbEnterArgs.push_back(builder.getInt64(bbIDCounter++));
            builder.CreateCall(tracer("enterBasicBlock"), bbEnterArgs);
            builder.CreateCall(tracer("instructionEnd"), {});
            builderMutex.unlock();

            for (BasicBlock::iterator itr_bb = first_insertion_point; itr_bb != bb.end(); ++itr_bb)
            {
                toInstrument.push_back(&*itr_bb);
            }
        }

        for (Instruction* instr : toInstrument)
        {
            BasicBlock::iterator itr_bb(instr);
            IRBuilder<> builder(instr->getParent(), itr_bb);
            IRBuilder<> nextInstructionBuilder(instr->getParent(), std::next(itr_bb, 1));
            instrumentInstr(instr, &builder, &nextInstructionBuilder);
        }

        // generate for each function at the beginning a call with an artificial operator such that the taint propagation is aware of a newly called function
        /*
         * This is needed because:
         * If a function is called by its pointer, then the name of the called function is not known at compile time.
         * If such a function calls itself recursively with this method, there will be no change in the name of the surrounding function for
         * subsequent instructions, i.e. it cannot be distinguished whether a function was called which is not instrumented or whether a dereferenced
         * function was called recursively. An explicit call at the beginning of each method makes it possible to distinguish this.
         */
        if (F.getName() != "_real_program_main")
        {
            Instruction* inst = &*(inst_begin(F));
            IRBuilder<> builder(inst);
            createMethodEntryCall(&builder, additionalOperators["methodEntry"]->opcode, inst);
        }

        return true;
    }
};

struct TaintTrackPlugin : public ModulePass
{
    static char ID; // Pass identification, replacement for typeid
    TaintTrackPlugin() : ModulePass(ID) {}

    bool runOnModule(Module& M)
    {
        auto& llvm_context = M.getContext();
        auto I64Ty = Type::getInt64Ty(llvm_context);
        auto I64PtrTy = Type::getInt64PtrTy(llvm_context);
        auto I8PtrTy = Type::getInt8PtrTy(llvm_context);
        auto I8PtrPtrTy = PointerType::get(I8PtrTy, 0);
        auto VoidTy = Type::getVoidTy(llvm_context);
        auto FloatTy = Type::getFloatTy(llvm_context);
        auto DoubleTy = Type::getDoubleTy(llvm_context);

        // ensure that we have all tracer functions available in the llvm module
        M.getOrInsertFunction("tracerllvm_argcargv", VoidTy, I64Ty, I8PtrPtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_callOperandInt", VoidTy, I8PtrTy, I64Ty, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_callOperandFloat", VoidTy, I8PtrTy, FloatTy, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_callOperandDouble", VoidTy, I8PtrTy, DoubleTy, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_callOperandString", VoidTy, I8PtrTy, I8PtrTy, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_callOperandFixedString", VoidTy, I8PtrTy, I8PtrTy, I8PtrTy, I64Ty, nullptr);
        M.getOrInsertFunction("tracerllvm_callOperandVector", VoidTy, I8PtrTy, I64PtrTy, I64Ty, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_callInstruction", VoidTy, I8PtrTy, I8PtrTy, I8PtrTy, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_instructionHeader", VoidTy, I64Ty, I8PtrTy, I8PtrTy, I8PtrTy, nullptr);
        M.getOrInsertFunction("tracerllvm_instructionEnd", VoidTy, nullptr);
        M.getOrInsertFunction("tracerllvm_filePosition", VoidTy, I64Ty, nullptr);
        M.getOrInsertFunction("tracerllvm_fileRawPrint", VoidTy, I64Ty, nullptr);
        M.getOrInsertFunction("tracerllvm_beginTrace", VoidTy, nullptr);
        M.getOrInsertFunction("tracerllvm_endTrace", VoidTy, nullptr);
        M.getOrInsertFunction("tracerllvm_enterBasicBlock", VoidTy, I64Ty, nullptr);

        // read functions to skip
        std::set<std::string> functionsToSkip;
        if (!InputFilename.empty())
        {
            std::cout << "[INFO] reading excluded functions from file: " << InputFilename << std::endl;

            std::ifstream infile(InputFilename);
            std::string line;
            while (std::getline(infile, line))
            {
                functionsToSkip.insert(line);
            }
        }

        // fill Constant string to global string map
        IRBuilder<> builder((&*(*M.begin()).begin()));
        std::map<std::string, Value*> globalStrings;
        globalStrings.insert(std::pair<std::string, Value*>("Constant", builder.CreateGlobalStringPtr("Constant")));
        globalStrings.insert(std::pair<std::string, Value*>("null", builder.CreateGlobalStringPtr("null")));
        globalStrings.insert(std::pair<std::string, Value*>("void", builder.CreateGlobalStringPtr("void")));

        Function* programMain = M.getFunction("main");
        if (programMain != nullptr)
        {
            if (programMain->arg_empty())
            {
                // normalize int main(void) function to "int main(int, char**)"
                new Argument(programMain->getReturnType(), "argc", programMain);
                new Argument(I8PtrPtrTy, "argv", programMain);
            }

            programMain->setName("_real_program_main");
        }

        std::mutex builderMutex;
        unsigned int concurrentThreadsSupported = ceil(std::thread::hardware_concurrency() * 3);
        std::cout << "[INFO] number of threads: " << concurrentThreadsSupported << std::endl;

        std::vector<std::vector<Function*>> threadFunctions(concurrentThreadsSupported);
        auto i = 0;
        for (Function& f : M.functions())
        {
            if (f.isDeclaration())
            {
                continue;
            }

            if (f.getName().startswith("tracerllvm_"))
            {
                continue;
            }

            if (functionsToSkip.find(f.getName()) != functionsToSkip.end())
            {
                continue;
            }

            threadFunctions[i % concurrentThreadsSupported].push_back(&f);
            ++i;
        }

        std::vector<std::thread> threads;
        for (auto& functions : threadFunctions)
        {
            threads.push_back(std::thread(&Worker::instrumentFunctions, new Worker(M, globalStrings, builderMutex), functions));
        }

        for (auto& thread : threads)
        {
            thread.join();
        }

        return true;
    }
};

char TaintTrackPlugin::ID = 0;
static RegisterPass<TaintTrackPlugin> X("traceplugin", "Plugin to write out traces for taint tracking.");
