package taintengine.handlers;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.ResourceManager;
import taintengine.operations.Operation;

import java.io.IOException;

public class TaintGenerationHandler extends OperationHandler {
    private final ResourceManager resourceManager;
    private final EventSender eventSender;

    /**
     * Creates an event generation handler with the given parameters.
     * This handler calls the taint generation function on operations which then create new taints in the nodeMapper.
     * @param eventSender
     */
    public TaintGenerationHandler(EventSender eventSender, ResourceManager resourceManager) {
        this.eventSender = eventSender;
        this.resourceManager = resourceManager;
    }

    /**
     * Computes information whether a structure was accessed and sends out FieldAccessEvents if the structure operation had taints attached.
     */
    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        try {
            operation.handleTaintGeneration(nodeMapper, eventSender, resourceManager);
        } catch (IOException e) {
            throw new IllegalStateException("The message Packer was not able to write data. The output may not be complete.", e);
        }
    }
}
