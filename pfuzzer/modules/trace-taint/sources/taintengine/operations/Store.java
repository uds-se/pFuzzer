package taintengine.operations;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.StructureMapper;
import utils.LineInformation;

public class Store extends Operation {
    /**
     * Creates a Store operation with the given line information.
     * @param info
     */
    public Store(LineInformation info) { super(info); }

    @Override
    public String[] getOperandNames() {
        String[] operands = {this.getOperands()[0].getName()};
        return operands;
    }

    private Long getStoreTo() {
        // this part of the operator is always an address and therefore a long
        return Long.parseLong(getOperands()[1].getValue());
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        // store even if no taint exists for the source value, this may overwrite old taints that are in the memory cells
        // overwriting such olds taints is important since the actual value in the cell is also overwritten when storing
        // an untainted value
        //		if (!nodeMapper.existsTaintForName(this.getOperandNames()[0])) {
        //			this.newNode = this.getOperands()[1].getName();
        //			return;
        //		}
        //		System.out.println(nodeMapper.getTaintForName(this.getOperandNames()[0]));
        // tmp should have existed at this point, so check the vector that is stored at this point
        nodeMapper.addAddressTaint(getStoreTo(), this.getOperandNames()[0], this.getOperands()[0].getByteSizeUnderlyingType() / this.getOperands()[0].getVectorLength());
        this.newNode = this.getOperands()[1].getName();
    }

    @Override
    public void handleStructureSendFieldAccess(StructureMapper structureMapper, NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        // check whether the pointer points to a field
        String structureName = this.getOperands()[1].getName();
        String fieldName = structureMapper.getFieldForLocal(structureName);
        if (fieldName == null) {
            return;
        }

        // if pointer points to a field, check whether the stores stores tainted data
        TaintVector fieldTaint = nodeMapper.getTaintForName(this.getOperands()[0].getName());
        // tv needs to be non null, if it is null, then there as no value initially
        if ((fieldTaint != null) && !fieldTaint.isEmpty()) {
            eventSender.fieldAccess(structureMapper.getStructureNameForLocal(structureName), fieldName, true, fieldTaint);
        }
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // for the store operation the memory is already allocated
        super.handleArrayAccess(nodeMapper, eventSender, arMapper);

        Long index = arMapper.getIndexForName(newNode);
        // if it was not mapped there was no array access for this variable
        if (index == null) {
            return;
        }

        eventSender.arrayWrite(index, Long.parseLong(arMapper.getAccessForName(newNode)), nodeMapper.getTaintForName(getOperands()[0].getName()), nodeMapper.getTaintForName(newNode));
    }
}
