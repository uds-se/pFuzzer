package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class ShiftLeft extends Operation {
    /**
     * Creates an shift left operation with the given line information.
     * @param info
     */
    public ShiftLeft(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        this.newNode = info.getAssignedRegisterName();

        TaintVector vectorTaint = nodeMapper.getTaintForName(info.getOperands()[0].getName());
        if (vectorTaint == null) {
            // if there was no taint for the original value, nothing has to be done here
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

        // begin at with shiftByte since lower bytes do not get any taints (they are filled with zeros)
        for (int index = shiftByte; index < numberOfBytes; index++) {
            // get the byte from where the taint comes from
            int getTaintFrom = Math.max(index - shiftByte, 0);
            newTaint = Taint.unionIntoByte(newTaint, orig, index, getTaintFrom);

            // now if the shift is not dividable by 8, a second taint (from the 1 lower byte) gets unioned into the new taint
            // also it has to be checked whether the upper bound is exceeded, if yes no second taint has to be applied
            if ((hasRemainder) && (index - shiftByte > 0)) {
                int secondTaint = Math.max(getTaintFrom + 1, numberOfBytes - 1);
                newTaint = Taint.unionIntoByte(newTaint, orig, index, secondTaint);
            }
        }
        return newTaint;
    }
}
