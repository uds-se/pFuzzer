package taintengine.operations;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Operand;
import utils.Utils;

public class Alloca extends Operation {
    /**
     * Creates a malloc operation with the given line information.
     * @param info
     */
    public Alloca(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        arMapper.setNameSize(newNode, getTypeSizeInBytes(info.getAssignedRegister()));
    }

    /**
     * Returns the number of bytes that are used to store the operand.
     * @return
     */
    private int getTypeSizeInBytes(Operand operand) {
        // if its a pointer to a pointer it only allocates space for a pointer
        if (operand.getType().endsWith("**")) {
            return Utils.POINTERBYTESIZE;
        }

        // use one reference less to determine type size
        int size = Operand.getByteSizeForType(operand.getType().replaceFirst("\\*", ""));

        // if there is no " x " the type is basic or a struct, so we already got the size we want
        return size;
    }
}
