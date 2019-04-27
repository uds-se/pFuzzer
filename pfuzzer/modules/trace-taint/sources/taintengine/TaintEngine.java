package taintengine;

import java.util.BitSet;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import taintengine.handlers.OperationHandler;
import taintengine.handlers.helperclasses.ArrayIndexMapper;
import taintengine.handlers.helperclasses.ResourceManager;
import taintengine.helperclasses.ProgramInputInformation;
import taintengine.operations.ConcreteOperationFactory;
import taintengine.operations.Operation;
import taintengine.operations.interfaces.OperationFactory;
import utils.LineInformation;
import utils.Operand;

public class TaintEngine {
    private final OperationFactory opFactory = new ConcreteOperationFactory();
    private final NodeMapper nodeMapper;
    private static final long ARGVSOURCEID = 0;
    private final List<OperationHandler> operationHandlers = new LinkedList<OperationHandler>();

    /**
     * Create the initial datastructure which will then be used to track the taint information.
     * Initialize the taint for the command line arguments.
     *
     * @param argv
     * @param funDefMap
     * @param gvarMap
     * @param resourceManager
     */
    public TaintEngine(ProgramInputInformation[][] argv, Map<String, String[]> funDefMap, List<Operand> gvarMap, ResourceManager resourceManager) {
        nodeMapper = new NodeMapper(funDefMap, gvarMap);

        int index = 0;
        for (ProgramInputInformation[] leafArray : argv) {
            for (ProgramInputInformation taintedInput : leafArray) {
                BitSet forTaint = new BitSet();
                resourceManager.setCharacter(ARGVSOURCEID, index, taintedInput.getCorrespondingCharacter());
                forTaint.set(index++);

                nodeMapper.addAddressTaint(taintedInput.getAddress(), new Taint(ARGVSOURCEID, forTaint, 1));
            }
        }
        resourceManager.saveFilePosition(ARGVSOURCEID, index);
        String[] operands = {"argc", "argv"};
        nodeMapper.functionCall("_real_program_main", operands, "MainReturnDummy");
    }

    /**
     * Gets the information of a line and generates a node out of this.
     * @param info The lineinformation
     */
    public void handleLine(LineInformation info) {
        Operation operation = opFactory.getOperation(info);

        for (OperationHandler opHandler : this.operationHandlers) {
            opHandler.handleOperation(operation, nodeMapper);
        }
    }

    /**
     * Adds an OperationHandler to the taint engine. This handler will be executed after all other handlers.
     * @param opHandler
     */
    public void registerHandler(OperationHandler opHandler) { this.operationHandlers.add(opHandler); }

    /**
     * Sets the arrayindexMapper in the node mapper.
     * @param arMapper
     */
    public void addArrayIndexMapperToNodeMapper(ArrayIndexMapper arMapper) { nodeMapper.addArrayIndexMapper(arMapper); }
}
