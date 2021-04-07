package edu.montana.csci.csci468.parser;

import com.sun.source.tree.IdentifierTree;
import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import javax.swing.plaf.nimbus.State;
import java.awt.*;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        if (tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        if(tokens.match(FUNCTION)) {
            return parseFunctionDefinitionStatement();
        } else {
            return parseStatement();
        }
    }


    private Statement parseStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        } else if (tokens.match(VAR)) {
            return parseVariableStatement();
        } else if (tokens.match(IF)) {
            return parseIfStatement();
        } else if (tokens.match(FOR)) {
            return parseForStatement();
        } else if (tokens.match(IDENTIFIER)) {
            Token ident = tokens.consumeToken();
            if (tokens.match(EQUAL)) {
                return parseAssignmentStatement(ident); //assignment
            } else {
                return parseFunctionCallStatement(ident); //function call
            }
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }


    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    private Statement parseVariableStatement() {
        VariableStatement variableStatement = new VariableStatement();
        variableStatement.setStart(tokens.consumeToken());
        Token variableName = require(IDENTIFIER, variableStatement);
        if(tokens.matchAndConsume(COLON)) {
            TypeLiteral explicitType = parseTypeExpression();
            variableStatement.setExplicitType(explicitType.getType());
            if(tokens.match(GREATER)) {
                tokens.matchAndConsume(GREATER);
            }
        }

        require(EQUAL, variableStatement);
        variableStatement.setExpression(parseExpression());
        variableStatement.setEnd(tokens.getCurrentToken());
        variableStatement.setVariableName(variableName.getStringValue());
        return variableStatement;
    }

    private Statement parseIfStatement() {
        IfStatement ifStatement = new IfStatement();
        LinkedList<Statement> trueStatements = new LinkedList<Statement>();
        LinkedList<Statement> elseStatements = new LinkedList<Statement>();
        ifStatement.setStart(tokens.consumeToken());
        require(LEFT_PAREN, ifStatement);
        Expression condition = parseExpression();
        require(RIGHT_PAREN, ifStatement);
        require(LEFT_BRACE, ifStatement);
        while(!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
            Statement statements = parseProgramStatement();
            trueStatements.add(statements);
        }
        require(RIGHT_BRACE, ifStatement);
        if (tokens.matchAndConsume(ELSE)) {
            if(tokens.match(LEFT_BRACE)) {
                require(LEFT_BRACE, ifStatement);
                while(!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                    Statement statements = parseProgramStatement();
                    elseStatements.add(statements);
                }
                require(RIGHT_BRACE, ifStatement);
            } else { // else if
                elseStatements.add(parseIfStatement());
            }
        }

        ifStatement.setExpression(condition);
        ifStatement.setTrueStatements(trueStatements);
        ifStatement.setElseStatements(elseStatements);
        return ifStatement;
    }

    private Statement parseForStatement () {
        ForStatement forStatement = new ForStatement();
        Token start  = tokens.consumeToken();
        require(LEFT_PAREN, forStatement);
        Token loopIdentifier = require(IDENTIFIER, forStatement);
        require(IN, forStatement);
        Expression condition = parseExpression();
        require(RIGHT_PAREN, forStatement);
        require(LEFT_BRACE, forStatement);
        List<Statement> body = new LinkedList<>();
        while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
            body.add(parseProgramStatement());
        }
        require(RIGHT_BRACE, forStatement);
        forStatement.setVariableName(loopIdentifier.getStringValue());
        forStatement.setStart(start);
        forStatement.setExpression(condition);
        forStatement.setBody(body);
        return forStatement;
    }

    private Statement parseAssignmentStatement(Token assignmentIdentifier) {
        AssignmentStatement assignStmt = new AssignmentStatement();
        require(EQUAL, assignStmt);
        assignStmt.setStart(assignmentIdentifier);
        assignStmt.setExpression(parseExpression());
        assignStmt.setVariableName(assignmentIdentifier.getStringValue());
        return assignStmt;
    }

    private Statement parseFunctionDefinitionStatement() {
        //function_declaration = 'function', IDENTIFIER, '(', parameter_list, ')' + [ ':' + type_expression ] + "{" + { function_body_statement } + "}";
        Token startToken = tokens.consumeToken();
        FunctionDefinitionStatement functionDefinitionStatement = new FunctionDefinitionStatement();
        functionDefinitionStatement.setStart(startToken);
        Token functionName = require(IDENTIFIER, functionDefinitionStatement);
        require(LEFT_PAREN, functionDefinitionStatement);
        List<CatscriptType> argumentTypeList = new ArrayList<>();
        List<String> argumentList = new ArrayList<>();
        while (!tokens.match(RIGHT_PAREN) && !tokens.match(EOF)) {
            Token paramIdentifier = require(IDENTIFIER, functionDefinitionStatement);
            if(tokens.matchAndConsume(COLON)) {
                functionDefinitionStatement.addParameter(paramIdentifier.getStringValue(), parseTypeExpression());
            } else {
                TypeLiteral typeLiteral = new TypeLiteral();
                typeLiteral.setType(CatscriptType.OBJECT);
                functionDefinitionStatement.addParameter(paramIdentifier.getStringValue(), typeLiteral);
            }
            if (!tokens.match(RIGHT_PAREN)) {
                require(COMMA, functionDefinitionStatement);
            }
        }
        require(RIGHT_PAREN, functionDefinitionStatement);
        TypeLiteral returnType = new TypeLiteral();
        returnType.setType(CatscriptType.VOID);
        if(tokens.match(COLON)) {
            require(COLON, functionDefinitionStatement);
            returnType = parseTypeExpression();
        }
        require(LEFT_BRACE, functionDefinitionStatement);
        List<Statement> body = new LinkedList<>();
        while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
            if (tokens.match(RETURN)) {
                body.add(parseReturnStatement(functionDefinitionStatement));
            } else {
                body.add(parseStatement());
            }
        }
        require(RIGHT_BRACE, functionDefinitionStatement);
        functionDefinitionStatement.setType(returnType);
        functionDefinitionStatement.setBody(body);
        functionDefinitionStatement.setName(functionName.getStringValue());
        return functionDefinitionStatement;
    }

    private Statement parseFunctionCallStatement(Token functionIdentifier) {
        List<Expression> args = new ArrayList<>();
        while(!tokens.match(RIGHT_PAREN) && !tokens.match(EOF)){
            Expression elem = parseExpression();
            args.add(elem);
            if(!tokens.match(RIGHT_PAREN)) {
                tokens.consumeToken(); // comma
            }
        }
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(functionIdentifier.getStringValue(), args);
        require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);


        //FunctionCallExpression funcCallExpr = parsePrimaryExpression();
        //require(LEFT_PAREN, )
        //FunctionCallExpression funcCallExpr = new FunctionCallExpression(functionIdentifier.getStringValue(), );
        FunctionCallStatement funcStmt = new FunctionCallStatement(functionCallExpression);
        funcStmt.setStart(functionIdentifier);
        return funcStmt;
    }

    private Statement parseReturnStatement(FunctionDefinitionStatement functionDefinitionStatement) {
        ReturnStatement returnStatement = new ReturnStatement();
        Token start = tokens.consumeToken();
        returnStatement.setStart(start);
        returnStatement.setFunctionDefinition(functionDefinitionStatement);
        if (tokens.match(RIGHT_BRACE)) {
            returnStatement.setEnd(start);
            return returnStatement;
            //returnStatement.setExpression(null);
        } else {
            returnStatement.setExpression(parseExpression());
            //returnStatement.setEnd(tokens.getCurrentToken());
            return returnStatement;
        }

    }


    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            if(tokens.match(LEFT_PAREN)){ //FUNCTION CALL
                tokens.consumeToken();
                List<Expression> args = new ArrayList<>();
                while(!tokens.match(RIGHT_PAREN) && !tokens.match(EOF)){
                    Expression elem = parseExpression();
                    args.add(elem);
                    if(tokens.match(COMMA)) {
                        tokens.consumeToken(); // comma
                    }
                }
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifierToken.getStringValue(), args);
                require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
                return functionCallExpression;
            }else {
                IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
                identifierExpression.setToken(identifierToken);
                return identifierExpression;
            }
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(TRUE)) {
            Token booleanToken = tokens.consumeToken();
            BooleanLiteralExpression booleanExpression = new BooleanLiteralExpression(true);
            booleanExpression.setToken(booleanToken);
            return booleanExpression;
        } else if (tokens.match(FALSE)) {
            Token booleanToken = tokens.consumeToken();
            BooleanLiteralExpression booleanExpression = new BooleanLiteralExpression(false);
            booleanExpression.setToken(booleanToken);
            return booleanExpression;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        } else if (tokens.match(LEFT_BRACKET)){ //LIST LITERAL
            tokens.consumeToken();
            List<Expression> list = new ArrayList<>();
            while(!tokens.match(RIGHT_BRACKET) && !tokens.match(EOF)) {
                Expression elem = parseExpression();
                list.add(elem);
                if (tokens.match(COMMA)){
                    tokens.consumeToken();
                }
            }
            ListLiteralExpression listLiteralExpression = new ListLiteralExpression(list);
            if (!tokens.match(RIGHT_BRACKET)) {
                listLiteralExpression.addError(ErrorType.UNTERMINATED_LIST);
            } else {
                tokens.consumeToken();
            }
            return listLiteralExpression;
        } else if (tokens.match(LEFT_PAREN)){
            Token leftParenToken = tokens.consumeToken();
            Expression rhs = parseExpression();
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(rhs);
            parenthesizedExpression.setStart(leftParenToken);
            parenthesizedExpression.setEnd(require(RIGHT_PAREN, rhs));
            return parenthesizedExpression;
        }else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    /*private CatscriptType parseTypeExpression(Token token){
        if (token.getStringValue() == "int") {
            return CatscriptType.INT;
        } else if (token.getStringValue() == "string") {
            return CatscriptType.STRING;
        } else if (token.getStringValue() == "bool") {
            return CatscriptType.BOOLEAN;
        } else if (token.getStringValue() == "object") {
            return CatscriptType.OBJECT;
        }
        return null;
    }*/

    private TypeLiteral parseTypeExpression(){
        Token startToken = tokens.consumeToken();
        if(startToken.getType() == IDENTIFIER) {
            if (startToken.getStringValue().equals("int")) {
                TypeLiteral typeLiteral = new TypeLiteral();
                typeLiteral.setToken(startToken);
                typeLiteral.setType(CatscriptType.INT);
                return typeLiteral;
            } else if (startToken.getStringValue().equals("bool")) {
                TypeLiteral typeLiteral = new TypeLiteral();
                typeLiteral.setToken(startToken);
                typeLiteral.setType(CatscriptType.BOOLEAN);
                return typeLiteral;
            } else if (startToken.getStringValue().equals("list")) {
                if (tokens.matchAndConsume(LESS)) {
                    TypeLiteral componentTypeLiteral = parseTypeExpression();
                    CatscriptType componentType = componentTypeLiteral.getType();
                    CatscriptType listType = CatscriptType.getListType(componentType);
                    TypeLiteral typeLiteral = new TypeLiteral();
                    typeLiteral.setStart(startToken);
                    typeLiteral.setType(listType);
                    return typeLiteral;
                } else {
                    TypeLiteral typeLiteral = new TypeLiteral();
                    typeLiteral.setType(CatscriptType.getListType(CatscriptType.OBJECT));
                    return typeLiteral;
                }
            }else if (startToken.getStringValue().equals("string")) {
                TypeLiteral typeLiteral = new TypeLiteral();
                typeLiteral.setToken(startToken);
                typeLiteral.setType(CatscriptType.STRING);
                return typeLiteral;
            }else if (startToken.getStringValue().equals("object")) {
                TypeLiteral typeLiteral = new TypeLiteral();
                typeLiteral.setToken(startToken);
                typeLiteral.setType(CatscriptType.OBJECT);
                return typeLiteral;
            }
        }
        return null;
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }
}
