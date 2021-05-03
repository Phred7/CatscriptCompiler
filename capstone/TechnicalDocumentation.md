# Section 4: Technical Report
## Introduction  
The program is a compiler for Catscript written entirely in Java. Catscript is a 
statically typed language that supports standard if statements, for loops, and 
function definitions as well as List Literals and local variable type interface, 
two features that Java does not have.   
### File Structure  
The code for this project is in `/src/main/java/edu/montana/csci/csci468`. In 
this folder is `tokenizer`, `parser`, and `bytecode` which are the main folders 
for this project. `tokenizer` contains the code for the tokenizer in `CatScriptTokenizer.java` 
`parser` contains the parser `CatScriptParser` as well as the folders `statements` 
and `expressions` which have various files for all the statements and expressions 
that CatScript supports.  
The tests for this project are in `/src/test/java/edu/montana/csci/csci468`. In this 
folder are the folders `tokenizer`, `parser`, `eval`, and `bytecode` which contain all 
multiple files that run tests to check the program is running as expected.  
### Typesystem  
Catscript is a statically typed language with a fairly simple type system. 
-  `int` - integers  
- `string` - Strings  
- `bool` - a Boolean value  
- `object` - any object  
- `null` - the type of null value  
- `void` - the type no type  
  
Catscript has one complex type `list`. A list can be declared with given type with `list<T>`.    
- `list<int>` - list of integers  
- `list<object>` - list of objects  
- `list<list<int>>` - list of lists of integers
Catscript elements have a `verify` method that verifies the code. It registers all function 
types in the symbol table, calls `validate`, collects parse errors from children, and throws 
if any errors occurred.   
#### Assignability  
For simple types, assignability is fairly simple: 
Nothing is assignable from `void`, everything is assignable from `null`. For all other types, 
check the assignability of the backing java classes.
Lists have different assignability. Catscript lists are covariant and immutable: 
Null is assignable to list. Otherwise check if it is list type, if so then check if the 
component types are assignable. 
  
## Tokenizing  
The tokenizer simplifies the grammar and optimizes the parser by turning strings into 
tokens. The goal of the tokenizer is to understand the syntax of the language rather 
than the meaning behind each token. The tokenizer is responsible for identifying tokens 
and maintaining the line the token is on.  
Below is the method in the tokenizer that consumes the whitespace between each 
token. You can see how the tokenizer keeps track of where it is in the line by adding 
to the offset each time whitespace is used. When a new line token is present in the input 
document, the tokenizer resets the `lineOffset` variable and adds to `line` to keep track 
of which line it is on.   
```java
private void consumeWhitespace() {
    while (!tokenizationEnd()) {
        char c = peek();
        if (c == ' ' || c == '\r' || c == '\t') {
            lineOffset++;
            postion++;
            continue;
        } else if (c == '\n') {
            lineOffset = 0;
            line++;
            postion++;
            continue;
        }
        break;
    }
}
```  
The `scanSyntax` method is the method that is responsible for parsing the syntax for Catscript. 
It checks each syntax token, if that token matches it adds the token to the list of tokens that 
will later be sent to the parser. Below is a snippet of this method.  
```java
private void scanSyntax() {
    int start = postion;
    if(matchAndConsume('+')) {
        tokenList.addToken(PLUS, "+", start, postion, line, lineOffset);
    } else if(matchAndConsume('-')) {
        tokenList.addToken(MINUS, "-", start, postion, line, lineOffset);
...
    } else if (matchAndConsume(']')) {
        tokenList.addToken(RIGHT_BRACKET, "]", start, postion, line, lineOffset);
    } else if (matchAndConsume(':')) {
        tokenList.addToken(COLON, ":", start, postion, line, lineOffset);
...
```   
   
## Parsing  
The parser takes the list of tokens from the tokenizer and verifies that the string is part of 
Catscript's grammar. If any syntax errors are found, it throws an error with the location of the 
syntax error provided by the tokenizer. Because recursive descent was used to build the parser, 
the structure of the parser looks similar to the grammar that Catscript recognizes.  
  
## Evaluating  
### Literals  
A literal is a literal value encoded into the programming language, evaluating these values is fairly 
simple. The values are returned when evaluated. Below is the evaluate method for a integer literal. 
Strings, booleans, null, and list literals are all evaluated this way.   
```java 
@Override
public Object evaluate(CatscriptRuntime runtime) {
    return integerVal;
}
```
### Parentheses  
Parenthesis are evaluated by returning the value the enclosed expression evaluates to. 
```java
@Override
public Object evaluate(CatscriptRuntime runtime) {
    return expression.evaluate(runtime);
}
```  
### Unary Expressions  
Unary expressions are expressions with a single argument expression and are evaluated by evaluating 
the right hand side and applying the operator to that value. Below is the evaluate method for unary 
expressions. 
```java 
@Override
public Object evaluate(CatscriptRuntime runtime) {
    Object rhsValue = getRightHandSide().evaluate(runtime);
    if (this.isMinus()) {
        return -1 * (Integer) rhsValue;
    } else if (this.isNot()) {
        if (rhsValue.equals(true)) {
            return false;
        } else {
            return true;
        }
    } else {
        return false;
    }
}
 ```
### Binary Expressions  
Binary expressions are expressions that have to arguments, a left hand and a right hand side. Binary 
expressions are evaluated by checking the type then concatenating or adding the two values together 
and returning the resulting value. Below is the evaluate method in `AdditiveExpression.java` for 
evaluating a binary expression.  
```java
@Override
public Object evaluate(CatscriptRuntime runtime) {
    if (leftHandSide.getType().equals(CatscriptType.STRING) || rightHandSide.getType().equals(CatscriptType.STRING)) {
        Object leftHandValue = leftHandSide.evaluate(runtime);
        Object rightHandValue = rightHandSide.evaluate(runtime);
        if (leftHandValue == null) {
            leftHandValue = "null";
        }
        if (rightHandValue == null) {
            rightHandValue = "null";
        }
        return leftHandValue.toString() + rightHandValue.toString();
    } else {
        Integer leftHandValue = (Integer) leftHandSide.evaluate(runtime);
        Integer rightHandValue = (Integer) rightHandSide.evaluate(runtime);
        if (isAdd()) {
            return leftHandValue + rightHandValue;
        } else {
            return leftHandValue - rightHandValue;
        }
    }
}
```  
  
## Compiling
The compile methods for each statement and expressions are written bytecode. We write what we need to do in 
Catscript into our Java IDE and use the bytecode that is generated to write the compile method for the 
expressions and statements. To compile an expression, we start by compiling the right hand side and adding it 
onto the stack. Depending on the expression, the left hand side will also be compiled and added to the stack. 
Both the right hand and left hand side will compile to a set of instructions and leave the value on the stack. 
The instruction is then run with that value. Opcodes are also used to compile expressions and statements. 
Different opcodes have different meanings as well. 
