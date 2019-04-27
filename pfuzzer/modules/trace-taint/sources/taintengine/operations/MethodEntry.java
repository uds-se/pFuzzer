package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.MethodCallHelper;
import utils.LineInformation;

import java.io.IOException;

public class MethodEntry extends Operation {
    /**
     * Creates a MethodEntry operation with the given line information.
     * @param info
     */
    public MethodEntry(LineInformation info) { super(info); }

    @Override
    public void handleMethodCall(NodeMapper nodeMapper, MethodCallHelper mch) throws IOException {
        super.handleMethodCall(nodeMapper, mch);
        mch.flushCallMethod(info.getFunction(), nodeMapper);
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        arMapper.methodCall();
        super.handleArrayAccess(nodeMapper, eventSender, arMapper);
    }
}
