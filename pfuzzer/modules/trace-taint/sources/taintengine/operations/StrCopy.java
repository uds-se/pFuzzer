package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

import java.io.IOException;

public class StrCopy extends Operation {
    /**
     * Creates a string copy operation which represents the standard lib string copy operation.
     * @param info
     */
    public StrCopy(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        long src = Long.parseLong(this.getOperands()[1].getValue());
        long dest = Long.parseLong(this.getOperands()[0].getValue());
        int byteSize = nodeMapper.getRemainingBytesForAddress(src);

        Taint[] taints = nodeMapper.getTaintForAddress(src, byteSize);
        nodeMapper.addTaintForAddress(dest, taints);

        this.newNode = this.getOperands()[0].getName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // do not explicitly set the size for the index here, for strcpy the memory the string is stored to is already set
        super.handleArrayAccess(nodeMapper, eventSender, arMapper);
    }
}
