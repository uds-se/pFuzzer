package taintengine;

import utils.TaintType;

import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class Taint implements Iterable<HashMap<Long, BitSet>> {
    // TODO immutability is not fully implemented, at least the Taint cannot be changed from the outside

    private final List<HashMap<Long, BitSet>> taintedBytes = new LinkedList<HashMap<Long, BitSet>>();
    private Set<TaintType> taintTypes = EnumSet.of(TaintType.DEFAULT);

    /**
     * Create a new taint with the given number of bytes.
     * @param numberOfBytes
     */
    public Taint(int numberOfBytes) {
        for (int i = 0; i < numberOfBytes; i++) {
            taintedBytes.add(new HashMap<>());
        }
    }

    /**
     * Creates a taint with the given sourceID and BitSet. It gives all bytes the same sourceID and BitSet
     * @param sourceID
     * @param bitset
     * @param numberOfBytes
     */
    public Taint(Long sourceID, BitSet bitset, int numberOfBytes) {
        for (int i = 0; i < numberOfBytes; i++) {
            HashMap<Long, BitSet> newMap = new HashMap<Long, BitSet>();
            newMap.put(sourceID, bitset); // bitset is not copied here!
            taintedBytes.add(newMap);
        }
    }

    /**
     * This is constructor to put taints one after another. I.e. for each taint the first byte is taken and put in the
     * constructed taint at the respective byte position induced by the taints position in the argument array.
     * Null cells are represented in the new taint as empty tainted byte.
     * @param taints
     */
    public Taint(Taint[] taints) {
        for (Taint tnt : taints) {
            HashMap<Long, BitSet> newMap = new HashMap<>();
            if (tnt != null) {
                for (Entry<Long, BitSet> ent : tnt.taintedBytes.get(0).entrySet()) {
                    newMap.put(ent.getKey(), (BitSet)ent.getValue().clone());
                }
                this.taintTypes.addAll(tnt.taintTypes);
            }
            this.taintedBytes.add(newMap);
        }
    }

    /**
     * Creates a copy of this taint. All changes to the copy do not affect the original and vice verca.
     */
    public Taint copy() {
        Taint newTaint = new Taint(this.taintedBytes.size());
        int index = 0;
        for (HashMap<Long, BitSet> map : taintedBytes) {
            for (Entry<Long, BitSet> centry : map.entrySet()) {
                BitSet entryBitSet = centry.getValue();
                newTaint.taintedBytes.get(index).put(centry.getKey(), (BitSet)entryBitSet.clone());
            }
            index++;
        }
        newTaint.taintTypes = EnumSet.copyOf(this.taintTypes);
        return newTaint;
    }

    /**
     * Set the bit at index for the given sourceID. This operation sets the bit for all bytes.
     * @param sourceID
     * @param index
     */
    public static Taint setBit(Taint tnt, long sourceID, int index) {
        Taint newTaint = tnt.copy();
        for (HashMap<Long, BitSet> map : newTaint.taintedBytes) {
            if (!map.containsKey(sourceID)) {
                map.put(sourceID, new BitSet());
            }
            map.get(sourceID).set(index);
        }

        return newTaint;
    }

    /**
     * Adds the taints of toUnion to the taints of the source.
     * The parameters will not be changed by this operation. All bytes get all taints from all other bytes.
     * @param toUnion
     * @param source
     */
    public static Taint unionIntoFull(Taint source, Taint toUnion) {
        assert toUnion != null;
        Taint newTaint = source.copy();
        for (int toUnionIndex = 0; toUnionIndex < toUnion.taintedBytes.size(); toUnionIndex++) {
            for (int thisIndex = 0; thisIndex < newTaint.taintedBytes.size(); thisIndex++) {
                unionIntoOneByte(toUnion, thisIndex, toUnionIndex, newTaint);
            }
        }
        newTaint.taintTypes.addAll(toUnion.taintTypes);
        return newTaint;
    }

    /**
     * Adds the taints of toUnion to the taints of the source. The parameters will not be changed by this operation.
     * The operation unions each byte to the respective other byte.
     * @param toUnion
     * @param source
     */
    public static Taint unionIntoByteWise(Taint source, Taint toUnion) {
        assert toUnion != null;
        Taint newTaint = source.copy();
        int upperBound = Math.min(newTaint.taintedBytes.size(), toUnion.taintedBytes.size());
        // the remaining bytes are unchanged
        for (int index = 0; index < upperBound; index++) {
            unionIntoOneByte(toUnion, index, index, newTaint);
        }

        newTaint.taintTypes.addAll(toUnion.taintTypes);
        return newTaint;
    }

    /**
     * Adds the taints of the parameter to the taints of this. The parameter will not be changed by this operation.
     * The operation unions the byte from toUnion at position posToUnion into the byte from this object at position posThis.
     * @param toUnion
     * @param posThis
     * @param posToUnion
     */
    public static Taint unionIntoByte(Taint source, Taint toUnion, int posThis, int posToUnion) {
        assert toUnion != null;
        Taint newTaint = source.copy();
        unionIntoOneByte(toUnion, posThis, posToUnion, newTaint);

        newTaint.taintTypes.addAll(toUnion.taintTypes);
        return newTaint;
    }

    private static void unionIntoOneByte(Taint toUnion, int posThis, int posToUnion, Taint newTaint) {
        for (Entry<Long, BitSet> centry : toUnion.taintedBytes.get(posToUnion).entrySet()) {
            // if the key exists then we can perform an or operation on the bitset of this taint
            BitSet mergeSet = newTaint.taintedBytes.get(posThis).get(centry.getKey());
            if (mergeSet == null) {
                // if it does not exist we can just create a copy of the bitset and add it here
                newTaint.taintedBytes.get(posThis).put(centry.getKey(), (BitSet)centry.getValue().clone());
            } else {
                mergeSet.or(centry.getValue());
            }
        }
    }

    /**
     * Returns the number of bytes contained in the taint.
     * @return
     */
    public int getSize() { return this.taintedBytes.size(); }

    @Override
    public String toString() {
        return "Taint [taint=" + taintedBytes + ']';
    }

    /**
     * Is true if and only if the taint is empty (no sourceID is stored in this taint).
     * @return
     */
    public boolean isEmpty() {
        boolean empty = true;
        for (HashMap<Long, BitSet> set : taintedBytes) {
            empty &= set.isEmpty();
        }

        return empty;
    }

    @Override
    public Iterator<HashMap<Long, BitSet>> iterator() {
        return this.copy().taintedBytes.iterator();
    }

    public Set<TaintType> getTaintTypes() {
        return Collections.unmodifiableSet(this.taintTypes);
    }

    public void setTaintType(TaintType taintType) {
        this.taintTypes = EnumSet.of(taintType);
    }

    public void addTaintType(TaintType taintType) {
        this.taintTypes.add(taintType);
    }

    public boolean hasTaintType(TaintType taintType) {
        return this.taintTypes.contains(taintType);
    }
}
