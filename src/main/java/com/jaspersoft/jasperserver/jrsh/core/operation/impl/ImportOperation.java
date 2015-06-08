package com.jaspersoft.jasperserver.jrsh.core.operation.impl;

import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.importservice.ImportParameter;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.importservice.ImportTaskRequestAdapter;
import com.jaspersoft.jasperserver.jaxrs.client.core.Session;
import com.jaspersoft.jasperserver.jaxrs.client.dto.importexport.StateDto;
import com.jaspersoft.jasperserver.jrsh.core.common.ZipUtil;
import com.jaspersoft.jasperserver.jrsh.core.operation.Operation;
import com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Master;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Parameter;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Value;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl.FileNameToken;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl.StringToken;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult.ResultCode.FAILED;
import static com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult.ResultCode.SUCCESS;
import static java.lang.String.format;

/**
 * @author Alexander Krasnyanskiy
 */
@Master(name = "import",
        usage = "import [file]",
        description = "Operation <import> is used to import resources to JRS")
public class ImportOperation implements Operation {

    public static final String OK_MSG = "Import status: Success";
    public static final String FAILURE_MSG = "Import failed";
    public static final String FORMATTED_FAILURE_MSG = "Import failed (%s)";
    public static final String UNKNOWN_CONTENT = "Neither a file nor a directory";
    public static final String IO_WARNING = "Could not delete a temporary file";
    private static final Logger log = Logger.getLogger(ImportOperation.class);

    @Parameter(mandatory = true, dependsOn = {"import"}, values =
    @Value(tail = true, tokenClass = FileNameToken.class, tokenAlias = "IPTH"))
    private String path;

    @Parameter(dependsOn = {"IPTH", "IIME", "IIAE", "IISS", "ISUU", "IWA"}, values =
    @Value(tokenAlias = "IIUR", tokenClass = StringToken.class,
            tokenValue = "with-include-audit-events", tail = true))
    private String withIncludeAuditEvents;

    @Parameter(dependsOn = {"IPTH", "IIUR", "IIAE", "IISS", "ISUU", "IWA"}, values =
    @Value(tokenAlias = "IIME", tokenClass = StringToken.class,
            tokenValue = "with-include-monitoring-events", tail = true))
    private String withIncludeMonitoringEvents;

    @Parameter(dependsOn = {"IPTH", "IIUR", "IIME", "IISS", "ISUU", "IWA"}, values =
    @Value(tokenAlias = "IIAE", tokenClass = StringToken.class,
            tokenValue = "with-include-access-events", tail = true))
    private String withIncludeAccessEvents;

    @Parameter(dependsOn = {"IWA", "ISUU", "IIAE", "IIME", "IIUR", "IPTH"}, values =
    @Value(tokenAlias = "IISS", tokenClass = StringToken.class,
            tokenValue = "with-include-server-settings", tail = true))
    private String withIncludeServerSettings;

    @Parameter(dependsOn = {"IWA", "IISS", "IIAE", "IIME", "IIUR", "IPTH"}, values =
    @Value(tokenAlias = "ISUU", tokenClass = StringToken.class,
            tokenValue = "with-skip-user-update", tail = true))
    private String withSkipUserUpdate;

    @Parameter(dependsOn = {"ISUU", "IISS", "IIAE", "IIME", "IIUR", "IPTH"}, values =
    @Value(tokenAlias = "IWA", tokenClass = StringToken.class,
            tokenValue = "with-update", tail = true))
    private String withUpdate;

    public ImportOperation() {
    }

    @Override
    public OperationResult eval(Session session) {
        //
        // Import zip/directory
        //
        OperationResult result;
        try {
            File content = new File(path);
            if (content.isDirectory()) {
                File importFile = ZipUtil.pack(path);
                ImportTaskRequestAdapter task = session.importService().newTask();
                //
                // Add parameters
                //
                for (ImportParameter parameter : convertImportParameters()) {
                    task.parameter(parameter, true);
                }
                StateDto entity = task.create(importFile).getEntity();
                String phase = wait(entity, session);
                //
                // Clean up zip file
                //
                if (importFile.exists()) {
                    //
                    // Delete temporary zip file
                    //
                    boolean isDeleted = importFile.delete();
                    if (!isDeleted) {
                        log.info(IO_WARNING);
                    }
                }
                //
                // Check task phase
                //
                switch (phase) {
                    case "finished":
                        result = new OperationResult(OK_MSG, SUCCESS, this, null);
                        break;
                    default:
                        //
                        // Failed
                        //
                        result = new OperationResult(FAILURE_MSG, FAILED, this, null);
                        break;
                }
            } else if (content.isFile()) {
                //
                // Upload resources
                //
                ImportTaskRequestAdapter task = session.importService().newTask();
                //
                // Add parameters
                //
                for (ImportParameter parameter : convertImportParameters()) {
                    task.parameter(parameter, true);
                }
                StateDto entity = task.create(new File(path)).getEntity();
                //
                // Wait until execution is completed
                //
                wait(entity, session);
                result = new OperationResult(OK_MSG, SUCCESS, this, null);
            } else {
                result = new OperationResult(UNKNOWN_CONTENT, FAILED, this, null);
            }
        } catch (Exception e) {
            result = new OperationResult(format(FORMATTED_FAILURE_MSG, e.getMessage()), FAILED, this, null);
        }
        return result;
    }

    protected String wait(StateDto state, Session session) {
        String phase;
        while (true) {
            phase = getPhase(state, session);
            if ("finished".equals(phase) || "failed".equals(phase)) {
                break;
            }
            try {
                //
                // Wait a quarter of second
                //
                TimeUnit.MILLISECONDS.sleep(250);
            } catch (InterruptedException ignored) {
                break;
            }
        }
        return phase;
    }

    protected String getPhase(StateDto state, Session session) {
        return session.exportService()
                .task(state.getId())
                .state()
                .getEntity()
                .getPhase();
    }

    protected List<ImportParameter> convertImportParameters() {
        List<ImportParameter> parameters = new ArrayList<>();
        if (withIncludeAccessEvents != null) {
            parameters.add(ImportParameter.INCLUDE_ACCESS_EVENTS);
        }
        if (withIncludeAuditEvents != null) {
            parameters.add(ImportParameter.INCLUDE_AUDIT_EVENTS);
        }
        if (withIncludeMonitoringEvents != null) {
            parameters.add(ImportParameter.INCLUDE_MONITORING_EVENTS);
        }
        if (withIncludeServerSettings != null) {
            parameters.add(ImportParameter.INCLUDE_SERVER_SETTINGS);
        }
        if (withUpdate != null) {
            parameters.add(ImportParameter.UPDATE);
        }
        if (withSkipUserUpdate != null) {
            parameters.add(ImportParameter.SKIP_USER_UPDATE);
        }
        return parameters;
    }

    public String getPath() {
        return this.path;
    }

    public String getWithIncludeAuditEvents() {
        return this.withIncludeAuditEvents;
    }

    public String getWithIncludeMonitoringEvents() {
        return this.withIncludeMonitoringEvents;
    }

    public String getWithIncludeAccessEvents() {
        return this.withIncludeAccessEvents;
    }

    public String getWithIncludeServerSettings() {
        return this.withIncludeServerSettings;
    }

    public String getWithSkipUserUpdate() {
        return this.withSkipUserUpdate;
    }

    public String getWithUpdate() {
        return this.withUpdate;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setWithIncludeAuditEvents(String withIncludeAuditEvents) {
        this.withIncludeAuditEvents = withIncludeAuditEvents;
    }

    public void setWithIncludeMonitoringEvents(String withIncludeMonitoringEvents) {
        this.withIncludeMonitoringEvents = withIncludeMonitoringEvents;
    }

    public void setWithIncludeAccessEvents(String withIncludeAccessEvents) {
        this.withIncludeAccessEvents = withIncludeAccessEvents;
    }

    public void setWithIncludeServerSettings(String withIncludeServerSettings) {
        this.withIncludeServerSettings = withIncludeServerSettings;
    }

    public void setWithSkipUserUpdate(String withSkipUserUpdate) {
        this.withSkipUserUpdate = withSkipUserUpdate;
    }

    public void setWithUpdate(String withUpdate) {
        this.withUpdate = withUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImportOperation that = (ImportOperation) o;
        if (!(path != null ? !path.equals(that.path) : that.path != null))
            if (!(withIncludeAuditEvents != null ? !withIncludeAuditEvents.equals(that.withIncludeAuditEvents) : that.withIncludeAuditEvents != null))
                if (!(withIncludeMonitoringEvents != null ? !withIncludeMonitoringEvents.equals(that.withIncludeMonitoringEvents) : that.withIncludeMonitoringEvents != null))
                    if (!(withIncludeAccessEvents != null ? !withIncludeAccessEvents.equals(that.withIncludeAccessEvents) : that.withIncludeAccessEvents != null))
                        if (!(withIncludeServerSettings != null ? !withIncludeServerSettings.equals(that.withIncludeServerSettings) : that.withIncludeServerSettings != null))
                            if (!(withSkipUserUpdate != null ? !withSkipUserUpdate.equals(that.withSkipUserUpdate) : that.withSkipUserUpdate != null))
                                if (!(withUpdate != null ? !withUpdate.equals(that.withUpdate) : that.withUpdate != null))
                                    return true;
        return false;

    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (withIncludeAuditEvents != null ? withIncludeAuditEvents.hashCode() : 0);
        result = 31 * result + (withIncludeMonitoringEvents != null ? withIncludeMonitoringEvents.hashCode() : 0);
        result = 31 * result + (withIncludeAccessEvents != null ? withIncludeAccessEvents.hashCode() : 0);
        result = 31 * result + (withIncludeServerSettings != null ? withIncludeServerSettings.hashCode() : 0);
        result = 31 * result + (withSkipUserUpdate != null ? withSkipUserUpdate.hashCode() : 0);
        result = 31 * result + (withUpdate != null ? withUpdate.hashCode() : 0);
        return result;
    }
}



