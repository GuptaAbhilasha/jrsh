package com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl;

import com.jaspersoft.jasperserver.jrsh.core.completion.impl.SkippedInputCompleter;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.AbstractToken;
import jline.console.completer.Completer;

/**
 * @author Alexander Krasnyanskiy
 */
public class SkipInputToken extends AbstractToken {

    public SkipInputToken(String name, String value, boolean mandatory, boolean tailOfRule) {
        super(name, value, mandatory, tailOfRule);
    }

    @Override
    public boolean isTailOfRule() {
        return tailOfRule;
    }

    @Override
    public Completer getCompleter() {
        return new SkippedInputCompleter();
    }

    @Override
    public boolean match(String input) {
        return true;
    }

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SkipInputToken)) return false;
        final SkipInputToken other = (SkipInputToken) o;
        return other.canEqual((Object) this) && super.equals(o);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + super.hashCode();
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof SkipInputToken;
    }
}
