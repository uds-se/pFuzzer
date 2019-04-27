package taintengine.operations;

import utils.LineInformation;

public class GenericOperation extends Operation {
    /**
     * This operation is used for operators which do not have any border cases.
     * @param info
     */
    public GenericOperation(LineInformation info) { super(info); }
}
