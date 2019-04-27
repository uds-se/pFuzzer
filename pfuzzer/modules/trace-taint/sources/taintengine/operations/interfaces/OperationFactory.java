package taintengine.operations.interfaces;

import taintengine.operations.Br;
import taintengine.operations.ByteBinOp;
import taintengine.operations.GenericOperation;
import taintengine.operations.BinOperation;
import taintengine.operations.Load;
import taintengine.operations.Operation;
import taintengine.operations.ReturnInst;
import taintengine.operations.SchuffleVector;
import taintengine.operations.Select;
import taintengine.operations.Store;
import taintengine.operations.TypeChange;
import utils.LineInformation;

public interface OperationFactory {
    /**
     * Creates an instertvalue operation with the given line information.
     * @param info
     * @return
     */
    Operation insertvalue(LineInformation info);

    /**
     * Creates an extractvalue operation with the given line information.
     * @param info
     * @return
     */
    Operation extractvalue(LineInformation info);

    /**
     * Creates a trunc operation with the given line information.
     * @param info
     * @return
     */
    Operation trunc(LineInformation info);

    /**
     * Creates a strto operation with the given line information.
     * Creates taint propagation for all strto operations, like strtol, strtod,...
     * @param info
     * @return
     */
    Operation strTo(LineInformation info);

    /**
     * Creates a realloc operation with the given line information.
     * @param info
     * @return
     */
    Operation realloc(LineInformation info);

    /**
     * Creates a strndup operation with the given line information.
     * @param info
     * @return
     */
    Operation strndup(LineInformation info);

    /**
     * Creates an isoc99sscnaf operation with the given line information.
     * @param info
     * @return
     */
    Operation isoc99Sscanf(LineInformation info);

    /**
     * Creates a read operation with the given line information.
     * @param info
     * @return
     */
    Operation read(LineInformation info);

    /**
     * Creates a strcmp operation with the given line information.
     * @param info
     * @return
     */
    Operation strcmp(LineInformation info);

    /**
     * Creates an sprintf operation with the given line information.
     * @param info
     * @return
     */
    Operation sprintf(LineInformation info);

    Operation switchstmt(LineInformation info);

    Operation getchar(LineInformation info);

    Operation bbEnter(LineInformation info);

    /**
     * Creates an strchr operation with the given line information.
     * @param info
     * @return
     */
    Operation strchr(LineInformation info);

    /**
     * Creates an generic operation based on the line information.
     * The opcode is used to decide which concrete operation will be generated.
     * @param info
     * @return
     */
    Operation getOperation(LineInformation info);

    /**
     * Creates a load operation with the given line information.
     * @param info
     * @return
     */
    Load load(LineInformation info);

    /**
     * Creates a store operation with the given line information.
     * @param info
     * @return
     */
    Store store(LineInformation info);

    /**
     * Creates a br operation with the given line information.
     * @param info
     * @return
     */
    Br br(LineInformation info);

    /**
     * Creates a default operation with the given line information.
     * This operation is a dead end. It does not propagate any taints and should only be used
     * as dummy for unimplemented operations.
     * @param info
     * @return
     */
    GenericOperation genericOperation(LineInformation info);

    /**
     * Creates a binary operation with the given line information.
     * @param info
     * @return
     */
    BinOperation binOperation(LineInformation info);

    /**
     * Creates a binary operation with the given line information.
     * @param info
     * @return
     */
    ByteBinOp byteBinop(LineInformation info);

    /**
     * Creates a call operation with the given line information.
     * @param info
     * @return
     */
    Operation call(LineInformation info);

    /**
     * Creates a return operation with the given line information.
     * @param info
     * @return
     */
    ReturnInst returnInst(LineInformation info);

    /**
     * Creates a select operation with the given line information.
     * @param info
     * @return
     */
    Select select(LineInformation info);

    /**
     * Creates a sufflevector operation with the given line information.
     * @param info
     * @return
     */
    SchuffleVector shufflevector(LineInformation info);

    /**
     * Creates a type change operation with the given line information.
     * @param info
     * @return
     */
    TypeChange typeChange(LineInformation info);

    /**
     * Creates a extractElement operation with the given line information.
     * @param info
     * @return
     */
    Operation extractElement(LineInformation info);

    /**
     * Creates a extractElement operation with the given line information.
     * @param info
     * @return
     */
    Operation shiftLeft(LineInformation info);

    /**
     * Creates a extractElement operation with the given line information.
     * @param info
     * @return
     */
    Operation logicalShiftRight(LineInformation info);

    /**
     * Creates a extractElement operation with the given line information.
     * @param info
     * @return
     */
    Operation arithmeticShiftRight(LineInformation info);

    /**
     * Creates a extractElement operation with the given line information.
     * @param info
     * @return
     */
    Operation signExtension(LineInformation info);

    /**
     * Creates a getElementPointer operation with the given line information.
     * @param info
     * @return
     */
    Operation getElementPointer(LineInformation info);

    /**
     * Creates an Alloca operation with the given line information.
     * @param info
     * @return
     */
    Operation alloca(LineInformation info);

    /**
     * Creates an MethodEntry operation with the given line information.
     * @param info
     * @return
     */
    Operation methodEntry(LineInformation info);

    /**
     * Creates an insertElement operation with the given line information.
     * @param info
     * @return
     */
    Operation insertElement(LineInformation info);

    /**
     * Creates an operation which handles the isoc99_fscanf call and taints the respective input.
     * @param info
     * @return
     */
    Operation isoc99FScanf(LineInformation info);

    /**
     * Creates an operation which handles the fgets call and taints the respective input.
     * @param info
     * @return
     */
    Operation fgets(LineInformation info);

    /**
     * Creates an operation which handles the fgetc call and taints the respective input.
     * @param info
     * @return
     */
    Operation fgetc(LineInformation info);

    /**
     * Creates an operation which handles the fread call and taints the respective input.
     * @param info
     * @return
     */
    Operation fread(LineInformation info);

    /**
     * Creates an operation which handles the filePosition call that stores the information on where the filepointer
     * currently is.
     * @param info
     * @return
     */
    Operation filePosition(LineInformation info);

    /**
     * Creates an operation for malloc and calloc calls.
     * currently is.
     * @param info
     * @return
     */
    Operation alloc(LineInformation info);
}
