package taintengine.handlers;

import taintengine.NodeMapper;
import taintengine.operations.Operation;

public abstract class OperationHandler {
    /**
     * A handler has to be registered in the Tainttracker main to the the taintEngine.
     */

    /**
     * Performs the for the handler specific computations on the operation with the given node mapper.
     * @param operation
     * @param nodeMapper
     */
    public abstract void handleOperation(Operation operation, NodeMapper nodeMapper);
}
