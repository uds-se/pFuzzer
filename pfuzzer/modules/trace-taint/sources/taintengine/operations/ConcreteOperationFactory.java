package taintengine.operations;

import taintengine.operations.interfaces.OperationFactory;
import utils.LineInformation;

import javax.sound.sampled.Line;

public class ConcreteOperationFactory implements OperationFactory {
    @Override
    public Load load(LineInformation info) {
        return new Load(info);
    }

    @Override
    public Store store(LineInformation info) {
        return new Store(info);
    }

    @Override
    public Br br(LineInformation info) {
        return new Br(info);
    }

    @Override
    public GenericOperation genericOperation(LineInformation info) {
        return new GenericOperation(info);
    }

    @Override
    public BinOperation binOperation(LineInformation info) {
        return new BinOperation(info);
    }

    @Override
    public ByteBinOp byteBinop(LineInformation info) {
        return new ByteBinOp(info);
    }

    @Override
    public Operation call(LineInformation info) {
        // resolve intrinsics that are important for taint propagation
        String instruction = info.getInstruction();
        if (instruction.contains("@llvm.memcpy") || instruction.contains("@memcpy")) {
            return new MemCopy(info);
        }
        if (instruction.contains("@strcpy(")) {
            return new StrCopy(info);
        }
        if (instruction.contains("@strncpy(")) {
            return new StrNCopy(info);
        }

        if (instruction.contains("@malloc") || instruction.contains("@calloc")) {
            return new Alloc(info);
        }
        if (instruction.contains("@free")) {
            return new Free(info);
        }
        if (instruction.contains("@strlen")) {
            return new StrLen(info);
        }
        if (instruction.contains("@isspace(")) {
            return new IsSpace(info);
        }

        // LLVM creates a method call to "ret void" for function with void type
        // this can be handled as a normal return instruction
        if (instruction.contains("ret void")) {
            return new ReturnInst(info);
        }

        return new Call(info);
    }

    @Override
    public ReturnInst returnInst(LineInformation info) {
        return new ReturnInst(info);
    }

    @Override
    public Select select(LineInformation info) {
        return new Select(info);
    }

    @Override
    public SchuffleVector shufflevector(LineInformation info) {
        return new SchuffleVector(info);
    }

    @Override
    public TypeChange typeChange(LineInformation info) {
        return new TypeChange(info);
    }

    @Override
    public Operation extractElement(LineInformation info) {
        return new ExtractElement(info);
    }

    @Override
    public Operation shiftLeft(LineInformation info) {
        return new ShiftLeft(info);
    }

    @Override
    public Operation logicalShiftRight(LineInformation info) {
        return new LogicalShiftRight(info);
    }

    @Override
    public Operation arithmeticShiftRight(LineInformation info) {
        return new ArithmeticShiftRight(info);
    }

    @Override
    public Operation signExtension(LineInformation info) {
        return new SExt(info);
    }

    @Override
    public Operation getElementPointer(LineInformation info) {
        return new GetElementPointer(info);
    }

    @Override
    public Operation alloca(LineInformation info) {
        return new Alloca(info);
    }

    @Override
    public Operation insertvalue(LineInformation info) {
        return new InsertValue(info);
    }

    @Override
    public Operation extractvalue(LineInformation info) {
        return new ExtractValue(info);
    }

    @Override
    public Operation trunc(LineInformation info) {
        return new Trunc(info);
    }

    @Override
    public Operation methodEntry(LineInformation info) {
        return new MethodEntry(info);
    }

    @Override
    public Operation insertElement(LineInformation info) {
        return new InsertElement(info);
    }

    @Override
    public Operation isoc99FScanf(LineInformation info) {
        //		System.out.println(info);
        return new Isoc99FScanf(info);
    }

    @Override
    public Operation fgets(LineInformation info) {
        //		System.out.println(info);
        return new FGets(info);
    }

    @Override
    public Operation fgetc(LineInformation info) {
        //		System.out.println(info);
        return new FGetc(info);
    }

    @Override
    public Operation fread(LineInformation info) {
        //		System.out.println(info);
        return new FRead(info);
    }

    @Override
    public Operation filePosition(LineInformation info) {
        return new FilePosition(info);
    }

    @Override
    public Operation strTo(LineInformation info) {
        return new StrTo(info);
    }

    @Override
    public Operation realloc(LineInformation info) {
        return new Realloc(info);
    }

    @Override
    public Operation alloc(LineInformation info) {
        return new Alloc(info);
    }

    @Override
    public Operation strndup(LineInformation info) {
        return new StrnDup(info);
    }

    @Override
    public Operation isoc99Sscanf(LineInformation info) {
        return new Isoc99SScanf(info);
    }

    @Override
    public Operation sprintf(LineInformation info) {
        return new Sprintf(info);
    }

    @Override
    public Operation read(LineInformation info) {
        return new Read(info);
    }

    @Override
    public Operation strcmp(LineInformation info) {
        return new Strcmp(info);
    }

    @Override
    public Operation switchstmt(LineInformation info) {
        return new Switch(info);
    }

    @Override
    public Operation getchar(LineInformation info) {
        return new Getchar(info);
    }

    @Override
    public Operation bbEnter(LineInformation info) {
        return new BasicBlockEnter(info);
    }

    @Override
    public Operation strchr(LineInformation info) {
        return new StrChr(info);
    }

    @Override
    public Operation getOperation(LineInformation info) {
        switch (info.getOpcode()) {
        // binops (for the tainting semantics):
        case ICMP:
        case FCMP:
        case ADD:
        case FADD:
        case SUB:
        case FSUB:
        case MUL:
        case FMUL:
        case UDIV:
        case SDIV:
        case FDIV:
        case UREM:
        case SREM:
        case FREM:
            return binOperation(info);

        case PTRTOINT:
        case INTTOPTR:
        case SITOFP:
        case FPEXT:
        case FPTRUNC:
        case FPTOSI:
        case FPTOUI:
        case UITOFP:
            return genericOperation(info);

        // bytewise binop
        case AND:
        case OR:
        case XOR:
            return byteBinop(info);

        case SHL:
            return shiftLeft(info);

        case LSHR:
            return logicalShiftRight(info);

        case ASHR:
            return arithmeticShiftRight(info);

        // typechanges
        case SEXT:
            return signExtension(info);

        case ZEXT:
        case BITCAST:
            return typeChange(info);

        case GETELEMENTPTR:
            return getElementPointer(info);

        case LOAD:
            return load(info);

        case STORE:
            return store(info);

        case BRANCH:
            return br(info);

        case CALL:
            return call(info);

        case RETURN:
            return returnInst(info);

        case SELECT:
            return select(info);

        case SHUFFLEVECTOR:
            return shufflevector(info);

        case EXTRACTELEMENT:
            return extractElement(info);

        case INSERTELEMENT:
            return insertElement(info);

        case ALLOCA:
            return alloca(info);

        case INSERTVALUE:
            return insertvalue(info);

        case EXTRACTVALUE:
            return extractvalue(info);

        case TRUNC:
            return trunc(info);

        case METHODENTRY:
            return methodEntry(info);

        case __ISO99_FSCANF:
            return isoc99FScanf(info);

        case FGETS:
            return fgets(info);

        case FGETC:
            return fgetc(info);

        case FREAD:
            return fread(info);

        case FILEPOSITION:
            return filePosition(info);

        case STRTO:
            return strTo(info);

        case REALLOC:
            return realloc(info);

        case ALLOC:
            return alloc(info);

        case STRNDUP:
            return strndup(info);

        case __ISOC99_SSCANF:
            return isoc99Sscanf(info);

        case SPRINTF:
            return sprintf(info);

        case READ:
            return read(info);

        case STRCMP:
            return strcmp(info);

        case SWITCH:
            return switchstmt(info);

        case GETCHAR:
            return getchar(info);

        case BASICBLOCKENTER:
            return bbEnter(info);

        case STRCHR:
            return strchr(info);

        default:
            throw new UnsupportedOperationException("For line : " + info + "\nthe operator was not yet defined.");
        }
    }

}
