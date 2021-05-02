package edu.montana.csci.csci468;

import edu.montana.csci.csci468.parser.ErrorType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PartnersTest extends CatscriptTestBase {

    @Test
    void paramsConflictWithGlobalVars() {
        assertEquals(ErrorType.DUPLICATE_NAME, getParseError("var x = 10\n" +
                "function foo(x, y){ print(x) }\n" + "var y = 468"));
    }

    @Test
    void literalExpressionsEvaluatesProperly() {
        assertEquals(Arrays.asList(139, 2, 3), evaluateExpression("[(1 + (19 * (16 / 2) - 3) + -11), 2, 3]"));
        assertEquals(Arrays.asList(150, 366, 468), evaluateExpression("[((1 + (19 * (16 / 2) - 3))), 400-34, 400+68]"));
        assertEquals(Arrays.asList(true, false, 0, null), evaluateExpression("[true, false, 0, null]"));
    }

    @Test
    void unterminatedListParses() {
        assertEquals(ErrorType.UNTERMINATED_LIST, getParseError("[true, false, 0, null"));
    }
}
