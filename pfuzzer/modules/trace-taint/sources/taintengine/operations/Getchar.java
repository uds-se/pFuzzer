package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;
import utils.Utils;

import java.io.IOException;

public class Getchar extends Operation {
    public Getchar(LineInformation info) {
        super(info);
    }

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) {
        int sourceID = Utils.STDINFILEMAPPER;
        char value = 0;
        try {
            value = (char) Integer.parseInt(info.getOperands()[0].getValue());
        } catch(NumberFormatException nfe) {
            // in this case something was read which is not representable as a char, likely a -1, in any case we can stop here
            System.out.println(info.getOperands()[0].getValue());
        }

        String regname = info.getAssignedRegisterName();

        int index = resourceManager.getFilePosition(sourceID);
        resourceManager.setCharacter(sourceID, index, value);
        Taint newTaint = Taint.setBit(new Taint(4), sourceID, index);
        nodeMapper.addTaintForLocal(regname, newTaint);
        resourceManager.saveFilePosition(sourceID, index + 1);

        newNode = info.getAssignedRegisterName();
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // no taint to be propagated
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
    }
}
