package taintengine.handlers;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.operations.Operation;

public class BranchHandler extends OperationHandler {
    private final EventSender eventSender;

    /**
     * Creates a handler to handle array writes and reads with the given parameters.
     * @param eventSender
     */
    public BranchHandler(EventSender eventSender) { this.eventSender = eventSender; }

    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        try {
            operation.handleBranch(nodeMapper, eventSender);
        } catch (IOException e) {
            throw new IllegalStateException("The message Packer was not able to write data. The output may not be complete.", e);
        }
    }
}
