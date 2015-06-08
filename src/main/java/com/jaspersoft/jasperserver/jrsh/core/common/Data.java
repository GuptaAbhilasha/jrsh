package com.jaspersoft.jasperserver.jrsh.core.common;

import java.util.List;

/**
 * @author Alexander Krasnyanskiy
 */
public class Data {
    private List<String> source;

    @java.beans.ConstructorProperties({"source"})
    public Data(List<String> source) {
        this.source = source;
    }

    public List<String> getSource() {
        return this.source;
    }
}
