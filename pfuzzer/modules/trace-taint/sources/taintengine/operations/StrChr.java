package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;

public class StrChr extends Operation {
    public StrChr(LineInformation info) {
        super(info);
    }

    @Override
    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        Operand[] operands = getOperands();
        Operand chr = operands[2];
        TaintVector taintSwitchRegister = nodeMapper.getTaintForName(chr.getName());

        if (taintSwitchRegister == null || taintSwitchRegister.isEmpty()) {
            // at this point the character that is used for comparison is not tainted and therefore of no interest for us
            return;
        }

        String searchIn = operands[1].getValue().split(" ",2)[1];

        eventSender.strchr(chr.getValue(), searchIn, taintSwitchRegister.getTaint(0));
    }

    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        //Intentionally left blank
    }
}
