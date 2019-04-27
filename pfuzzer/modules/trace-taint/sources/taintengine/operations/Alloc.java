package taintengine.operations;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

public class Alloc extends Operation {
    /**
     * Creates a malloc or calloc operation with the given line information.
     * @param info
     */
    public Alloc(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        //the commented out line below seems to not make sense for alloc operations -> we just alloc at [0] the number of bytes indicated by [1]
        //super.handleArrayAccess(nodeMapper, eventSender, arMapper);
        arMapper.setIndexSize(Long.parseLong(info.getOperands()[0].getValue()), Integer.parseInt(info.getOperands()[1].getValue()));
    }
}
