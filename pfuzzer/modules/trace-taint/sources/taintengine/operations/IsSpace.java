package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;

public class IsSpace extends Operation {
    public IsSpace(LineInformation info) {
        super(info);
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        Operand value = getOperands()[0];
        var taint = nodeMapper.getTaintForName(value.getName());
        if (!taint.isEmpty()) {
            eventSender.strchr(value.getValue(), " \t\n\f\r", taint.getTaint(0));
        }
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // Intentionally left blank
    }
}
