package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import utils.LineInformation;

import java.io.IOException;
import java.util.Map;

public class Isoc99SScanf extends Scanf {
    /**
     * Creates a isoc99_fscanf operation which reflects the stdlibc method.
     * @param info
     */
    public Isoc99SScanf(LineInformation info) { super(info); }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        for (Map.Entry<Long, Integer> entry : addressSizeMapping.entrySet()) {
            arMapper.setIndexSize(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        long filePointer = Long.parseLong(getOperands()[1].getValue().split(" ")[0]);
        String rawValue = info.getOperands()[2].getValue();
        String formatString = rawValue.substring(rawValue.indexOf(" ") + 1);
        String readChars = info.getOperands()[1].getValue();
        readChars = readChars.substring(readChars.indexOf(" ") + 1);

        char[] format = formatString.toCharArray();
        // The second operand is the first to assign a value to.
        parseFormatSpecifier(resourceManager, readChars.toCharArray(), format, nodeMapper, filePointer, 3);
    }

    @Override
    protected void storeAtPosition(ResourceManager resourceManager, char[] characters, int startingPoint, long address, int numberToStore, NodeMapper nodeMapper, int typeSize, boolean arraySpecifier, long sourceID) {
        // get taint at the beginning, this is either filled if the read information is not stored in an
        // array, otherwise each byte in the memory gets the respective taint
        TaintVector tnt = nodeMapper.getTaintsForAddress(sourceID + startingPoint, 1, numberToStore);

        if (!arraySpecifier) {
            // store the assigned taints to the memory, e.g. for a i32* which is not an array
            nodeMapper.addAddressTaint(address, TaintVector.unionIntoFull(new TaintVector(1, 4), tnt), typeSize);
        } else {
            // store all characters consecutively in the shadow memory, the numberToStore is important here
            nodeMapper.addAddressTaint(address, tnt, numberToStore);
        }
    }
}
