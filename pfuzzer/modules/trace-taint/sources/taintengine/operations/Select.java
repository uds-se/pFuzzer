package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import utils.LineInformation;
import utils.Operand;

public class Select extends Operation {
    /**
     * Creates a select operation with the given line information.
     * @param info
     */
    public Select(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // TODO When operands[0] is a vector the selection should be done element by element.

        Operand[] ops = this.getOperands();
        // this part of the operator is always an index and therefore a long
        long selectValue = Long.parseLong(ops[0].getValue());
        String[] selOperands = {null};
        if (selectValue == 0) {
            selOperands[0] = ops[2].getName();
        } else {
            selOperands[0] = ops[1].getName();
        }

        Operand result = info.getAssignedRegister();
        nodeMapper.addLocal(result.getName(),
                            selOperands,
                            result.getVectorLength(),
                            result.getByteSizeUnderlyingType(),
                            TaintVector::unionIntoByteWise);

        this.newNode = result.getName();
    }
}
