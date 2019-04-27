package taintengine.handlers;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.StructureMapper;
import taintengine.operations.Operation;

public class StructureHandler extends OperationHandler {
    private final StructureMapper structureMapper;
    private final EventSender eventSender;

    /**
     * Creates a structure handler with the given parameters.
     * @param structureMapper
     * @param eventSender
     */
    public StructureHandler(StructureMapper structureMapper, EventSender eventSender) {
        this.structureMapper = structureMapper;
        this.eventSender = eventSender;
    }

    /**
     * Computes information whether a structure was accessed and sends out FieldAccessEvents if the structure operation had taints attached.
     */
    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        try {
            operation.handleStructureSendFieldAccess(structureMapper, nodeMapper, eventSender);
        } catch (IOException e) {
            throw new IllegalStateException("The message Packer was not able to write data. The output may not be complete.", e);
        }
    }
}
