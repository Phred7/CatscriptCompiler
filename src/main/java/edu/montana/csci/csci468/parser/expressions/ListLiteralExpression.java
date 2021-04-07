package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.SymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ListLiteralExpression extends Expression {
    List<Expression> values;
    private CatscriptType type;

    public ListLiteralExpression(List<Expression> values) {
        this.values = new ArrayList<>();
        for (Expression value : values) {
            this.values.add(addChild(value));
        }
    }

    public List<Expression> getValues() {
        return values;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        for (Expression value : values) {
            value.validate(symbolTable);
        }
        if (values.size() > 0) {
            CatscriptType inferedType = CatscriptType.NULL;
            type = CatscriptType.getListType(values.get(0).getType());
            for (Expression value : this.values) {
                CatscriptType componentType = value.getType();
                if (!inferedType.isAssignableFrom(componentType)) {
                    if (inferedType == CatscriptType.NULL) {
                        inferedType = componentType;
                    } else {
                        inferedType = CatscriptType.OBJECT;
                    }
                    //addError(ErrorType.INCOMPATIBLE_TYPES); //necessary?
                }
            }
            inferedType = CatscriptType.getListType(values.get(0).getType());
        } else {
            type = CatscriptType.getListType(CatscriptType.OBJECT);
        }
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        List<Object> list = new ArrayList<>();
        for (Expression value : values) {
            Object ob = value.evaluate(runtime);
            list.add(ob);
            //runtime.setValue();
        }
        return list;
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        super.compile(code);
    }


}
