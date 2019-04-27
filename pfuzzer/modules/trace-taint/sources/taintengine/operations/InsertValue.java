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

public class InsertValue extends ValueOperation {
    /**
     * Creates a Insertvalue operation with the given line information.
     * @param info
     */
    public InsertValue(LineInformation info) { super(info); }

    @Override
    public int getNumIndices() {
        return this.getOperands().length - 2;
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        TaintVector sourceVector = nodeMapper.getTaintForName(this.getOperandNames()[1]);
        if ((sourceVector == null) || (sourceVector.isEmpty())) {
            this.newNode = info.getAssignedRegisterName();
            return;
        }

        TaintVector targetVector = nodeMapper.getTaintForName(this.getOperandNames()[0]);

        if (targetVector == null) {
            targetVector = new TaintVector(1, Operand.getByteSizeForType(info.getAssignedRegisterType()));
        }

        // vectorPosition 0 because the struct or array is sequentially stored in the memory
        Taint currentTaint = targetVector.getTaint(0);
        int pos = extractBytePositionByIndices();

        for (Taint t : sourceVector) {
            for (int x = 0; x < t.getSize(); x++) {
                currentTaint = Taint.unionIntoByte(currentTaint, t, x + pos, x);
            }
        }

        Taint[] taints = {currentTaint};
        nodeMapper.addLocalVector(info.getAssignedRegisterName(), taints, currentTaint.getSize(), TaintVector::unionIntoByteWise);

        this.newNode = info.getAssignedRegisterName();
    }

    @Override
    public void handleStructureSendFieldAccess(StructureMapper structureMapper, NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        // check if a structure was actually accessed by the insertvalue
        if (!info.getAssignedRegister().getType().contains("{")) {
            return;
        }

        // if there is no " = " contained the accessed structure has no name, so nothing will be sent here
        if (!info.getAssignedRegisterType().contains(" = ")) {
            return;
        }
        int[] indices = getIndices();

        // iteratively go down the structure tree defined by the indices
        String structName = null;
        String fieldName = info.getAssignedRegisterType().split(" = ")[0].replace("%struct.", "");
        String accessedStructure = info.getAssignedRegisterType().split(" = ")[0].replace("%struct.", "");

        for (int index : indices) {
            structName = fieldName;
            fieldName = structureMapper.getFieldNameForStructureAndIndex(structName, index);
        }

        // now extract the taints that were written
        TaintVector valueTaints = nodeMapper.getTaintForName(this.getOperandNames()[1]);

        eventSender.fieldAccess(accessedStructure, fieldName, true, valueTaints);
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // check if a structure was actually accessed by the insertvalue
        if (info.getAssignedRegister().getType().contains("{")) {
            return;
        }

        super.handleArrayAccess(nodeMapper, eventSender, arMapper);

        Long originalStorage = arMapper.getIndexForName(getOperandNames()[0]);

        // compute the index out of the bytes where the data starts and the size of the written data
        int valueTaints = extractBytePositionByIndices() / getOperands()[1].getByteSizeUnderlyingType();

        // taints that were written
        TaintVector writtenTaints = nodeMapper.getTaintForName(this.getOperandNames()[1]);
        // index is never tainted, indices are constants for insertvalue
        TaintVector indexTaints = new TaintVector(0, 0);

        // the value is reassigned to a new SSA register, therefore the original storage has to be set again
        arMapper.setArrayIndex(info.getAssignedRegisterName(), originalStorage);
        eventSender.arrayWrite(valueTaints, originalStorage, writtenTaints, indexTaints);
    }
}
