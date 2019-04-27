package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;

import java.io.IOException;

public class FRead extends Operation {
    private int initialFilePosition;

    /**
     * Creates a fread operation which reflects the stdlibc method.
     * @param info
     */
    public FRead(LineInformation info) { super(info); }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        long address = Long.parseLong(info.getOperands()[1].getValue().split(" ")[0]);
        // number of read bytes is the return value of fread
        // number and size as defined by the documentation of fread
        int size = Integer.parseInt(info.getOperands()[6].getValue()) - initialFilePosition;

        arMapper.setNameSize(info.getOperands()[1].getName(), size);
        arMapper.setIndexSize(info.getOperands()[1].getName(), address);
    }

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        // get address and read string
        long address = Long.parseLong(info.getOperands()[1].getValue().split(" ")[0]);
        String rawValue = info.getOperands()[1].getValue();
        String value = rawValue.substring(rawValue.indexOf(" ") + 1);

        // address of the file and therefore the sourceID
        long sourceID = Long.parseLong(info.getOperands()[4].getValue());
        initialFilePosition = resourceManager.getFilePosition(sourceID);
        int counter = 0;
        Taint[] newTaint = new Taint[value.length()];
        for (char c : value.toCharArray()) {
            resourceManager.setCharacter(sourceID, initialFilePosition + counter, c);
            // TODO this operation is quite expensive, better set all bits in the end..
            newTaint[counter] = Taint.setBit(new Taint(1), sourceID, initialFilePosition + counter);

            counter++;
        }

        nodeMapper.addAddressTaint(address, new TaintVector(newTaint), 1);
        newNode = info.getAssignedRegisterName();
    }
}
