package taintengine.handlers;

import taintengine.NodeMapper;
import taintengine.operations.Operation;

public class TaintPropagationHandler extends OperationHandler {
    /**
     * The taint propagation method of the current operator is called in this handler.
     */
    @Override
    public void handleOperation(Operation operation, NodeMapper nodeMapper) {
        operation.propagateTaint(nodeMapper);
    }
}
