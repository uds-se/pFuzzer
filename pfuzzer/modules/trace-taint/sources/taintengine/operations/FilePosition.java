package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;

public class FilePosition extends Operation {
    /**
     * Creates an operation which stores the current position of the filepointerin the
     * resourceManager. This information can then be used to precisely taint the input.
     * @param info
     */
    public FilePosition(LineInformation info) { super(info); }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {}

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        Operand[] operands = info.getOperands();

        // start with 0, if ftell failed or the position in the file is larger than 2^32 bytes
        // then the position is said to be 0
        // TODO in future files larger than 4 GB may be supported
        int filePosition = 0;
        try {
            filePosition = Integer.parseInt(operands[1].getValue());
        } catch (NumberFormatException nfe) {
            // nothing to be done here, filepositions over this size are currently not recognized
        }
        resourceManager.saveFilePosition(Long.parseLong(operands[0].getValue()), filePosition);
    }
}
