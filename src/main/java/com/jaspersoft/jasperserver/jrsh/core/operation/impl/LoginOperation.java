package com.jaspersoft.jasperserver.jrsh.core.operation.impl;

import com.jaspersoft.jasperserver.jaxrs.client.core.Session;
import com.jaspersoft.jasperserver.jrsh.core.common.SessionFactory;
import com.jaspersoft.jasperserver.jrsh.core.operation.Operation;
import com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Master;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Parameter;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Value;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.TokenPreconditions;
import com.jaspersoft.jasperserver.jrsh.core.operation.parser.exception.WrongConnectionStringFormatException;

import static com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult.ResultCode.FAILED;
import static com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult.ResultCode.SUCCESS;
import static java.lang.String.format;

/**
 * @author Alexander Krasnyanskiy
 */
@Master(name = "login",
        usage = "login [username]|[organization]%[password]@[url]",
        description = "Operation <login> is used to login into JRS")
public class LoginOperation implements Operation {

    public static int counter = 0;
    //
    // All messages will be moved to i18n in the next release
    //
    public static final String OK_MSG = "You have logged in";
    public static final String FORMATTED_OK_MSG = "You have logged in as %s";
    public static final String FAILURE_MSG = "Login failed";
    public static final String FORMATTED_FAILURE_MSG = "Login failed (%s)";

    private String server;
    private String username;
    private String password;
    private String organization;

    @Parameter(mandatory = true, dependsOn = "login", values =
    @Value(tokenAlias = "CS", tail = true))
    private String connectionString;

    @Override
    public OperationResult eval(Session ignored) {
        //
        // Log in
        //
        OperationResult result;
        try {
            SessionFactory.createSharedSession(server, username, password, organization);
            result = new OperationResult(format(FORMATTED_OK_MSG, username), SUCCESS, this, null);
            //
            // Counting only successful attempts
            //
            counter++;
        } catch (Exception err) {
            result = new OperationResult(format(FORMATTED_FAILURE_MSG, err.getMessage()), FAILED, this, null);
        }
        return result;
    }

    public void setConnectionString(String connectionString) {
        if (!TokenPreconditions.isConnectionString(connectionString)) {
            throw new WrongConnectionStringFormatException();
        }
        this.connectionString = connectionString;
        String[] tokens = connectionString.split("[@]");
        switch (tokens.length) {
            case 2:
                server = tokens[1].trim();
                tokens = tokens[0].split("[%]");
                switch (tokens.length) {
                    case 2:
                        password = tokens[1].trim();
                        tokens = tokens[0].split("[|]");
                        switch (tokens.length) {
                            case 2:
                                username = tokens[0].trim();
                                organization = tokens[1].trim();
                                break;
                            case 1:
                                username = tokens[0].trim();
                                break;
                        }
                        break;
                    case 1:
                        tokens = tokens[0].split("[|]");
                        switch (tokens.length) {
                            case 2:
                                username = tokens[0].trim();
                                organization = tokens[1].trim();
                                break;
                            default:
                                username = tokens[0].trim();
                                break;
                        }
                        break;
                }
                break;
        }
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getServer() {
        return this.server;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getOrganization() {
        return this.organization;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
