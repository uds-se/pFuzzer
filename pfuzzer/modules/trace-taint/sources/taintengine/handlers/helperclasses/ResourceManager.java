package taintengine.handlers.helperclasses;

import java.util.Map;
import java.util.HashMap;

public class ResourceManager {

    private final Map<Long, Resource> resources = new HashMap<Long, Resource>();

    private static class Resource {
        // current position in the resource
        public Integer currPosition = 0;

        // view of the resource as an array of (ASCII) characters
        public Map<Integer, Character> charAtPosition = new HashMap<Integer, Character>();
    }

    /**
     * Creates a new ResourceManager which handles the calls to files and other inputs.
     * It manages the information on how much of the resource was already read (i.e. which bytes were already read).
     */
    public ResourceManager() { return; }

    /**
     * Stores the fileposition given by the parameter.
     *
     * @param sourceID
     * @param position
     */
    public void saveFilePosition(long sourceID, int position) {
        if (!resources.containsKey(sourceID)) {
            resources.put(sourceID, new Resource());
            if (position == -1) {
                resources.get(sourceID).currPosition = 0;
            }
        }

        if (position >= 0) {
            resources.get(sourceID).currPosition = position;
        }
    }

    /**
     * Returns the stored position of the file.
     *
     * @param sourceID
     * @return
     */
    public int getFilePosition(long sourceID) {
        if (!resources.containsKey(sourceID)) {
            return 0;
        }

        return resources.get(sourceID).currPosition;
    }

    /**
     * Associate a given character to a resource location
     */
    public void setCharacter(long sourceID, Integer position, Character value) {
        if (!resources.containsKey(sourceID)) {
            resources.put(sourceID, new Resource());
        }

        resources.get(sourceID).charAtPosition.put(position, value);
    }

    public String sourceToString(long sourceID) {
        if (!resources.containsKey(sourceID)) {
            return "";
        }

        Map<Integer, Character> indexToChar = resources.get(sourceID).charAtPosition;
        StringBuilder sbuilder = new StringBuilder();
        for (int i = 0; i < indexToChar.size(); ++i) {
            sbuilder.append(indexToChar.getOrDefault(i, '?'));
        }

        return sbuilder.toString();
    }
}
