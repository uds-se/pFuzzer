package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class LogicalShiftRight extends Operation {
    /**
     * Creates a logical shift operation with the given line information.
     * @param info
     */
    public LogicalShiftRight(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        TaintVector vectorTaint = nodeMapper.getTaintForName(info.getOperands()[0].getName());

        // if there was no taint for the original value, nothing has to be done here
        if (vectorTaint == null) {
            this.newNode = info.getOperands()[0].getName();
            return;
        }

        Taint[] newTaints = new Taint[vectorTaint.getLength()];
        Integer[] toShift = extractValues(info.getOperands()[1].getValue());

        for (int i = 0; i < vectorTaint.getLength(); i++) {
            newTaints[i] = shift(vectorTaint.getTaint(i), toShift[i]);
        }

        nodeMapper.addLocalVector(info.getAssignedRegisterName(), newTaints, info.getAssignedRegister().getByteSizeUnderlyingType(), TaintVector::unionIntoByteWise);

        this.newNode = info.getAssignedRegisterName();
    }

    private Taint shift(Taint orig, int toShift) {
        int numberOfBytes = info.getAssignedRegister().getByteSizeUnderlyingType();
        int shiftByte = (int)Math.ceil((double)toShift / 8);
        boolean hasRemainder = (((double)toShift / 8) != 0);
        Taint newTaint = new Taint(numberOfBytes);

        for (int index = 0; index < numberOfBytes; index++) {
            if (index + shiftByte > numberOfBytes) {
                break;
            }
            // get the byte from where the taint comes from
            int getTaintFrom = Math.min(index + shiftByte, numberOfBytes - 1);
            newTaint = Taint.unionIntoByte(newTaint, orig, index, getTaintFrom);

            // now if the shift is not dividable by 8, a second taint (from the 1 lower byte) gets unioned into the new taint
            // also it has to be checked whether the upper bound is exceeded, if yes no second taint has to be applied
            int secondTaint = getTaintFrom - 1;
            if ((hasRemainder) && (index + shiftByte < numberOfBytes) && (secondTaint >= 0)) {
                newTaint = Taint.unionIntoByte(newTaint, orig, index, secondTaint);
            }
        }
        return newTaint;
    }
}
