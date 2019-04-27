package utils;

import java.util.Map;

public class Effect {
    private final Map<String, String> data;

    /**
     * Create an operand with the given information.
     * @param name
     * @param value
     * @param type
     */
    public Effect(Map<String, String> data) {
        this.data = data;
    }

    public String get(String field) {
        return data.get(field);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("Effect [");
        for (Map.Entry<String, String> entry : this.data.entrySet()) {
            result.append(" ");
            result.append(entry.getKey());
            result.append("=\"");
            result.append(entry.getValue());
            result.append("\" ");
        }
        result.append("]");
        return result.toString();
    }
}
