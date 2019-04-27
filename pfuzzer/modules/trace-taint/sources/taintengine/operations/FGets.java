package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;

import java.io.IOException;

public class FGets extends Operation {
    // stores temporarily the fileposition at the point when this operation was called
    private int initialFilePosition;

    /**
     * Creates a fgets operation which reflects the stdlibc method.
     * @param info
     */
    public FGets(LineInformation info) { super(info); }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        long address = Long.parseLong(info.getOperands()[1].getValue().split(" ")[0]);
        int size = info.getOpts()[1].get("result").length();

        arMapper.setNameSize(info.getAssignedRegisterName(), size);
        arMapper.setIndexSize(info.getAssignedRegisterName(), address);
    }

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        // get address and read string
        long address = Long.parseLong(info.getOperands()[0].getValue());
        long sourceID = Long.parseLong(info.getOpts()[1].get("source"));
        String value = info.getOpts()[1].get("result");

        initialFilePosition = resourceManager.getFilePosition(sourceID);
        int counter = 0;

        for (char c : value.toCharArray()) {
            resourceManager.setCharacter(sourceID, initialFilePosition + counter, c);
            // TODO this operation is quite expensive, better set all bits in the end..
            Taint newTaint = Taint.setBit(new Taint(1), sourceID, initialFilePosition + counter);
            nodeMapper.addAddressTaint(address + counter, newTaint);

            counter++;
        }

        // get the size of the read string and advance the stream position
        int size = value.length();

        resourceManager.saveFilePosition(sourceID, initialFilePosition + size);
    }
}
