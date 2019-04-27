package taintengine.handlers;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.MethodCallHelper;
import taintengine.operations.Operation;

import java.io.IOException;

public class MethodCallHandler extends OperationHandler {
    private final MethodCallHelper mch;

    /**
     * Generates a new MethodCallHandler and sets the {@link EventSender} s.t. events can later be generated for methodEntries.
     * @param eventSender
     */
    public MethodCallHandler(EventSender eventSender) { this.mch = new MethodCallHelper(eventSender); }

    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        try {
            operation.handleMethodCall(nodeMapper, mch);
        } catch (IOException e) {
            throw new IllegalStateException("The message Packer was not able to write data. The output may not be complete.", e);
        }
    }
}
