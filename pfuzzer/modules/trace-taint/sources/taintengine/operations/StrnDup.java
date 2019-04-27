package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

import java.io.IOException;

public class StrnDup extends Operation {
    /**
     * Creates a strdup operation which implements the functionality of the strdup function
     * @param info
     */
    public StrnDup(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        int byteSize;
        // if a @NumberFormatException appears, the size was -1 originally
        // and the string size is important for taint propagation
        String operand2Value = this.getOperands()[2].getValue();
        String operand0Value = this.getOperands()[0].getValue();
        try {
            byteSize = Integer.parseInt(operand2Value);
        } catch (NumberFormatException nfe) {
            byteSize = operand0Value.substring(operand0Value.indexOf(' ')).length();
        }

        long src = Long.parseLong(this.getOperands()[1].getValue().split(" ")[0]);
        long dest = Long.parseLong(this.getOperands()[0].getValue().split(" ")[0]);

        Taint[] taint = nodeMapper.getTaintForAddress(src, byteSize);
        nodeMapper.addTaintForAddress(dest, taint);

        this.newNode = this.getOperands()[0].getName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        int byteSize;
        // if a @NumberFormatException appears, the size was -1 originally
        // and the string size is important for taint propagation
        String operand2Value = this.getOperands()[2].getValue();
        String operand0Value = this.getOperands()[0].getValue();
        try {
            byteSize = Integer.parseInt(operand2Value);
        } catch (NumberFormatException nfe) {
            byteSize = operand0Value.substring(operand0Value.indexOf(' ')).length();
        }

        long dest = Long.parseLong(this.getOperands()[0].getValue().split(" ")[0]);
        arMapper.setIndexSize(dest, byteSize);
    }
}
