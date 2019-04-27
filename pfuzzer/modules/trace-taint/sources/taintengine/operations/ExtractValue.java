package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.StructureMapper;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;

public class ExtractValue extends ValueOperation {
    // TODO implement taint propagation and so as defined by semantics

    /**
     * Creates a ExtractValue operation with the given line information.
     * @param info
     */
    public ExtractValue(LineInformation info) { super(info); }

    @Override
    public int getNumIndices() {
        return this.getOperands().length - 1;
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        TaintVector sourceVector = nodeMapper.getTaintForName(this.getOperandNames()[0]);
        if ((sourceVector == null) || (sourceVector.isEmpty())) {
            return;
        }

        TaintVector targetVector = nodeMapper.getTaintForName(info.getAssignedRegisterName());
        if (targetVector == null) {
            targetVector = new TaintVector(1, Operand.getByteSizeForType(info.getAssignedRegisterType()));
        }

        // vectorPosition 0 because it can only be sequentially read
        Taint currentTaint = targetVector.getTaint(0);
        Taint sourceTaint = sourceVector.getTaint(0);
        int pos = extractBytePositionByIndices();

        for (int x = 0; x < currentTaint.getSize(); x++) {
            currentTaint = Taint.unionIntoByte(currentTaint, sourceTaint, x, x + pos);
        }

        Taint[] taints = {currentTaint};
        nodeMapper.addLocalVector(info.getAssignedRegisterName(), taints, currentTaint.getSize(), TaintVector::unionIntoByteWise);

        this.newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleStructureSendFieldAccess(StructureMapper structureMapper, NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        // check if a structure was actually accessed by the extractvalue
        if (!info.getOperands()[0].getType().contains("{")) {
            this.newNode = info.getOperands()[0].getName();
            return;
        }

        // if there is no " = " contained the accessed structure has no name, so nothing will be sent here
        if (!info.getOperands()[0].getType().contains(" = ")) {
            this.newNode = info.getOperands()[0].getName();
            return;
        }
        int[] indices = getIndices();

        // iteratively go down the structure tree defined by the indices
        String structName = null;
        String fieldName = info.getOperands()[0].getType().split(" = ")[0].replace("%struct.", "");
        String accessedStructure = info.getOperands()[0].getType().split(" = ")[0].replace("%struct.", "");

        for (int index : indices) {
            structName = fieldName;
            fieldName = structureMapper.getFieldNameForStructureAndIndex(structName, index);
        }

        // the read bytes are exactly those that are stored to the local
        TaintVector taints = nodeMapper.getTaintForName(this.newNode);

        eventSender.fieldAccess(accessedStructure, fieldName, false, taints);

        this.newNode = info.getOperands()[0].getName();
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // check if a structure was actually accessed by the insertvalue
        if (getOperands()[0].getType().contains("{")) {
            return;
        }

        super.handleArrayAccess(nodeMapper, eventSender, arMapper);

        Long originalStorage = arMapper.getIndexForName(getOperandNames()[0]);
        int valueTaints = extractBytePositionByIndices() / info.getAssignedRegister().getByteSizeUnderlyingType();

        // taints that were written
        TaintVector readTaints = nodeMapper.getTaintForName(this.newNode);
        // index is never tainted, indices are constants for insertvalue
        TaintVector indexTaints = new TaintVector(0, 0);

        // the value is reassigned to a new SSA register, therefore the original storage has to be set again
        arMapper.setArrayIndex(info.getAssignedRegisterName(), originalStorage);
        eventSender.arrayRead(valueTaints, originalStorage, readTaints, indexTaints);
    }
}
