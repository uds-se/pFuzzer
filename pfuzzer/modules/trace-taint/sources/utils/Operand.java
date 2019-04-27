package utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Deque;
import java.util.LinkedList;

public class Operand {
    private final String name;
    private final String value;
    private final String type;
    private final int vectorLength;

    /**
     * Create an operand with the given information.
     * @param name
     * @param value
     * @param type
     */
    public Operand(String name, String value, String type) {
        super();
        this.name = name;
        this.value = value;
        this.type = type;

        // if its a pointer to a vector the actual vector length is 1
        if (!type.contains("<") || type.endsWith("*")) {
            this.vectorLength = 1;
        } else {
            // a vector type looks like "<4 x i8>", so the second member is the length
            this.vectorLength = Integer.parseInt(type.replace("<", "").split(" ")[0]);
        }
    }

    /**
     * Return the name of the operand.
     * @return
     */
    public String getName() { return name; }

    /**
     * Return the value of the operand.
     * @return
     */
    public String getValue() { return value; }

    /**
     * Return the type of the operand.
     * @return
     */
    public String getType() { return type; }

    /**
     * Return the vector length of the operand. If it is not a vector this returns 0.
     * @return
     */
    public int getVectorLength() { return vectorLength; }

    /**
     * Returns the number of bytes denoted by the type.
     * For pointer the size of the pointsTo is returned, for vectors and arrays the size of the element.
     * @return
     */
    public int getByteSizeUnderlyingType() { return getByteSizeForType(this.type); }

    /**
     * Returns the size in bytes which is consumed by an element of
     * the given type.
     * @param type
     * @return
     */
    public static int getByteSizeForType(String type) {
        String[] splitted = type.split(" ");
        LinkedList<Integer> addStack = new LinkedList<Integer>();
        LinkedList<Integer> mulStack = new LinkedList<Integer>();

        // initialize with nothing for standard types
        addStack.addFirst(0);
        // for types like "void (i32, %struct.siginfo_t*, i8*)*" or "ioid ()*"
        // one has to wait until ")*" and skip all inbetween
        boolean waitForPointer = false;
        Iterator<String> stIterator = Arrays.asList(splitted).iterator();
        while (stIterator.hasNext()) {
            String str = stIterator.next();
            if (waitForPointer) {
                if (str.endsWith(")*") || str.endsWith(")*,")) {
                    waitForPointer = false;
                    addStack.addFirst(addStack.pollFirst() + Utils.POINTERBYTESIZE);
                    continue;
                }

                if (str.matches(".*\\)\\*\\]*")) {
                    waitForPointer = false;
                    endArrayByteSizeCalculation(addStack, mulStack, str);
                    continue;
                }
                // skip everything until the void pointer is resolved
                continue;
            }
            if (str.equals("void") || str.equals("ioid")) {
                waitForPointer = true;
                continue;
            }
            if (str.startsWith("<")) {
                // skip "x"
                stIterator.next();

                // extract type
                String endOfVector = stIterator.next();
                if (endOfVector.endsWith("*")) {
                    addStack.addFirst(Utils.POINTERBYTESIZE);
                    continue;
                }
                int size = getByteSizeForStandardTypeStructAndUnion(endOfVector.replace(">", ""));
                addStack.addFirst(size * Integer.parseInt(str.replace("<", "")));
                continue;
            }
            str = str.replace(",", "");
            if (str.startsWith("{")) {
                addStack.addFirst(0);
                continue;
            }

            if (str.startsWith("[")) {
                mulStack.addFirst(Integer.parseInt(str.replace("[", "")));
                continue;
            }

            // if this happens, an anonymous struct is the second part of an array
            // so the top of the addstack has to be put onto the mulstack
            if (str.startsWith("}]")) {
                mulStack.addFirst(addStack.pollFirst());
                str = str.substring(1);
            }

            if (str.endsWith("]")) {
                endArrayByteSizeCalculation(addStack, mulStack, str);
                continue;
            }

            if (str.endsWith("}")) {
                addStack.addFirst(addStack.pollFirst() + addStack.pollFirst());
                continue;
            }

            if (str.startsWith("i") || str.startsWith("%")) {
                addStack.addFirst(addStack.pollFirst() + getByteSizeForStandardTypeStructAndUnion(str));
                continue;
            }

            // skip unnecessary syntax symbols
            if (str.equals("x") || str.equals("=") || str.equals("type") || str.equals("null")) {
                continue;
            }

            addStack.addFirst(getByteSizeForStandardTypeStructAndUnion(str));
        }

        return addStack.getFirst();
    }

    private static void endArrayByteSizeCalculation(Deque<Integer> addStack, Deque<Integer> mulStack, String str) {
        // count number of closing parenthesis
        String typeNoParenthesis = str.replace("]", "");
        int parenCount = str.length() - typeNoParenthesis.length();
        // if the type is empty is was calculated before (e.g. an anonymous struct was part of it)
        if (!typeNoParenthesis.equals("")) {
            mulStack.addFirst(getByteSizeForStandardTypeStructAndUnion(typeNoParenthesis));
        }

        for (int x = 0; x < parenCount; x++) {
            mulStack.addFirst(mulStack.pollFirst() * mulStack.pollFirst());
        }

        addStack.addFirst(addStack.pollFirst() + mulStack.pollFirst());
    }

    private static int getByteSizeForStandardTypeStructAndUnion(String type) {
        if (type.startsWith("@")) {
            // this was a function pointer, so the size does not matter esentially, just return pointer size
            return Utils.POINTERBYTESIZE;
        }
        if (type.endsWith("*")) {
            return Utils.POINTERBYTESIZE;
        }
        if (type.startsWith("double")) {
            return Utils.DOUBLEBYTESIZE;
        }

        if (type.startsWith("float")) {
            return Utils.FLOATBYTESIZE;
        }

        if (type.startsWith("half")) {
            return Utils.HALFBYTESIZE;
        }

        if (type.startsWith("void")) {
            return Utils.VOIDBYTESIZE;
        }

        if (type.startsWith("i86_fp80") || type.startsWith("x86_fp80")) {
            return Utils.I86_FP80SIZE;
        }

        // if its a structure we will recognize this here and write out the correct size
        Integer structSize = Utils.SIZEMAP.get(type.replaceAll("%struct.", "").replaceAll("%union.", ""));
        if (structSize != null) {
            return (int)Math.ceil((double)structSize / 8); // size is stored in bits, has to be converted in bytes
        }

        // if the size is not dividable by 8 the bytes still have to loaded, therefore the result has to be ceiled.
        // i is removed in order to get the bitsize denoted by llvm base type
        return (int)Math.ceil((double)Integer.parseInt(type.replaceAll("i", "")) / 8);
    }

    private static int byteSizeArray(String type) {
        type = type.replace("{ ", "").replace(" }", "");
        String[] types = type.split(" x ");
        int byteSizeType = getByteSizeForType(types[types.length - 1].replace("]", ""));

        int finalSize = 0;
        // the last element of the splitted type is the base type
        for (int x = 0; x < types.length - 1; x++) {
            finalSize += byteSizeType * Integer.parseInt(types[x].replace("[", ""));
        }

        return finalSize;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + vectorLength;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Operand other = (Operand)obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }

        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }

        return (vectorLength == other.vectorLength);
    }

    @Override
    public String toString() {
        return "Operand [name=" + name + ", value=" + value + ", type=" + type + "]";
    }
}
