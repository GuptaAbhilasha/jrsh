package com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token;

import jline.console.completer.Completer;

/**
 * @author Alexander Krasnyanskiy
 */
public abstract class AbstractToken implements Token {

    protected String name;
    protected String value;
    protected boolean mandatory;
    protected boolean tailOfRule;

    //@ConstructorProperties({"name", "value", "mandatory", "tailOfRule"})
    public AbstractToken(String name, String value, boolean mandatory, boolean tailOfRule) {
        this.name = name;
        this.value = value;
        this.mandatory = mandatory;
        this.tailOfRule = tailOfRule;
    }

    @Override
    public abstract Completer getCompleter();

    @Override
    public abstract boolean match(String input);

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isMandatory() {
        return this.mandatory;
    }

    public boolean isTailOfRule() {
        return this.tailOfRule;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AbstractToken)) return false;
        final AbstractToken other = (AbstractToken) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        if (this.isMandatory() != other.isMandatory()) return false;
        if (this.isTailOfRule() != other.isTailOfRule()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 0 : $name.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 0 : $value.hashCode());
        result = result * PRIME + (this.isMandatory() ? 79 : 97);
        result = result * PRIME + (this.isTailOfRule() ? 79 : 97);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof AbstractToken;
    }
}
