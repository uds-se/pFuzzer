package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class SExt extends TypeChange {
    /**
     * Create a sign extension operation with the given line information.
     * @param info
     */
    public SExt(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        TaintVector orig = nodeMapper.getTaintForName(this.getOperandNames()[0]);

        // if the original value was not tainted there is no need to do something for the sign
        // extension
        if (orig == null) {
            TaintVector toAssign = nodeMapper.getTaintForName(info.getAssignedRegisterName());
            if (toAssign != null && !toAssign.isEmpty()) {
                nodeMapper.removeLocal(info.getAssignedRegisterName());
            }
            this.newNode = info.getAssignedRegisterName();
            return;
        }

        Taint[] newTaints = new Taint[orig.getLength()];
        int byteSizeUnderlyingType = info.getAssignedRegister().getByteSizeUnderlyingType();

        for (int index = 0; index < orig.getLength(); index++) {
            newTaints[index] = createNewTaint(orig.getTaint(index), byteSizeUnderlyingType);
        }

        nodeMapper.addLocalVector(info.getAssignedRegisterName(), newTaints, byteSizeUnderlyingType, TaintVector::unionIntoByteWise);
        this.newNode = info.getAssignedRegisterName();
    }

    private Taint createNewTaint(Taint taint, int byteSizeUnderlyingType) {
        Taint newTaint = new Taint(byteSizeUnderlyingType);
        // fill the first bytes with the correct taints
        newTaint = Taint.unionIntoByteWise(newTaint, taint);

        // fill the remaining bytes with the taint of the largest byte since this is a sign extension
        for (int i = taint.getSize(); i < newTaint.getSize(); i++) {
            newTaint = Taint.unionIntoByte(newTaint, taint, i, taint.getSize() - 1);
        }
        return newTaint;
    }
}
