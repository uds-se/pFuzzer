package taintengine.helperclasses;

import utils.Operand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class TypeTree {
    private final List<TypeTree> root = new LinkedList<TypeTree>();
    private final int size;

    /**
     * Generates a new TypeTree based on the given type string.
     * The type string is either:
     *     a struct,e.g.: type = { i32, { i64, i8 }, i16 }
     *     or an array, e.g.: [2 x [2 x i32]]
     * @param type
     * @return
     */
    public static TypeTree getTypeTree(String type) {
        if (type.contains("{")) {
            return new TypeTree(new ArrayList<>(Arrays.asList(type.split(", "))));
        } else {
            if (type.contains("[")) {
                String[] types = type.split(" x ");
                int[] sizes = new int[types.length - 1];

                // the last element of the splitted type is the base type
                for (int x = 0; x < types.length - 1; x++) {
                    sizes[x] = Integer.parseInt(types[x].replace("[", ""));
                }

                String byteSize = types[types.length - 1].replace("]", "");
                return new TypeTree(sizes, Operand.getByteSizeForType(byteSize), 0);
            }
        }

        return null;
    }

    /**
     * Recursively generate a type tree out of a type list (which is typically used for structs)
     * @param types
     */
    private TypeTree(List<String> types) {
        size = -1;

        if (types.get(0).contains("{ ")) {
            types.set(0, types.get(0).replace("{ ", ""));
        }

        while (!types.isEmpty()) {
            if (types.get(0).contains("{ ")) {
                root.add(new TypeTree(types));
                continue;
            }

            if (types.get(0).contains(" }")) {
                root.add(new TypeTree(Operand.getByteSizeForType(types.get(0).replace(" }", ""))));
                types.remove(0);
                return;
            }

            root.add(new TypeTree(Operand.getByteSizeForType(types.get(0))));
            types.remove(0);
        }
    }

    /**
     * Generates a type tree from a given list of sizes and a type (usually used in arrays)
     * @param sizes
     * @param typeSize
     * @param currentSizeIndex
     */
    private TypeTree(int[] sizes, int typeSize, int currentSizeIndex) {
        if (currentSizeIndex == sizes.length) {
            size = typeSize;
            return;
        }

        for (int x = 0; x < sizes[currentSizeIndex]; x++) {
            this.root.add(new TypeTree(sizes, typeSize, currentSizeIndex + 1));
        }
        size = -1;
    }

    private TypeTree(int size) { this.size = size; }

    /**
     * Returns the size of the tree and all subtrees combined.
     * @return
     */
    public int getSize() {
        if (this.size != -1) {
            return this.size;
        }

        int result = 0;

        for (TypeTree tt : this.root) {
            result += tt.getSize();
        }

        return result;
    }

    /**
     * Gives the number of bytes that exist before the index, e.g.
     * for { i32, { i64, i8 }, i16 } and index list [1, 1]
     * there are 12 bytes in front (i32 and i64).
     * @param indices
     * @return
     */
    public int getSizeUntilIndex(int[] indices) { return getSizeUntilIndex(indices, 0); }

    private int getSizeUntilIndex(int[] indices, int nextIndex) {
        int index = indices[nextIndex];

        int result = 0;
        Iterator<TypeTree> ttIterator = this.root.iterator();
        for (int x = 0; x < index; x++) {
            result += ttIterator.next().getSize();
        }

        // base case, all indices are handled
        if (indices.length <= nextIndex + 1) {
            return result;
        }
        return result + ttIterator.next().getSizeUntilIndex(indices, ++nextIndex);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{ ");

        if (this.size != -1) {
            return Integer.toString(this.size);
        }

        for (TypeTree tt : this.root) {
            stringBuilder.append(tt.toString());
            stringBuilder.append(", ");
        }

        // delete unnecessary comma before return
        return stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length() - 1).append("}").toString();
    }
}
