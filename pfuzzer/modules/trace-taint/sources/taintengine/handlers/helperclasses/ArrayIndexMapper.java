package taintengine.handlers.helperclasses;

import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map.Entry;

import taintengine.helperclasses.ProgramInputInformation;

public class ArrayIndexMapper {
    // maps variable to index of array
    private final Deque<Map<String, Long>> variableIndexMap = new LinkedList<Map<String, Long>>();

    // maps variable to name of accessed array
    private final Deque<Map<String, String>> variableAccessMap = new LinkedList<Map<String, String>>();

    // maps the index of an array to the size of an array
    private final Map<Long, Integer> indexSizeMap = new HashMap<Long, Integer>();

    // maps the variable name to the size of the allocated memory. This map is only a temporary storage.
    private final Deque<Map<String, Integer>> nameSizeMap = new LinkedList<Map<String, Integer>>();

    /**
     * Creates a new empty arrayIndexMapper
     * @param argvInformation
     */
    public ArrayIndexMapper(ProgramInputInformation[][] argvInformation) {
        Map<String, Long> newMap = new HashMap<String, Long>();
        variableIndexMap.addLast(newMap);

        Map<String, String> newAccessMap = new HashMap<String, String>();
        variableAccessMap.addLast(newAccessMap);

        Map<String, Integer> newSizeMap = new HashMap<String, Integer>();
        nameSizeMap.add(newSizeMap);

        // initialize index-size mapping for the argv elements s.t. they are correctly handled when used and passed in calls
        for (ProgramInputInformation[] programInputInformations : argvInformation) {
            indexSizeMap.put(programInputInformations[0].getAddress(), programInputInformations.length);
        }
    }

    /**
     * Maps a variable name to an index of an array
     * @param varName
     * @param index
     */
    public void setArrayIndex(String varName, long index) {
        Map<String, Long> map = variableIndexMap.getLast();
        map.put(varName, index);
    }

    /**
     * Maps a variable name to the accessed array
     * @param varName
     * @param accessed
     */
    public void setAccessedArray(String varName, String accessed) {
        Map<String, String> map = variableAccessMap.getLast();
        map.put(varName, accessed);
    }

    /**
     * Sets for an array the size of the allocated memory.
     * @param name
     * @param index
     */
    public void setIndexSize(String name, Long index) {
        if (nameSizeMap.getLast().isEmpty()) {
            return;
        }
        Integer size = nameSizeMap.getLast().get(name);

        if (size == null) {
            return;
        }

        nameSizeMap.getLast().remove(name);

        indexSizeMap.put(index, size);
    }

    /**
     * Sets index and size directly if both are known.
     * @param index
     * @param size
     */
    public void setIndexSize(long index, int size) {
        indexSizeMap.put(index, size);
    }

    /**
     * Store information about the size of an array for a given variable name.
     * Later the size will be mapped to the originating index of the array.
     * @param name
     * @param size
     */
    public void setNameSize(String name, Integer size) { nameSizeMap.getLast().put(name, size); }

    /**
     * Returns the index for a variable name.
     * @param name
     * @return
     */
    public Long getIndexForName(String name) { return variableIndexMap.getLast().get(name); }

    /**
     * Returns the accessed array for the variable name.
     * @param name
     * @return
     */
    public String getAccessForName(String name) { return variableAccessMap.getLast().get(name); }

    /**
     * Looks up if the given address is part of an already seen allocated memory part, if yes it is checked
     * how many bytes can be accessed from this address until the end of the array.
     * @param address
     * @return
     */
    public int getRemainingBytesForAddress(long address) {
        for (Entry<Long, Integer> entry : this.indexSizeMap.entrySet()) {
            // second comparison must be a real small, a consecutive array may start at entry.getKey() + entry.getValue()
            if (entry.getKey() <= address && address < entry.getKey() + entry.getValue()) {
                // can safely cast here, array sizes are smaller than 4GB
                return (int)(entry.getValue() - (address - entry.getKey()));
            }
        }

        return 0;
    }

    /**
     * On method call a new scope has to be created.
     */
    public void methodCall() {
        variableIndexMap.add(new HashMap<String, Long>());

        variableAccessMap.addLast(new HashMap<String, String>());

        nameSizeMap.addLast(new HashMap<String, Integer>());
    }

    /**
     * On return the scope has to be removed and the old scope is taken again.
     */
    public void returnCall() {
        variableIndexMap.removeLast();
        variableAccessMap.removeLast();
        nameSizeMap.removeLast();
    }

    /**
     * Removes an index to size map, is usually used if a pointer is freed.
     * @param index
     */
    public void deleteIndexEntry(long index) {
        // TODO maybe one has to iterate over the indices and delete all references
        indexSizeMap.remove(index);
    }
}
