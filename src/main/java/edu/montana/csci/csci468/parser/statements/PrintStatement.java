package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

import java.io.PrintStream;
import java.util.Objects;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class PrintStatement extends Statement {
    private Expression expression;

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }


    public Expression getExpression() {
        return expression;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        getProgram().print(expression.evaluate(runtime));
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        expression.compile(code);
        //code.addMethodInstruction(Opcodes.GETSTATIC, internalNameFor(System.class), "out", "Ljava/io/PrintStream;");
        //

        //box(code, expression.getType());

        //code.addInstruction(Opcodes.ALOAD);
        code.addMethodInstruction(Opcodes.INVOKEVIRTUAL, internalNameFor(PrintStream.class),
                "println", "(Ljava/lang/Object;)V");
        //super.compile(code);
    }

}
