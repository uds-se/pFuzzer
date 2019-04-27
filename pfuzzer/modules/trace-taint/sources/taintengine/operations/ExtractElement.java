package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class ExtractElement extends Operation {
    /**
     * Create an extractElement operation with the given line information.
     * @param info
     */
    public ExtractElement(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // the index lies in the second operator and is always an int
        int index = Integer.parseInt(this.getOperands()[1].getValue());

        TaintVector vectorTaint = nodeMapper.getTaintForName(this.getOperandNames()[0]);

        Taint[] newVector = {vectorTaint.getTaint(index)};
        nodeMapper.addLocalVector(info.getAssignedRegisterName(), newVector, info.getAssignedRegister().getByteSizeUnderlyingType(), TaintVector::unionIntoByteWise);
        this.newNode = info.getAssignedRegisterName();
    }
}
