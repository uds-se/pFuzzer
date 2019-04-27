package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

import java.io.IOException;

public class Free extends Operation {
    /**
     * Creates a free operation with the given line information.
     * @param info
     */
    public Free(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // nothing to do here, a free does not delete the cell values
        // does not return anything
        this.newNode = "";
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        arMapper.deleteIndexEntry(Long.parseLong(info.getOperands()[0].getValue()));
    }
}
