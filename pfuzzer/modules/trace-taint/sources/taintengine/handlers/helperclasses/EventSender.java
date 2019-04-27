package taintengine.handlers.helperclasses;

import java.io.IOException;
import java.util.List;

import taintengine.Taint;
import taintengine.TaintVector;

public interface EventSender {
    /**
     * Close the EventSender output stream
     */
    void close() throws IOException;

    /**
     * Sends a FieldAccessEvent: EventCode, StructureName, FieldName, IsWrite, NumTaintElements, [NumBytes, [SourceID, NumBits, [Bits]*]*]*
     */
    void fieldAccess(String structureName, String fieldName, boolean isWrite, TaintVector taints) throws IOException;

    /**
     * Sends a MethodEnterEvent: EventCode, MethodName, NumOperands, [NumTaintElements, [NumBytes, [SourceID, NumBits, [Bits]*]*]*]*
     */
    void methodEnter(String methodName, TaintVector[] argumentTaints) throws IOException;

    /**
     * Sends a MethodExitEvent: EventCode, MethodName, NumTaintElements, [NumBytes, [SourceID, NumBits, [Bits]*]*]*
     */
    void methodExit(String methodName, TaintVector returnTaint) throws IOException;

    /**
     * Sends a ArrayWriteEvent: EventCode, Index, IdentitiyCode(long), NumTaintElements(for value), [NumBytes, [SourceID, NumBits, [Bits]*]*]*,
     * 		NumTaintElements(for index), [NumBytes, [SourceID, NumBits, [Bits]*]*]*
     */
    void arrayWrite(long index, long identCode, TaintVector valueTaint, TaintVector indexTaint) throws IOException;

    /**
     * Sends a ArrayReadEvent:EventCode, Index, IdentitiyCode(long), NumTaintElements(for value), [NumBytes, [SourceID, NumBits, [Bits]*]*]*,
     * 		NumTaintElements(for index), [NumBytes, [SourceID, NumBits, [Bits]*]*]*
     */
    void arrayRead(long index, long identCode, TaintVector valueTaint, TaintVector indexTaint) throws IOException;

    /**
     * Sends a BranchEvent: EventCode, NumTaintElements, [NumBytes, [SourceID, NumBits, [Bits]*]*]*
     */
    void branch(TaintVector branchTaint) throws IOException;

    /**
     * Sends a BinOperationEvent
     */
    void binOperation(Integer operatorVal, String op1Val, String op2Val, TaintVector resTaint, TaintVector op1Taint, TaintVector op2Taint) throws IOException;

    /**
     * Sends a StrcmpEvent
     */
    void strcmp(String str1, String str2, Taint str1Taint, Taint str2Taint) throws IOException;

    /**
     * Sends switch
     */
    void swtch(String str1, String[] str2, Taint str1Taint) throws IOException;

    /**
     * Sends strchr
     * @param str1 the ordinal value of the char that is searched in string representation
     * @param str2 the string in which is searched
     * @param str1Taint the taint of str1
     */
    void strchr(String str1, String str2, Taint str1Taint) throws IOException;

    /**
     * Sends conversion operation (like strto*)
     * @param str1 the string or char used in the conversion operation
     * @param conversionFunction function used for conversion
     * @param str1Taint the taint of str1
     */
    void conversion(String str1, String conversionFunction, Taint str1Taint) throws IOException;

    /**
     * Sends basic block entering event
     * @param id id of basic block
     */
    void bbEnter(int id) throws IOException;
}
