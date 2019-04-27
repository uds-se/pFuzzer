package taintengine.operations;

import java.io.IOException;

import taintengine.NodeMapper;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.StructureMapper;
import utils.LineInformation;

public class GetElementPointer extends Operation {
    /**
     * Creates an getElementPoitner operation with the given line information.
     * @param info
     */
    public GetElementPointer(LineInformation info) { super(info); }

    // for a GEP the structure mapper has to save the name of the variable together with the name of the accesses field element.
    @Override
    public void handleStructureSendFieldAccess(StructureMapper structureMapper, NodeMapper nodeMapper, EventSender eventSender) {
        // a pointer to a structure
        if (this.getOperands().length < 3) {
            return;
        }
        // TODO it might be the case, that multi dereferencing leads to a cascade of dereferencings
        structureMapper.mapLocalToElement(this.getNewNodeName(), this.getOperands()[0].getType(), this.extractValues(this.getOperands()[2].getValue())[0]);
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        super.handleArrayAccess(nodeMapper, eventSender, arMapper);
        // if "[" is contained, then an element from an array is taken
        if (getOperands()[0].getType().contains("[")) {
            // the third operand defines the index
            arMapper.setArrayIndex(info.getAssignedRegisterName(), Integer.parseInt(getOperands()[2].getValue()));
            arMapper.setAccessedArray(info.getAssignedRegisterName(), getOperands()[0].getValue());
        }
    }
}
