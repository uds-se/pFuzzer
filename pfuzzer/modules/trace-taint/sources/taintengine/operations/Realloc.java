package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

import java.io.IOException;

public class Realloc extends Operation {
    /**
     * Creates a malloc operation with the given line information.
     * @param info
     */
    public Realloc(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        long oldAddress = Long.parseLong(info.getOperands()[1].getValue());
        long newAddress = Long.parseLong(info.getOperands()[0].getValue());

        // if the memory was extended at the same position, everything is fine, otw.
        // the taints have to be copied to the new position
        if (oldAddress != newAddress) {
            int remBytes = nodeMapper.getRemainingBytesForAddress(oldAddress);
            Taint[] taints = nodeMapper.getTaintForAddress(oldAddress, remBytes);
            nodeMapper.addTaintForAddress(newAddress, taints);
        }

        newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        super.handleArrayAccess(nodeMapper, eventSender, arMapper);

        int size = Integer.parseInt(info.getOperands()[2].getValue());
        long newAddress = Long.parseLong(info.getOperands()[0].getValue());
        arMapper.setIndexSize(newAddress, size);
    }
}
