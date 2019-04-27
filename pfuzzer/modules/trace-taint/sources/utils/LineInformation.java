package utils;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import utils.Effect;

public class LineInformation {
    private String instruction = "";

    // each instruction has a unique number which should not deviate for the three parsed lines
    //(linenumber, instrumented instruction and instruction information)
    private Opcode opcode;

    // for assignments the assigned register is stored here
    private Operand assignedRegister;

    // the operands of the instruction are stored here
    private Operand[] operands;

    // the optionals of the instruction are stored here
    private Effect[] opts;

    // the surrounding function
    private String function;

    /**
     * Set the string representation of the LLVM-IR instruction.
     * @param instruction
     */
    public void setInstruction(String instruction) { this.instruction = instruction; }

    /**
     * Set the opcode of the instruction.
     * @param opcode
     */
    public void setOpcode(int opcode) {
        // the opcode is implicitly given by its position. The enum starts at 0, the opcode numbering at 1
        this.opcode = Opcode.values()[opcode - 1];
    }

    /**
     * Set the name and type of the assigned Register of the instruction (which can only be done for assignment instructions).
     * @param assignedRegister
     * @param type
     */
    public void setAssignedRegister(String assignedRegister, String type) { this.assignedRegister = new Operand(assignedRegister, null, type); }

    /**
     * Set the operands of the instruction.
     * @param operands
     */
    public void setOperands(List<Map<String, String>> operands) {
        this.operands = new Operand[operands.size()];

        int counter = 0;
        for (Map<String, String> operand : operands) {
            this.operands[counter] = new Operand(operand.get("o"), operand.get("v"), operand.get("t"));
            ++counter;
        }
    }

    public void setOpts(List<Map<String, String>> opts) {
        this.opts = new Effect[opts.size()];

        int counter = 0;
        for (Map<String, String> effect : opts) {
            this.opts[counter] = new Effect(effect);
            ++counter;
        }
    }

    public Effect[] getOpts() { return opts.clone(); }

    /**
     * @return The string representation of the LLVM-IR instruction.
     */
    public String getInstruction() { return instruction; }

    /**
     * @return The opcode ordinal for the instruction.
     */
    public Opcode getOpcode() { return opcode; }

    /**
     * @return The register that gets assigned by this instruction. May be null for non-assignment instructions.
     */
    public String getAssignedRegisterName() { return assignedRegister.getName(); }

    /**
     * Returns the assigned register.
     * @return
     */
    public Operand getAssignedRegister() { return assignedRegister; }

    /**
     * @return The operands of this instruction.
     */
    public Operand[] getOperands() { return operands.clone(); }

    /**
     * @return The surrounding function for this line.
     */
    public String getFunction() { return function; }

    /**
     * @param function The surrounding function for this line.
     */
    public void setFunction(String function) { this.function = function; }

    /**
     * Returns the type of the assigned register.
     * @return
     */
    public String getAssignedRegisterType() { return assignedRegister.getType(); }

    @Override
    public String toString() {
        return "LineInformation [instruction=" + instruction + ", opcode=" + opcode + ", assignedRegister="
            + assignedRegister + ", operands=" + Arrays.toString(operands) + ", opts=" + Arrays.toString(opts) + ", function=" + function + "]";
    }
}
