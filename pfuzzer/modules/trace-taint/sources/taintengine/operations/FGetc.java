package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;

import java.io.IOException;

public class FGetc extends Operation {
    /**
     * Creates a fgetc operation which reflects the stdlibc method.
     * @param info
     */
    public FGetc(LineInformation info) { super(info); }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // Intentionally left blank
    }

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        long sourceID = Long.parseLong(info.getOperands()[1].getValue());
        long characterNumber = Long.parseLong(info.getOperands()[0].getValue());
        char assignedCharacter = (char)characterNumber;
        int resourcePosition = resourceManager.getFilePosition(sourceID);
        resourceManager.setCharacter(sourceID, resourcePosition, assignedCharacter);

        Taint newTaint = new Taint(4);
        newTaint = Taint.setBit(newTaint, sourceID, resourcePosition);
        String name = info.getAssignedRegisterName();
        nodeMapper.addTaintForLocal(name, newTaint);

        resourceManager.saveFilePosition(sourceID, resourcePosition + 1);
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // the taint "propagation" already happens in the taint generation, since here the variable is initially assigned
        this.newNode = info.getOperands()[0].getName();
    }
}
