package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class VariableStatement extends Statement {
    private Expression expression;
    private String variableName;
    private CatscriptType explicitType;
    private CatscriptType type;

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public void setExplicitType(CatscriptType type) {
        this.explicitType = type;
    }

    public CatscriptType getExplicitType() {
        return explicitType;
    }

    public boolean isGlobal() {
        return getParent() instanceof CatScriptProgram;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            // TODO if there is an explicit type, ensure it is correct
            //      if not, infer the type from the right hand side expression
            if(getExplicitType() != null) {
                this.type = expression.getType();
                if(!getExplicitType().equals(this.type)){
                    if(!this.getExplicitType().isAssignableFrom(this.type)){
                        addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
            } else {
                this.type = expression.getType();
                setExplicitType(expression.getType());
            }
            symbolTable.registerSymbol(variableName, type);
        }
    }

    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        runtime.setValue(getVariableName(), expression.evaluate(runtime));
        return;
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        if(isGlobal()){ //store in field
            String descriptor;
            if (getType() == CatscriptType.INT || getType() == CatscriptType.BOOLEAN) {
                descriptor = "I";
            } else {
                descriptor = "L" + internalNameFor(getType().getJavaType()) + ";";
            }
            code.addVarInstruction(Opcodes.ALOAD, 0);
            expression.compile(code);
            code.addField(getVariableName(), descriptor);
            code.addFieldInstruction(Opcodes.PUTFIELD, getVariableName(), descriptor, code.getProgramInternalName());
        } else { //store in slot
            expression.compile(code);
            Integer slotForVar = code.createLocalStorageSlotFor(getVariableName());
            if (getType() == CatscriptType.INT || (getType() == CatscriptType.BOOLEAN)) {
                code.addVarInstruction(Opcodes.ISTORE, slotForVar); // if not an int (or bool)
            } else {
                code.addVarInstruction(Opcodes.ASTORE, slotForVar); // if not an int
            }

        }

    }
}
