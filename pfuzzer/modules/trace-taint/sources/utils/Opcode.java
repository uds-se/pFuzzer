package utils;

public enum Opcode {
    // Terminator Instructions
    RETURN,
    BRANCH,
    SWITCH,
    INDIRECTBRANCH,
    INVOKE,
    RESUME,
    UNREACHABLEINSTRUCTION,
    CLEANUPRETURNINSTRUCTION,
    CATCHRETURNINSTRUCTION,
    CATCHSWITCHINSTRUCTION,

    // Standard binary operators
    ADD,
    FADD,
    SUB,
    FSUB,
    MUL,
    FMUL,
    UDIV,
    SDIV,
    FDIV,
    UREM,
    SREM,
    FREM,

    // logical operators
    SHL,
    LSHR,
    ASHR,
    AND,
    OR,
    XOR,

    // memory instructions
    ALLOCA,
    LOAD,
    STORE,
    GETELEMENTPTR,
    FENCE,
    ATOMICCMPXCHG,
    ATOMICRMW,

    // cast operators
    TRUNC,
    ZEXT,
    SEXT,
    FPTOUI,
    FPTOSI,
    UITOFP,
    SITOFP,
    FPTRUNC,
    FPEXT,
    PTRTOINT,
    INTTOPTR,
    BITCAST,
    ADDRSPACECAST,
    CLEANUPPAD,
    CATCHPAD,

    // other operators
    ICMP,
    FCMP,
    PHI,
    CALL,
    SELECT,
    USEROP1,
    USEROP2,
    VAARG,
    EXTRACTELEMENT,
    INSERTELEMENT,
    SHUFFLEVECTOR,
    EXTRACTVALUE,
    INSERTVALUE,
    LANDINGPAD,

    // selfmade operators
    METHODENTRY,
    FILEPOSITION,
    __ISO99_FSCANF,
    FGETS,
    FGETC,
    FREAD,
    STRTO,
    REALLOC,
    ALLOC, // represents malloc and calloc
    STRNDUP,
    __ISOC99_SSCANF,
    SPRINTF,
    READ,
    STRCMP,
    GETCHAR,
    BASICBLOCKENTER,
    STRCHR
}
