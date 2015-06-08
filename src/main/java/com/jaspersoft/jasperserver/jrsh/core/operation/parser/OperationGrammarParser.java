package com.jaspersoft.jasperserver.jrsh.core.operation.parser;

import com.jaspersoft.jasperserver.jrsh.core.operation.Operation;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Master;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Parameter;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Prefix;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Value;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Grammar;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Rule;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.Rule.DefaultRule;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.graph.TokenEdge;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.graph.TokenEdgeFactory;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.Token;
import com.jaspersoft.jasperserver.jrsh.core.operation.parser.exception.CannotCreateTokenException;
import com.jaspersoft.jasperserver.jrsh.core.operation.parser.exception.OperationParseException;
import com.jaspersoft.jasperserver.jrsh.core.operation.parser.exception.WrongOperationFormatException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Alexander Krasnyanskiy
 */
public class OperationGrammarParser {
    private static final Logger log = Logger.getLogger(OperationGrammarParser.class);
    private static Graph<Token, TokenEdge<Token>> graph;
    private static Map<String, Pair<Token, String[]>> dependencies;
    private static Map<String, RuleGroup> groups;
    private static Token root;

    public static Grammar parse(final Operation operation) throws OperationParseException {
        graph = new DefaultDirectedGraph<>(new TokenEdgeFactory());
        dependencies = new HashMap<>();
        groups = new HashMap<>();

        Grammar grammar = new DefaultGrammar();
        Set<Rule> rules = new HashSet<>();
        Class<?> clazz = operation.getClass();
        Master master = clazz.getAnnotation(Master.class);
        //
        // Read annotation metadata and parse it to dependencies
        //
        if (master != null) {
            root = createToken(master.tokenClass(), master.name(), master.name(), true, true);
            if (master.tail()) {
                Rule rule = new DefaultRule();
                rule.addToken(root);
                rules.add(rule);
            }

            dependencies.put(root.getName(), new ImmutablePair<>(root, new String[]{}));
            graph.addVertex(root);

            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                Prefix prefix = field.getAnnotation(Prefix.class);
                Parameter param = field.getAnnotation(Parameter.class);

                if (param != null) {
                    OperationParameter p1 = new OperationParameter();
                    p1.getTokens().add(root);

                    for (String key : groups.keySet()) {
                        groups.get(key).getParameters().add(p1);
                    }
                    boolean isMandatory = param.mandatory();
                    Value[] values = param.values();
                    OperationParameter p2 = new OperationParameter();

                    for (Value v : values) {
                        Token token = createToken(v.tokenClass(), v.tokenAlias(),
                                                  v.tokenValue(), isMandatory, v.tail());
                        graph.addVertex(token);
                        if (prefix != null) {
                            Token prefixTkn = createToken(prefix.tokenClass(), prefix.value(),
                                    prefix.value(), isMandatory, false);

                            dependencies.put(prefixTkn.getName(), new ImmutablePair<>(prefixTkn, param.dependsOn()));
                            dependencies.put(token.getName(), new ImmutablePair<>(token, new String[]{
                                    prefix.value()
                            }));

                            p2.getTokens().add(prefixTkn);
                            graph.addVertex(prefixTkn);
                        } else {
                            dependencies.put(token.getName(), new ImmutablePair<>(token, param.dependsOn()));
                        }

                        p2.getTokens().add(token);
                        String[] ruleGroups = param.ruleGroups();

                        for (String group : ruleGroups) {
                            RuleGroup ruleGroup = groups.get(group);
                            if (ruleGroup != null) {
                                ruleGroup.getParameters().add(p2);
                            } else {
                                RuleGroup newRuleGroup = new RuleGroup();
                                newRuleGroup.getParameters().add(p1);
                                newRuleGroup.getParameters().add(p2);
                                groups.put(group, newRuleGroup);
                            }
                        }
                    }
                }
            }
        }
        //
        // Build token graph
        //
        buildEdgesInGraph();
        //
        // For tailed @Master
        //
        if (!(graph.vertexSet().size() == 1 && graph.vertexSet().contains(root))) {
            rules.addAll(buildRules());
        }
        //
        // Configure grammar
        //
        if (!rules.isEmpty()) {
            grammar.addRules(rules);
        } else {
            throw new WrongOperationFormatException();
        }

        return grammar;
    }

    protected static Set<Rule> buildRules() {
        KShortestPaths<Token, TokenEdge<Token>> paths = new KShortestPaths<>(graph, root, 2500);
        Set<Token> vertexes = graph.vertexSet();
        Set<Rule> rules = new LinkedHashSet<>();
        //
        // Use graph token vertexes to get all available
        // paths and convert each path into a rule
        //
        for (Token vertex : vertexes) {
            if (!vertex.equals(root)) {
                if (vertex.isTailOfRule()) {
                    //
                    // Get all available paths in graph
                    //
                    List<GraphPath<Token, TokenEdge<Token>>> ps = paths.getPaths(vertex);
                    for (GraphPath<Token, TokenEdge<Token>> path : ps) {
                        Rule rule = convertPathToRule(path);
                        //
                        // Check if rule is valid
                        //
                        if (isValidRule(rule)) {
                            rules.add(rule);
                        }
                    }
                }
            }
        }
        return rules;
    }

    protected static boolean isValidRule(final Rule rule) {
        List<Token> tokens = rule.getTokens();
        for (RuleGroup group : groups.values()) {
            if (group.getGroupTokens().containsAll(tokens)) {
                Set<OperationParameter> parameters = group.getParameters();
                for (OperationParameter parameter : parameters) {
                    Set<Token> mt = parameter.getOnlyMandatoryTokens();
                    if (mt.size() > 0) {
                        boolean notContains = true;
                        for (Token token : tokens) {
                            if (mt.contains(token)) {
                                notContains = false;
                            }
                        }
                        if (notContains) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    protected static Rule convertPathToRule(GraphPath<Token, TokenEdge<Token>> path) {
        List<TokenEdge<Token>> list = path.getEdgeList();
        Rule rule = new DefaultRule();
        Set<Token> set = new LinkedHashSet<>();
        for (TokenEdge<Token> edge : list) {
            set.add(edge.getSource());
            set.add(edge.getTarget());
        }
        for (Token token : set) {
            rule.addToken(token);
        }
        return rule;

    }

    protected static void buildEdgesInGraph() {
        for (Map.Entry<String, Pair<Token, String[]>> entry : dependencies.entrySet()) {
            Pair<Token, String[]> tokenPair = entry.getValue();
            for (String dependencyName : tokenPair.getRight()) {
                Pair<Token, String[]> dependency = dependencies.get(dependencyName);
                graph.addEdge(dependency.getLeft(), tokenPair.getLeft());
            }
        }
    }

    protected static Token createToken(Class<? extends Token> tokenType, String tokenName,
                                       String tokenValue, boolean mandatory, boolean tail)
            throws CannotCreateTokenException {
        try {
            return tokenType.getConstructor(String.class, String.class, boolean.class, boolean.class)
                    .newInstance(tokenName, tokenValue, mandatory, tail);
        } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException e) {
            throw new CannotCreateTokenException(tokenType);
        }
    }

    protected static class DefaultGrammar implements Grammar {
        private List<Rule> rules = new ArrayList<>();

        public DefaultGrammar(Rule... rules) {
            Collections.addAll(this.rules, rules);
        }

        @Override
        public List<Rule> getRules() {
            return rules;
        }

        @Override
        public void addRule(Rule rule) {
            rules.add(rule);
        }

        @Override
        public void addRules(Collection<Rule> rules) {
            this.rules.addAll(rules);
        }
    }

    protected static class RuleGroup {
        Set<OperationParameter> parameters = new HashSet<>();

        public RuleGroup() {
        }

        Set<Token> getGroupTokens() {
            Set<Token> set = new HashSet<>();
            for (OperationParameter parameter : parameters) {
                set.addAll(parameter.getTokens());
            }
            return set;
        }

        public Set<OperationParameter> getParameters() {
            return this.parameters;
        }

        public void setParameters(Set<OperationParameter> parameters) {
            this.parameters = parameters;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof RuleGroup)) return false;
            final RuleGroup other = (RuleGroup) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$parameters = this.getParameters();
            final Object other$parameters = other.getParameters();
            if (this$parameters == null ? other$parameters != null : !this$parameters.equals(other$parameters))
                return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $parameters = this.getParameters();
            result = result * PRIME + ($parameters == null ? 0 : $parameters.hashCode());
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof RuleGroup;
        }
    }

    protected static class OperationParameter {
        Set<Token> tokens = new HashSet<>();

        public OperationParameter() {
        }

        Set<Token> getOnlyMandatoryTokens() {
            Set<Token> mandatoryTokens = new HashSet<>();
            for (Token token : tokens) {
                if (token.isMandatory()) {
                    mandatoryTokens.add(token);
                }
            }
            return mandatoryTokens;
        }

        public Set<Token> getTokens() {
            return this.tokens;
        }

        public void setTokens(Set<Token> tokens) {
            this.tokens = tokens;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof OperationParameter)) return false;
            final OperationParameter other = (OperationParameter) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$tokens = this.getTokens();
            final Object other$tokens = other.getTokens();
            if (this$tokens == null ? other$tokens != null : !this$tokens.equals(other$tokens)) return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $tokens = this.getTokens();
            result = result * PRIME + ($tokens == null ? 0 : $tokens.hashCode());
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof OperationParameter;
        }
    }

}
