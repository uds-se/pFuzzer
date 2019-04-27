package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import utils.LineInformation;
import utils.Operand;
import utils.TaintType;

public class ByteBinOp extends Operation {
    /**
     * Creates a binary operation with the given line information which works bitwise.
     * @param info
     */
    public ByteBinOp(LineInformation info) { super(info); }

    /**
     * Creates a new Node with bytewise taint union.
     * @param nodeMapper Maps the name of a ssa-register to a node.
     */
    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        this.newNode = info.getAssignedRegisterName();
        var tnt1 = nodeMapper.getTaintForName(info.getOperands()[0].getName());
        var tnt2 = nodeMapper.getTaintForName(info.getOperands()[1].getName());
        // TODO in future have some better integration for taint types, for the moment we do not propagate strlen taints
        // over calculations
        if ((tnt1 != null) && tnt1.getTaint(0).hasTaintType(TaintType.STRLEN) ||
                (tnt2 != null) && tnt2.getTaint(0).hasTaintType(TaintType.STRLEN)) {
            return;
        }
        var result = info.getAssignedRegister();
        nodeMapper.addLocal(result.getName(),
                this.getOperandNames(),
                result.getVectorLength(),
                result.getByteSizeUnderlyingType(),
                TaintVector::unionIntoFull);
    }
}
