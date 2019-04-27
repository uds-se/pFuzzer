package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

import java.io.IOException;

public class BasicBlockEnter extends Operation {

    /**Creates new enter for BasicBlocks
     * @param info The line information for the bb entry.
     */
    public BasicBlockEnter(LineInformation info) {
        super(info);
    }

    @Override
    public void handleBinOperation(NodeMapper nodeMapper, EventSender es) throws IOException {
        es.bbEnter(Integer.parseInt(this.getOperands()[0].getValue()));
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // intentionally left blank
    }
}
