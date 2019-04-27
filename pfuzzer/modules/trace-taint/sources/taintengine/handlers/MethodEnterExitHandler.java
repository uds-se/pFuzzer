package taintengine.handlers;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.operations.Operation;

public class MethodEnterExitHandler extends OperationHandler {
    private final EventSender eventSender;
    private final ArrayIndexMapper arMapper;

    /**
     * Creates a handler to handle mathod entries and exits with the given parameters.
     * @param eventSender
     */
    public MethodEnterExitHandler(EventSender eventSender, ArrayIndexMapper arMapper) {
        this.eventSender = eventSender;
        this.arMapper = arMapper;

        try {
            eventSender.methodEnter("_real_program_main", new TaintVector[2]);
        } catch (IOException e) {
            throw new IllegalStateException("Writing out the method enter to main was not possilble ", e);
        }
    }

    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        try {
            operation.handleMethodEnterExit(nodeMapper, eventSender, arMapper);
        } catch (IOException e) {
            throw new IllegalStateException("The message Packer was not able to write data. The output may not be complete.", e);
        }
    }
}
