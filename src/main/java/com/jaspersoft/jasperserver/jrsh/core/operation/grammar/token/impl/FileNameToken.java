package com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl;

import com.google.common.io.Files;
import com.jaspersoft.jasperserver.jrsh.core.completion.impl.FileCompleter;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.AbstractToken;
import jline.console.completer.Completer;

import java.io.File;

/**
 * @author Alexander Krasnyanskiy
 */
public class FileNameToken extends AbstractToken {

    public FileNameToken(String name, String value, boolean mandatory, boolean tailOfRule) {
        super(name, value, mandatory, tailOfRule);
    }

    @Override
    public Completer getCompleter() {
        return new FileCompleter();
    }

    @Override
    public boolean match(String input) {
        Files.isFile().apply(new File(input));
        return true;
    }

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof FileNameToken)) return false;
        final FileNameToken other = (FileNameToken) o;
        return other.canEqual((Object) this) && super.equals(o);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + super.hashCode();
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof FileNameToken;
    }
}
