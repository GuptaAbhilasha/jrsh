package com.jaspersoft.jasperserver.jrsh.core.common.exception;

import static java.lang.String.format;

/**
 * @author Alexander Krasnyanskiy
 */
public class DirectoryDoesNotExistException extends RuntimeException {
    public DirectoryDoesNotExistException(String directory) {
        super(format("Directory %s does not exist", directory));
    }
}
