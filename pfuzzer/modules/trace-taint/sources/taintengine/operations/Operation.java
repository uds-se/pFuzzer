package taintengine.operations;

import java.io.IOException;
import java.util.LinkedList;

import taintengine.NodeMapper;
import taintengine.TaintVector;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import taintengine.handlers.helperclasses.MethodCallHelper;
import taintengine.handlers.helperclasses.ResourceManager;
import taintengine.handlers.helperclasses.StructureMapper;
import utils.LineInformation;
import utils.Opcode;
import utils.Operand;

public abstract class Operation {
    protected LineInformation info;
    protected String newNode;

    /**
     * Creates an Operation object based on the given line information. This is the abstract constructor.
     * @param info
     */
    public Operation(LineInformation info) {
        super();
        this.info = info;
    }

    /**
     * @return The opcode read from the line information. This works for all operations equivalently.
     */
    public Opcode getOpcode() { return info.getOpcode(); }

    /**
     * @return Returns the unfiltered operand List. Also contains values etc. To get only the names of the operands call {@link getOperandNames}
     */
    public Operand[] getOperands() { return info.getOperands(); }

    /**
     * @return Returns only the names of the operands. These are the wished SSA registers.
     */
    public String[] getOperandNames() {
        LinkedList<String> ret = new LinkedList<String>();
        for (Operand o : this.getOperands()) {
            ret.add(o.getName());
        }
        return ret.toArray(new String[0]);
    }

    /**
     * 	 * Returns the name of the node generated in the propagateTaint operation.
     * This method has to be run beforehand.
     * @return
     * @throws IllegalStateException If the propagateTaint operation was not run beforehand.
     */
    public String getNewNodeName() throws IllegalStateException {
        if (newNode == null) {
            throw new IllegalStateException("The propagateTaint method was not run beforehand or a new Node was not set.");
        }
        return newNode;
    }

    /**
     * Creates a new Node based on the operation. May be overwritten in a concrete class if necessary.
     * @param nodeMapper Maps the name of a ssa-register to a node.
     */
    public void propagateTaint(NodeMapper nodeMapper) {
        Operand result = info.getAssignedRegister();
        nodeMapper.addLocal(result.getName(),
                            this.getOperandNames(),
                            result.getVectorLength(),
                            result.getByteSizeUnderlyingType(),
                            TaintVector::unionIntoFull);

        this.newNode = result.getName();
    }

    /**
     * Returns an array of values. For scalars the array has size 1, for vectors the length of the vector.
     * @return
     */
    public Integer[] extractValues(String value) {
        String[] values = value.split(" ");
        Integer[] ret = new Integer[values.length];
        int index = 0;
        for (String v : values) {
            ret[index] = Integer.parseInt(v);
            index++;
        }

        return ret;
    }

    /**
     * Works on the StructureMapper to track field accesses and sends events regarding field accesses.
     * @param structureMapper
     * @param nodeMapper
     */
    public void handleStructureSendFieldAccess(StructureMapper structureMapper, NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        // Intentionally left blank
    }

    /**
     * Sends events regarding method enter and exit.
     * @param nodeMapper
     */
    public void handleMethodEnterExit(NodeMapper nodeMapper, EventSender eventSender , ArrayIndexMapper arMapper) throws IOException {
        // Intentionally left blank
    }

    /**
     * Sends events regarding array accesses and stores information regarding array accesses in the arMapper.
     * @param nodeMapper
     */
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        // check whether we need for some variable information about the size for an index
        for (Operand op : this.getOperands()) {
            if (op.getType().contains("*")) {
                arMapper.setIndexSize(op.getName(), Long.parseUnsignedLong(op.getValue()));
            }
        }
    }

    /**
     * Sends events regarding branches.
     * @param nodeMapper
     */
    public void handleBranch(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        // Intentionally left blank
    }

    /**
     * Generically handles the call to a method. For mostly all operations this is just a return.
     * @param nodeMapper
     * @param mch
     * @throws IOException
     */
    public void handleMethodCall(NodeMapper nodeMapper, MethodCallHelper mch) throws IOException {
        // Intentionally left blank
    }

    /**
     * Generically handles the taint generation. For mostly all operations this is just a return.
     * @param eventSender
     * @param resourceManager
     * @throws IOException
     */
    public void handleTaintGeneration(NodeMapper nodeMapper, EventSender eventSender, ResourceManager resourceManager) throws IOException {
        // Intentionally left blank
    }

    /**
     * Generically handles a binary operation
     * @param nodeMapper
     * @param eventSender
     * @throws IOException
     */
    public void handleBinOperation(NodeMapper nodeMapper, EventSender eventSender) throws IOException {
        // Intentionally left blank
    }

    @Override
    public String toString() {
        return "Operation [info=" + info + ", newNode=" + newNode + "]";
    }
}
