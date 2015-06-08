package com.jaspersoft.jasperserver.jrsh.core.operation.parser;

import com.jaspersoft.jasperserver.jrsh.core.operation.Operation;
import com.jaspersoft.jasperserver.jrsh.core.operation.OperationFactory;
import com.jaspersoft.jasperserver.jrsh.core.operation.OperationReflector;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Grammar;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Lexer;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Lexer.DefaultLexer;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Rule;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.Token;
import com.jaspersoft.jasperserver.jrsh.core.operation.parser.exception.OperationParseException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Alexander Krasnyanskiy
 */
public class LL1OperationParser implements OperationParser {

    private static final Logger log = Logger.getLogger(LL1OperationParser.class);
    private Lexer lexer;

    public LL1OperationParser() {
        lexer = new DefaultLexer();
    }

    public Operation parse(String line) throws OperationParseException {
        List<String> inputTokens = lexer.getTokens(line);
        String operationName = inputTokens.get(0);

        Operation operation = OperationFactory.createOperationByName(operationName);
        Conditions.checkOperation(operation);
        //
        // Retrieve grammar from operation metadata
        // via annotations
        //
        Grammar grammar = OperationGrammarParser.parse(operation);
        List<Rule> grammarRules = grammar.getRules();
        boolean matchedRuleExist = false;
        //
        // Match tokens and configure operation instance
        //
        for (Rule rule : grammarRules) {
            List<Token> ruleTokens = rule.getTokens();
            if (match(ruleTokens, inputTokens)) {
                OperationReflector.set(operation, ruleTokens, inputTokens);
                matchedRuleExist = true;
            }
        }
        //
        // If unmatched then throw exception
        //
        Conditions.checkMatchedRulesFlag(matchedRuleExist);
        return operation;
    }

    protected boolean match(List<Token> ruleTokens, List<String> inputTokens) {
        //
        // Check tokens length
        //
        if (ruleTokens.size() != inputTokens.size()) {
            return false;
        }
        //
        // If at least one token in rule is unmatched,
        // return `false` immediately
        //
        for (int i = 0; i < ruleTokens.size(); i++) {
            if (!ruleTokens.get(i).match(inputTokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    public void setLexer(Lexer lexer) {
        this.lexer = lexer;
    }
}
