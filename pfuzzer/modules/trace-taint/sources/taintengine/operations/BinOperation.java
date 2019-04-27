package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Opcode;
import utils.Operand;
import utils.TaintType;

import java.io.IOException;

public class BinOperation extends GenericOperation {
    /**
     * Create a sign extension operation with the given line information.
     * @param info
     */
    public BinOperation(LineInformation info) {
        super(info);
    }

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

        // this is a special case in which very likely the length of an array or string is taken
        // i.e. this is a substitution on addresses with tainted content
        handleStrlen(nodeMapper);
    }

    private void handleStrlen(NodeMapper nodeMapper) {
        if (info.getOpcode() == Opcode.SUB) {
            long address1 = Long.parseUnsignedLong(this.getOperands()[0].getValue());
            var tnt1 = nodeMapper.getTaintForAddress(address1, 1);
            long address2 = Long.parseUnsignedLong(this.getOperands()[1].getValue());
            var tnt2 = nodeMapper.getTaintForAddress(address2, 1);
            if (!tnt1[0].isEmpty() && !tnt2[0].isEmpty()) {
                int numberOfBytes = (int) (address1 - address2);
                //here the string has length 0, so the value is of no interest for us
                if (numberOfBytes <= 0) {
                    return;
                }
                // address2 is the smaller address as you substract the smaller address from the larger
                var newTaint = nodeMapper.getTaintsForAddress(address2, 1, numberOfBytes);
                newTaint.forEach(tnt -> tnt.addTaintType(TaintType.STRLEN));
                newTaint = TaintVector.unionIntoFull(new TaintVector(1, info.getAssignedRegister().getByteSizeUnderlyingType()), newTaint);
                Taint[] tnts = {newTaint.getTaint(0)};
                nodeMapper.addTaintForLocal(info.getAssignedRegisterName(), tnts);
            }
        }
    }

    @Override
    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        if (this.getOperands().length < 3) {
            //we only look at binops atm
            return;
        }

        Operand assignedRegister = info.getAssignedRegister();
        Operand operand1 = info.getOperands()[0];
        Operand operand2 = info.getOperands()[1];
        Operand operator = info.getOperands()[2];
        eventSender.binOperation(Integer.parseInt(operator.getValue()),
                                 operand1.getValue(),
                                 operand2.getValue(),
                                 nodeMapper.getTaintForName(assignedRegister.getName()),
                                 nodeMapper.getTaintForName(operand1.getName()),
                                 nodeMapper.getTaintForName(operand2.getName()));
    }
}
