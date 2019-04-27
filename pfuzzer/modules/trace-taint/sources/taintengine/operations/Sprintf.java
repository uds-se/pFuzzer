package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.EventSender;
import utils.LineInformation;
import utils.Operand;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

public class Sprintf extends Operation {
    private final Set<Character> specifier = new HashSet<Character>(Arrays.asList('c', 'd', 'i', 'e', 'E', 'f', 'g', 'G', 'o', 's', 'u', 'x', 'X', 'p', 'n', '%'));

    //indicates the first operand that is written
    protected int operandCounter;

    /**
     * Creates an spintf object with the given lineinformation.
     * @param info
     */
    public Sprintf(LineInformation info) {
        super(info);
        operandCounter = 3;
    }

    @Override
    public void handleArrayAccess(NodeMapper nodeMapper, EventSender eventSender, ArrayIndexMapper arMapper) throws IOException {
        String rawValue = getOperands()[1].getValue();
        String value = rawValue.substring(rawValue.indexOf(' ') + 1);
        // TODO if the allocated memory was larger ascthe string that is stored, then we cut here the size of
        // the stored area. Since sprintf is used for strings, we will keep this behavior for now.
        arMapper.setIndexSize(Long.parseLong(rawValue.split(" ")[0]), value.length());
    }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        char[] format = getFormat();

        long baseAddress = getBaseAddress();

        int finalCounter = 0;
        int formatCounter = 0;

        while (formatCounter < format.length) {
            // if no format specifier is found, just increase and do not perform any tainting
            if (format[formatCounter] != '%') {
                formatCounter++;
                finalCounter++;
                continue;
            }

            if (format[formatCounter] == '%') {
                formatCounter++;
                // get the format specifier for the next operand to be printed
                StringBuilder formatSpecification = new StringBuilder();
                formatSpecification.append('%');
                while (!specifier.contains(format[formatCounter])) {
                    formatSpecification.append(format[formatCounter]);
                    formatCounter++;
                }

                formatSpecification.append(format[formatCounter]);
                formatCounter++;
                String formatSpecifier = formatSpecification.toString();
                // if it ends with a '%', this is the printed constant character, so no taint propagation
                if (formatSpecifier.endsWith("%")) {
                    finalCounter++;
                    continue;
                }

                // for 'n' nothing happens
                if (formatSpecifier.endsWith("n")) {
                    continue;
                }

                String converted = convertAndPrint(operandCounter, formatSpecifier);
                taintPropagation(nodeMapper, operandCounter, baseAddress + finalCounter, converted.length());
                operandCounter++;
                finalCounter += converted.length();
            }
        }

        this.newNode = info.getOperands()[1].getName();
    }

    protected char[] getFormat() {
        String rawFormat = getOperands()[2].getValue();
        return rawFormat.substring(rawFormat.indexOf(' ') + 1).toCharArray();
    }

    protected long getBaseAddress() {
        String rawFinal = getOperands()[1].getValue();
        return Long.parseLong(rawFinal.split(" ")[0]);
    }

    private void taintPropagation(NodeMapper nodeMapper, int operandCounter, long storeTo, int bytesToWrite) {
        Operand operand = getOperands()[operandCounter];

        if (operand.getType().endsWith("*")) {
            // for strings and similar, the bytes to write are equal to the bytes retrieved from the source
            // address
            long sourceAddress = Long.parseLong(operand.getValue().split(" ")[0]);
            Taint[] taints = nodeMapper.getTaintForAddress(sourceAddress, bytesToWrite);
            nodeMapper.addTaintForAddress(storeTo, taints);
        } else {
            nodeMapper.addAddressTaints(storeTo, operand.getName(), bytesToWrite);
        }
    }

    /**
     * Converts the value of the operand to the respective number value if needed and returns the string representation
     * of the value as defined by the format specifier.
     * @param operandCounter
     * @param formatSpecifier
     * @return
     */
    private String convertAndPrint(int operandCounter, String formatSpecifier) {
        Operand operand = info.getOperands()[operandCounter];
        if (operand.getType().equals("float")) {
            float opvalue = Float.parseFloat(operand.getValue());
            return String.format(formatSpecifier, opvalue);
        }

        if (operand.getType().equals("double")) {
            double opvalue = Double.parseDouble(operand.getValue());
            return String.format(formatSpecifier, opvalue);
        }

        if (formatSpecifier.endsWith("c")) {
            char opvalue = (char)Integer.parseInt(operand.getValue());
            return String.format(formatSpecifier, opvalue);
        }

        if (operand.getType().startsWith("i") && !operand.getType().endsWith("*")) {
            long opvalue = Long.parseLong(operand.getValue());
            //format specifier u is not recognized by java
            //format specifier l is for the moment not important, it indicates a long or long long (for double l)
            return String.format(formatSpecifier.replace("u", "d").replace("l", ""), opvalue);
        }

        return String.format(formatSpecifier, operand.getValue().substring(operand.getValue().indexOf(' ') + 1));
    }
}
