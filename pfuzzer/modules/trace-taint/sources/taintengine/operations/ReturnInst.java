package taintengine.operations;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.StructureMapper;
import utils.LineInformation;

public class ReturnInst extends Operation {
    /**
     * Creates a return instruction with the given line information
     * @param info
     */
    public ReturnInst(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        if (this.getOperands().length == 0) {
            this.newNode = nodeMapper.returnCall("", 0, 0);
            return;
        }

        // return the register it returns to
        // the first operator defines the vector length
        this.newNode = nodeMapper.returnCall(this.getOperandNames()[0], info.getOperands()[0].getVectorLength(), info.getOperands()[0].getByteSizeUnderlyingType());
    }

    @Override
    public void handleStructureSendFieldAccess(StructureMapper structureMapper, NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        structureMapper.returnCall();
    }

    @Override
    public void handleMethodEnterExit(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        super.handleArrayAccess(nodeMapper, eventSender, arMapper);
        // at this point the function stack is already reduced, so we have to look at the taints of the returned node
        TaintVector taintForName = nodeMapper.getTaintForName(newNode);
        // if there is no taint for the return value, then print no taint but create event anyway
        if (taintForName == null) {
            taintForName = new TaintVector(0, 0);
        }
        eventSender.methodExit(info.getFunction(), taintForName);
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        arMapper.returnCall();
    }
}
