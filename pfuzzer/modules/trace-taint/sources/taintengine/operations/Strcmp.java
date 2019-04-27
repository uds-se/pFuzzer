package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;

public class Strcmp extends Operation {
    /**
     * Create a string comparison operation which represents the standard lib strncmp/strcmp
     * @param info
     */
    public Strcmp(LineInformation info) {
        super(info);
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
//        System.out.println("Strcmp:");
//        System.out.println("  " + this.getOperands()[0]);
//        System.out.println("  " + this.getOperands()[1]);
//        System.out.println("  " + this.getOperands()[2]);
        this.newNode = this.getOperands()[0].getName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // Intentionally left blank
    }

    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        Operand operand1 = getOperands()[1];
        Operand operand2 = getOperands()[2];
        long addressOp1 = Long.parseLong(operand1.getValue().split(" ", 2)[0]);
        long addressOp2 = Long.parseLong(operand2.getValue().split(" ", 2)[0]);
        String stringOp1 = operand1.getValue().split(" ", 2)[1];
        String stringOp2 = operand2.getValue().split(" ", 2)[1];

        Taint taintOp1 = nodeMapper.getTaintForAddress(addressOp1, stringOp1.length())[0];
        Taint taintOp2 = nodeMapper.getTaintForAddress(addressOp2, stringOp2.length())[0];

        eventSender.strcmp(stringOp1, stringOp2, taintOp1, taintOp2);
    }
}
