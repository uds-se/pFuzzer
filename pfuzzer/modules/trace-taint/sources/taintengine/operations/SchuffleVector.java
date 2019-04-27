package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class SchuffleVector extends Operation {
    /**
     * Create a shufflevector operation with the given lineinformation.
     * @param info
     */
    public SchuffleVector(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // the mask vector is the third parameter
        Integer[] maskVector = this.extractValues(this.getOperands()[2].getValue());

        TaintVector tvOperand0 = nodeMapper.getTaintForName(this.getOperands()[0].getName());
        TaintVector tvOperand1 = nodeMapper.getTaintForName(this.getOperands()[1].getName());

        Taint[] shuffledTaints = new Taint[maskVector.length];

        int index = 0; // index in new vector
        for (int i : maskVector) {
            // i is the index pointing to one of the operand vectors
            if (i < tvOperand0.getLength()) {
                shuffledTaints[index] = tvOperand0.getTaint(i);
            } else {
                // now i lies in the second operand,
                // so the index to get the correct taint is calculated and the taint is taken from the second operand
                shuffledTaints[index] = tvOperand1.getTaint(i - tvOperand0.getLength());
            }

            index++;
        }

        nodeMapper.addLocalVector(info.getAssignedRegisterName(), shuffledTaints, info.getAssignedRegister().getByteSizeUnderlyingType(), TaintVector::unionIntoByteWise);

        this.newNode = info.getAssignedRegisterName();
    }
}
