package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class ArithmeticShiftRight extends Operation {
    /**
     * Creates an arithmetic shift operation with the given line information.
     * @param info
     */
    public ArithmeticShiftRight(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        TaintVector vectorTaint = nodeMapper.getTaintForName(info.getOperands()[0].getName());

        this.newNode = info.getAssignedRegisterName();
        if (vectorTaint == null) {
            // if there was no taint for the original value, then nothing has to be done here
            return;
        }

        Taint[] newTaints = new Taint[vectorTaint.getLength()];
        Integer[] toShift = extractValues(info.getOperands()[1].getValue());

        for (int i = 0; i < vectorTaint.getLength(); i++) {
            newTaints[i] = shift(vectorTaint.getTaint(i), toShift[i]);
        }

        nodeMapper.addLocalVector(info.getAssignedRegisterName(), newTaints, info.getAssignedRegister().getByteSizeUnderlyingType(), TaintVector::unionIntoByteWise);
    }

    private Taint shift(Taint orig, int toShift) {
        int numberOfBytes = info.getAssignedRegister().getByteSizeUnderlyingType();
        int shiftByte = (int)Math.ceil((double)toShift / 8);
        boolean hasRemainder = (((double)toShift / 8) != 0);
        Taint newTaint = new Taint(numberOfBytes);

        for (int index = 0; index < numberOfBytes; index++) {
            // get the byte from where the taint comes from
            int getTaintFrom = Math.min(index + shiftByte, numberOfBytes - 1);
            newTaint = Taint.unionIntoByte(newTaint, orig, index, getTaintFrom);

            // now if the shift is not dividable by 8, a second taint (from the 1 lower byte) gets unioned into the new taint
            // also it has to be checked whether the upper bound is exceeded, if yes no second taint has to be applied
            if ((hasRemainder) && (index + shiftByte < numberOfBytes)) {
                int secondTaint = Math.max(getTaintFrom - 1, 0);
                newTaint = Taint.unionIntoByte(newTaint, orig, index, secondTaint);
            }
        }
        return newTaint;
    }
}
