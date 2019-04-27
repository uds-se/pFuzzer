package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;

import java.io.IOException;

public class StrTo extends Operation {
    /**
     * Creates a strto operation which performs the taint propagation over
     * strto calls, like strtol, strtod, ...
     * @param info
     */
    public StrTo(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        String[] splittedValue = info.getOperands()[1].getValue().split(" ");
        newNode = info.getAssignedRegisterName();
        if (splittedValue.length < 2) {
            // nothing was converted, the function got an empty string
            return;
        }
        long address = Long.parseLong(splittedValue[0]);
        int strLength = splittedValue[1].length();

        TaintVector taintedMemory = nodeMapper.getTaintsForAddress(address, 1, strLength);
        int byteSizeUnderlyingTypeAssignedRegister = info.getAssignedRegister().getByteSizeUnderlyingType();
        taintedMemory = TaintVector.unionIntoFull(new TaintVector(1, byteSizeUnderlyingTypeAssignedRegister), taintedMemory);
        Taint[] tnts = {taintedMemory.getTaint(0)};
        nodeMapper.addLocalVector(info.getAssignedRegisterName(), tnts, byteSizeUnderlyingTypeAssignedRegister, TaintVector::unionIntoFull);

    }

    @Override
    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        String[] splittedValue = info.getOperands()[1].getValue().split(" ");
        if (splittedValue.length < 2) {
            // nothing was converted, the function got an empty string
            return;
        }
        long address = Long.parseLong(splittedValue[0]);
        var converted = splittedValue[1];
        TaintVector taintedMemory = nodeMapper.getTaintsForAddress(address, 1, converted.length());
        int byteSizeUnderlyingTypeAssignedRegister = info.getAssignedRegister().getByteSizeUnderlyingType();
        taintedMemory = TaintVector.unionIntoFull(new TaintVector(1, byteSizeUnderlyingTypeAssignedRegister), taintedMemory);
        Taint tnts = taintedMemory.getTaint(0);
        if (!tnts.isEmpty()) {
            eventSender.conversion(converted, getOperandNames()[getOperands().length - 1], tnts);
        }
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // Intentionally left blank
    }
}
