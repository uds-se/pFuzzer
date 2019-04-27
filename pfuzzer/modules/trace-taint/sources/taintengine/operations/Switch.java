package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;
import java.util.LinkedList;

public class Switch extends Operation {
    /**
     * Creates a switch operation with the given line information.
     * @param info
     */
    public Switch(LineInformation info) { super(info); }

    @Override
    public void handleBranch(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        eventSender.branch(nodeMapper.getTaintForName(getOperands()[0].getName()));
    }

    @Override
    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        Operand[] operands = getOperands();
        Operand switchRegister = operands[0];
        TaintVector taintSwitchRegister = nodeMapper.getTaintForName(switchRegister.getName());

        if (taintSwitchRegister == null || taintSwitchRegister.isEmpty()) {
            // at this point the character that is used for comparison is not tainted and therefore of no interest for us
            return;
        }

        var values = new LinkedList<String>();
        for (int i = 1; i < operands.length ; i++) {
            Operand operand = operands[i];
            if ("label".equals(operand.getType())) {
                continue;
            }

            values.add(operand.getValue());
        }
        eventSender.swtch(switchRegister.getValue(), values.toArray(new String[0]), taintSwitchRegister.getTaint(0));
    }
}
