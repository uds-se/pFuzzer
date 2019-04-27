package utils;

import java.util.Map;
import java.util.HashMap;

public class Utils {
    // call objects start with this string as name
    public static final String CALLOBJECTSTARTER = "i";

    // call instruction opcode
    public static final int CALLINSTRUCTIONOPCODE = 54;

    // the seperator between different values
    public static final String SEPERATOR = "\t";

    // the number of lines per instruction containing taint information
    public static final int INSTRUCTIONTRACELINES = 3;

    // the length of the line containing the linenumber must be the given value
    public static final int LINENUMBERLINELENGTH = 4;

    // the length of the line containing the instrumented instruction must be the given value
    public static final int INSTRUMENTEDINSTRUCTIONLINELENGTH = 4;

    // the length of the line containing the operand information must be at least the given value
    public static final int OPERANDLINELENGTH = 4;

    // the length of the line containing the information about the program arguments of the program under test
    public static final int ARGUMENTLINELENGTH = 3;

    // the string which shows, that the next element of the array begins
    public static final String ARGUMENTNEWELEMENT = "NewArrayElement";

    // the number of different informations per operand (e.g. name, value, type)
    public static final int INFORMATIONPEROPERAND = 3;

    // Name of the logger to store the taint information in a file.
    public static final String TAINTINFORMATIONLOGGER = "TaintLogger";

    // Name of the logger to store the taint information in a file.
    public static final String DEBUGLOGGER = "DebugLogger";

    // Byte size of a double
    public static final int DOUBLEBYTESIZE = 8;

    // Byte size of a float
    public static final int FLOATBYTESIZE = 4;

    // byte size of a half
    public static final int HALFBYTESIZE = 2;

    // byte size of a void
    public static final int VOIDBYTESIZE = 0;

    // size of a pointer
    // the size may be system dependant. May later be read from metadata which is generated for each program.
    public static final int POINTERBYTESIZE = 4;

    // size of x85_fp80
    public static final int I86_FP80SIZE = 10;

    // Map from structname to size in bits
    public static final Map<String, Integer> SIZEMAP = new HashMap<String, Integer>();

    public static final int STDINFILEMAPPER = 0;

    //this constant is used to define how long a keyword could be
    public static final int KEYWORDLENGTH = 20;

    // Flag for taint propagation over array indices: If on and the index of a load operation is tainted,
    // the read value is also tainted
    private static boolean arrayIndexTaintPropagation;

    public static boolean isArrayIndexTaintPropagation() {
        return arrayIndexTaintPropagation;
    }

    public static void setArrayIndexTaintPropagation(boolean arrayTaintProp) {
        Utils.arrayIndexTaintPropagation = arrayTaintProp;
    }
}
