package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;

import java.io.IOException;

public class Read extends Operation {
    /**
     * Creates a read operation with the given line information.
     * @param info
     */
    public Read(LineInformation info) { super(info); }

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        long width = Long.parseLong(info.getOperands()[0].getValue());

        // reads larger than maxint are too large and skipped, likely nothing was read then.
        if (width > Integer.MAX_VALUE) {
            newNode = info.getAssignedRegisterName();
            return;
        }

        int size = Integer.parseInt(info.getOperands()[0].getValue());
        long sourceID = Long.parseLong(info.getOperands()[1].getValue());
        String value = info.getOperands()[2].getValue();
        long address = Long.parseLong(value.substring(0, value.indexOf(" ")));
        String str = value.substring(value.indexOf(" ") + 1, value.length());
        int offset = resourceManager.getFilePosition(sourceID);

        char[] strArray = str.toCharArray();
        for (int x = 0; x < size; ++x) {
            resourceManager.setCharacter(sourceID, offset + x, strArray[x]);
            Taint newTaint = Taint.setBit(new Taint(1), sourceID, offset + x);
            nodeMapper.addAddressTaint(address + x, newTaint);
        }

        resourceManager.saveFilePosition(sourceID, offset + size);
        newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        long width = Long.parseLong(info.getOperands()[0].getValue());
        // reads larger than maxint are too large and skipped, likely nothing was read then.
        if (width > Integer.MAX_VALUE) {
            newNode = info.getAssignedRegisterName();
            return;
        }
        String value = info.getOperands()[2].getValue();
        long address = Long.parseLong(value.substring(0, value.indexOf(" ")));
        arMapper.setIndexSize(address, (int)width);
    }
}
