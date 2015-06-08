package com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl;

import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.AbstractToken;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

/**
 * @author Alexander Krasnyanskiy
 */
public class StringToken extends AbstractToken {

    public StringToken(String name, String value, boolean mandatory, boolean tailOfRule) {
        super(name, value, mandatory, tailOfRule);
    }

    @Override
    public boolean isTailOfRule() {
        return tailOfRule;
    }

    @Override
    public Completer getCompleter() {
        return new StringsCompleter(value);
    }

    @Override
    public boolean match(String input) {
        return input.equals(value);
    }

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof StringToken)) return false;
        final StringToken other = (StringToken) o;
        return other.canEqual((Object) this) && super.equals(o);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + super.hashCode();
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof StringToken;
    }
}
