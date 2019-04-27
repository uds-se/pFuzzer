package taintengine.operations;

import taintengine.NodeMapper;
import taintengine.Taint;
import taintengine.TaintVector;
import utils.LineInformation;

public class Trunc extends Operation {
    /**
     * Creates a trunc operation with the given line information.
     * @param info
     */
    public Trunc(LineInformation info) { super(info); }

    @Override
    public void propagateTaint(NodeMapper nodeMapper) {
        TaintVector taintedSource = nodeMapper.getTaintForName(info.getOperands()[0].getName());
        this.newNode = info.getAssignedRegisterName();

        // if there was no taint for the original value, then nothing has to be done here
        if (taintedSource == null) {
            return;
        }

        TaintVector result = new TaintVector();
        taintedSource.elements().map(t -> trunc(t)).forEach(result::add);

        nodeMapper.addTaintForLocal(info.getAssignedRegisterName(), result);
    }

    private Taint trunc(Taint orig) {
        int numberOfBytes = info.getAssignedRegister().getByteSizeUnderlyingType();
        Taint newTaint = new Taint(numberOfBytes);

        // trunc fills as many bytes as possible
        for (int x = 0; x < numberOfBytes; x++) {
            newTaint = Taint.unionIntoByte(newTaint, orig, x, x);
        }

        return newTaint;
    }
}
