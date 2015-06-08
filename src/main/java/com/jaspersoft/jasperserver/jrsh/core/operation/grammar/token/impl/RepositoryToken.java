package com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl;

import com.jaspersoft.jasperserver.jrsh.core.completion.impl.RepositoryCompleter;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.AbstractToken;
import jline.console.completer.Completer;

/**
 * @author Alexander Krasnyanskiy
 */
public class RepositoryToken extends AbstractToken {

    public RepositoryToken(String name, String value, boolean mandatory, boolean tailOfRule) {
        super(name, value, mandatory, tailOfRule);
    }

    @Override
    public Completer getCompleter() {
        return new RepositoryCompleter();
    }

    @Override
    public boolean match(String input) {
        return input.startsWith("/") && !input.endsWith("/");
    }

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof RepositoryToken)) return false;
        final RepositoryToken other = (RepositoryToken) o;
        return other.canEqual((Object) this) && super.equals(o);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + super.hashCode();
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof RepositoryToken;
    }
}
