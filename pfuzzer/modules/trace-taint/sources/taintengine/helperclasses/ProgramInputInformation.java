package taintengine.helperclasses;

public class ProgramInputInformation {
    private final long address;
    private final char correspondingCharacter;

    /**
     * Create a leaf node (they point always to themselves implictly).
     * @param address The unique taint value
     * @param correspondingCharacter The character it corresponds to. This is the character the user can define.
     */
    public ProgramInputInformation(long address, char correspondingCharacter) {
        super();
        this.address = address;
        this.correspondingCharacter = correspondingCharacter;
    }

    /**
     * Its taint value.
     * @return The unique taint value.
     */
    public long getAddress() { return address; }

    /**
     * The corresponding character.
     * @return The correstponding character
     */
    public char getCorrespondingCharacter() { return correspondingCharacter; }

    @Override
    public int hashCode() {
        return (int)address;
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

        ProgramInputInformation other = (ProgramInputInformation)obj;
        if (address != other.address) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "LeafNode [taint=" + address + ", correspondingCharacter=" + correspondingCharacter + "]";
    }
}
