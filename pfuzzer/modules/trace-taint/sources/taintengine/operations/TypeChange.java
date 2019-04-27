package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import utils.LineInformation;
import utils.Operand;

public class TypeChange extends Operation {
    /**
     * Creates an bitcast operation with the given line information.
     * @param info
     */
    public TypeChange(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        Operand result = info.getAssignedRegister();
        nodeMapper.addLocal(result.getName(),
                            getOperandNames(),
                            result.getVectorLength(),
                            result.getByteSizeUnderlyingType(),
                            TaintVector::unionIntoByteWise);

        this.newNode = result.getName();
    }
}
