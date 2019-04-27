package taintengine.operations;

import taintengine.helperclasses.TypeTree;
import utils.LineInformation;

public abstract class ValueOperation extends Operation {
    /**
     * Passes the {@link LineInformation} to the constructor of the {@link Operation} class.
     * @param info
     */
    public ValueOperation(LineInformation info) { super(info); }

    public abstract int getNumIndices();

    /**
     * Calculates based on the indices and the type the position of the accessed element in bytes relative to the
     * beginning of the structure,e.g.
     * for { i32, { i64, i8 }, i16 } and index list [1, 1]
     * there are 12 bytes in front (i32 and i64).
     * @return
     */
    protected int extractBytePositionByIndices() {
        String type = this.getOperands()[0].getType();
        // distinguish between struct and array type and split the first part away, type now looks like {...}
        if (!type.contains("[") && type.contains(" type ")) {
            type = type.split(" type ")[1];
        }

        int[] indices = getIndices();

        return TypeTree.getTypeTree(type).getSizeUntilIndex(indices);
    }

    /**
     * Generates the index list for this operation.
     * @return
     */
    public int[] getIndices() {
        int numIndices = getNumIndices();
        int[] indices = new int[numIndices];

        for (int x = this.getOperands().length - numIndices; x < this.getOperands().length; x++) {
            indices[x - (this.getOperands().length - numIndices)] = Integer.parseInt(this.getOperands()[x].getValue());
        }

        return indices;
    }
}
