package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class IdentifierExpression extends Expression {
    private final String name;
    private CatscriptType type;

    public IdentifierExpression(String value) {
        this.name = value;
    }

    public String getName() {
        return name;
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        CatscriptType type = symbolTable.getSymbolType(getName());
        if (type == null) {
            addError(ErrorType.UNKNOWN_NAME);
        } else {
            this.type = type;
        }
    }

    public String getDescriptor() {
        StringBuilder sb = new StringBuilder("");
        if (type.equals(CatscriptType.VOID)) {
            sb.append("V");
        } else if (type.equals(CatscriptType.BOOLEAN) || type.equals(CatscriptType.INT)) {
            sb.append("I");
        } else {
            sb.append("L").append(internalNameFor(getType().getJavaType())).append(";");
        }
        return sb.toString();
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        return runtime.getValue(this.getName());
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Integer slotNum = code.resolveLocalStorageSlotFor(getName());
        String descriptor = getDescriptor();
        if (slotNum != null) {
            //look up slot
            if (getType() == CatscriptType.INT || (getType() == CatscriptType.BOOLEAN)) {
                code.addVarInstruction(Opcodes.ILOAD, slotNum);
            } else {
                code.addVarInstruction(Opcodes.ALOAD, slotNum);
            }
        } else {
            //look up field
            code.addVarInstruction(Opcodes.ALOAD, 0);
            code.addFieldInstruction(Opcodes.GETFIELD, getName(), descriptor, code.getProgramInternalName());

        }
    }


}
