package taintengine.handlers.helperclasses;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.AutoGramEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map.Entry;

public class MessagePackEventSender implements EventSender {
    private final MessagePacker messagePacker;

    /**
     * Generates a EventSender which uses messagePack and writes to the given outputfile
     * @param outputFilename
     */
    public MessagePackEventSender(String outputFilename) throws IOException {
        messagePacker = MessagePack.newDefaultPacker(Files.newOutputStream(Paths.get(outputFilename)));
    }

    public void close() throws IOException {
        messagePacker.close();
    }

    @Override
    public void fieldAccess(String structureName, String fieldName, boolean isWrite, TaintVector taints) throws IOException {
        messagePacker.packInt(AutoGramEvents.FIELDACCESS.ordinal());
        messagePacker.packString(structureName);
        messagePacker.packString(fieldName);
        messagePacker.packBoolean(isWrite);
        sendTaints(taints);

        messagePacker.flush();
    }

    /**
     * Sends the taints of the given taintVector as defined in {@link EventSender}
     * @throws IOException
     */
    private void sendTaints(TaintVector taintedVector) throws IOException {
        // for empty taints just tell, that the taint is empty
        if (taintedVector == null || taintedVector.isEmpty()) {
            messagePacker.packInt(0);
            return;
        }
        messagePacker.packInt(taintedVector.getLength());
        for (Taint t : taintedVector) {
            messagePacker.packInt(t.getSize());
            for (HashMap<Long, BitSet> map : t) {
                // if there is no taint for this byte, then just write a 0 taint
                if (map.isEmpty()) {
                    messagePacker.packInt(-1);
                    messagePacker.packInt(0);
                }
                for (Entry<Long, BitSet> entry : map.entrySet()) {
                    messagePacker.packLong(entry.getKey());
                    messagePacker.packInt(entry.getValue().size());
                    for (int index = 0; index < entry.getValue().size(); index++) {
                        messagePacker.packBoolean(entry.getValue().get(index));
                    }
                }
            }
        }
    }

    @Override
    public void methodEnter(String methodName, TaintVector[] argumentTaints) throws IOException {
        messagePacker.packInt(AutoGramEvents.METHODENTER.ordinal());
        messagePacker.packString(methodName);
        messagePacker.packInt(argumentTaints.length);
        for (TaintVector tv : argumentTaints) {
            sendTaints(tv);
        }

        messagePacker.flush();
    }

    @Override
    // EventCode, MethodName, NumTaintElements, [NumBytes, [SourceID, NumBits, [Bits]*]*]*
    public void methodExit(String methodName, TaintVector returnTaint) throws IOException {
        messagePacker.packInt(AutoGramEvents.METHODEXIT.ordinal());
        messagePacker.packString(methodName);
        sendTaints(returnTaint);

        messagePacker.flush();
    }

    @Override
    public void arrayWrite(long index, long identCode, TaintVector valueTaint, TaintVector indexTaint) throws IOException {
        messagePacker.packInt(AutoGramEvents.ARRAYACCESSWRITE.ordinal());
        messagePacker.packLong(index);
        messagePacker.packLong(identCode);
        sendTaints(valueTaint);
        sendTaints(indexTaint);
    }

    @Override
    public void arrayRead(long index, long identCode, TaintVector valueTaint, TaintVector indexTaint) throws IOException {
        messagePacker.packInt(AutoGramEvents.ARRAYACCESSREAD.ordinal());
        messagePacker.packLong(index);
        messagePacker.packLong(identCode);
        sendTaints(valueTaint);
        sendTaints(indexTaint);
    }

    @Override
    public void branch(TaintVector branchTaint) {
        // TODO branching events are currently ignored in autogram, so I can prune them for the moment here
    }

    @Override
    public void binOperation(Integer operatorVal, String op1Val, String op2Val, TaintVector resTaint, TaintVector op1Taint, TaintVector op2Taint) {
        // Intentionally left blank
    }

    @Override
    public void strcmp(String str1, String str2, Taint str1Taint, Taint str2Taint) {
        // Intentionally left blank
    }

    @Override
    public void swtch(String str1, String[] str2, Taint str1Taint) {
        // Intentionally left blank
    }

    @Override
    public void strchr(String str1, String str2, Taint str1Taint) {
        // Intentionally left blank
    }

    @Override
    public void bbEnter(int id) {
        // Intentionally left blank
    }

    @Override
    public void conversion(String str1, String conversionFunction, Taint str1Taint) {
        // Intentionally left blank
    }


}
