package taintengine.handlers;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.operations.Operation;

public class ArrayAccessHandler extends OperationHandler {
    private final EventSender eventSender;
    private final ArrayIndexMapper arMapper;

    /**
     * Creates a handler to handle array writes and reads with the given parameters.
     * @param eventSender
     */
    public ArrayAccessHandler(EventSender eventSender, ArrayIndexMapper arMapper) {
        this.eventSender = eventSender;
        this.arMapper = arMapper;
    }

    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        try {
            operation.handleArrayAccess(nodeMapper, eventSender, arMapper);
        } catch (IOException e) {
            throw new IllegalStateException("The message Packer was not able to write data. The output may not be complete.", e);
        }
    }
}
