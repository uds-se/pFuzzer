package parser;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.List;

import taintengine.handlers.helperclasses.StructureMapper;
import taintengine.helperclasses.ProgramInputInformation;
import utils.LineInformation;
import utils.Operand;

public interface ITraceParser {
    /**
     * Parses the Arguments of the program under test and returns a
     * @return
     * @throws IOException
     */
    ProgramInputInformation[][] parseArguments() throws IOException;

    /**
     * Parses the next instruction as specified by the tracer format.
     * Currently the first line starting with the tracer tag is the linenumber, the second line is the instruction
     * and the third line contains the information about the instruction.
     * @return true if the parsing was successful, false if the file does not contain any more trace information.
     * @throws IOException
     */
    LineInformation parseNextInstruction() throws IOException;

    /**
     * Parses the metadata. Those contain additional information to increase tainting precision like global variables,
     * function definitions, unions, structs.
     * @param gvars
     * @return
     * @throws IOException
     */
    void parseMetadata(File metadata, StructureMapper structureMapper, Map<String, String[]> functions, List<Operand> gvars) throws IOException;

    /**
     * Extracts the static method information that is needed for JFlow.
     * @param metadata
     * @param oos
     */
    void extractStaticInfo(File metadata, ObjectOutputStream oos) throws IOException;
}
