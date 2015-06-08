package com.jaspersoft.jasperserver.jrsh.core.operation.impl;

import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.exportservice.ExportParameter;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.exportservice.ExportService;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.exportservice.ExportTaskAdapter;
import com.jaspersoft.jasperserver.jaxrs.client.core.Session;
import com.jaspersoft.jasperserver.jaxrs.client.dto.importexport.StateDto;
import com.jaspersoft.jasperserver.jrsh.core.operation.Operation;
import com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Master;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Parameter;
import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Value;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl.FileNameToken;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl.RepositoryToken;
import com.jaspersoft.jasperserver.jrsh.core.operation.grammar.token.impl.StringToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult.ResultCode.FAILED;
import static com.jaspersoft.jasperserver.jrsh.core.operation.OperationResult.ResultCode.SUCCESS;
import static java.lang.String.format;

/**
 * @author Alexander Krasnyanskiy
 */
@Master(name = "export",
        usage = "export [context] [parameters]",
        description = "Operation <export> is used to download JRS resources")
public class ExportOperation implements Operation {

    public static final String FORMATTED_OK_MSG = "Export status: Success (File has been created: %s)";
    public static final String FAILURE_MSG = "Export failed";
    public static final String FORMATTED_FAILURE_MSG = "Export failed (%s)";

    @Parameter(mandatory = true, dependsOn = "export", values = {
            @Value(tokenAlias = "RE", tokenClass = StringToken.class, tokenValue = "repository")
    })
    private String context;

    @Parameter(mandatory = true, dependsOn = "export", ruleGroups = "BRANCH", values =
    @Value(tokenAlias = "OL", tokenClass = StringToken.class, tokenValue = "all", tail = true))
    private String all;

    @Parameter(mandatory = true, dependsOn = "RE", values =
    @Value(tokenAlias = "RP", tokenClass = RepositoryToken.class, tail = true))
    private String repositoryPath;

    @Parameter(dependsOn = "RP", values =
    @Value(tokenAlias = "->", tokenClass = StringToken.class, tokenValue = "to"))
    private String to;

    @Parameter(dependsOn = "->", values =
    @Value(tokenAlias = "F", tokenClass = FileNameToken.class, tail = true))
    private String fileUri;

    @Parameter(dependsOn = {"F", "RP", "IUR", "IME", "RPP", "IAE"}, values =
    @Value(tokenAlias = "UR", tokenClass = StringToken.class,
            tokenValue = "with-user-roles", tail = true))
    private String withUserRoles;

    @Parameter(dependsOn = {"F", "RP", "UR", "IME", "RPP", "IAE"}, values =
    @Value(tokenAlias = "IUR", tokenClass = StringToken.class,
            tokenValue = "with-include-audit-events", tail = true))
    private String withIncludeAuditEvents;

    @Parameter(dependsOn = {"F", "RP", "UR", "IUR", "RPP", "IAE"}, values =
    @Value(tokenAlias = "IME", tokenClass = StringToken.class,
            tokenValue = "with-include-monitoring-events", tail = true))
    private String withIncludeMonitoringEvents;

    @Parameter(dependsOn = {"F", "RP", "UR", "IUR", "IME", "IAE"}, values =
    @Value(tokenAlias = "RPP", tokenClass = StringToken.class,
            tokenValue = "with-repository-permissions", tail = true))
    private String withRepositoryPermissions;

    @Parameter(dependsOn = {"F", "RP", "UR", "IUR", "RPP", "IME"}, values =
    @Value(tokenAlias = "IAE", tokenClass = StringToken.class,
            tokenValue = "with-include-access-events", tail = true))
    private String withIncludeAccessEvents;

    public ExportOperation() {
    }

    @Override
    public OperationResult eval(Session session) {
        //
        // Perform export logic
        //
        OperationResult result;
        try {
            ExportService exportService = session.exportService();
            ExportTaskAdapter task = exportService.newTask();
            //
            // Export specific repository
            //
            if ("repository".equals(context)) {
                if (repositoryPath != null) {
                    task.uri(repositoryPath);
                }

                StateDto state = task
                        .parameters(convertExportParameters())
                        .create()
                        .getEntity();

                InputStream entity = session.exportService()
                        .task(state.getId())
                        .fetch()
                        .getEntity();

                if (to != null) {
                    if (fileUri != null) {
                        File target = new File(fileUri);
                        FileUtils.copyInputStreamToFile(entity, target);
                    }
                } else {
                    File target = new File("export.zip");
                    FileUtils.copyInputStreamToFile(entity, target);
                    //
                    // Save path for operation result message
                    //
                    fileUri = target.getAbsolutePath();
                }
            }
            //
            // Export everything
            //
            // todo: fix me!
            if (all != null && !all.isEmpty()) {
                StateDto state = task
                        .parameter(ExportParameter.EVERYTHING)
                        .create()
                        .getEntity();

                InputStream entity = session.exportService()
                        .task(state.getId())
                        .fetch()
                        .getEntity();

                File target = new File("export.zip");
                FileUtils.copyInputStreamToFile(entity, target);
                fileUri = target.getAbsolutePath();
            }
            result = new OperationResult(format(FORMATTED_OK_MSG, fileUri), SUCCESS, this, null);
        } catch (Exception err) {
            result = new OperationResult(FAILURE_MSG, FAILED, this, null);
        }

        return result;
    }

    protected List<ExportParameter> convertExportParameters() {
        List<ExportParameter> parameters = new ArrayList<>();
        if (withIncludeAccessEvents != null) {
            parameters.add(ExportParameter.INCLUDE_ACCESS_EVENTS);
        }
        if (withIncludeAuditEvents != null) {
            parameters.add(ExportParameter.INCLUDE_AUDIT_EVENTS);
        }
        if (withRepositoryPermissions != null) {
            parameters.add(ExportParameter.REPOSITORY_PERMISSIONS);
        }
        if (withUserRoles != null) {
            parameters.add(ExportParameter.ROLE_USERS);
        }
        if (withIncludeMonitoringEvents != null) {
            parameters.add(ExportParameter.INCLUDE_MONITORING_EVENTS);
        }
        return parameters;
    }

    public String getContext() {
        return this.context;
    }

    public String getAll() {
        return this.all;
    }

    public String getRepositoryPath() {
        return this.repositoryPath;
    }

    public String getTo() {
        return this.to;
    }

    public String getFileUri() {
        return this.fileUri;
    }

    public String getWithUserRoles() {
        return this.withUserRoles;
    }

    public String getWithIncludeAuditEvents() {
        return this.withIncludeAuditEvents;
    }

    public String getWithIncludeMonitoringEvents() {
        return this.withIncludeMonitoringEvents;
    }

    public String getWithRepositoryPermissions() {
        return this.withRepositoryPermissions;
    }

    public String getWithIncludeAccessEvents() {
        return this.withIncludeAccessEvents;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setAll(String all) {
        this.all = all;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public void setWithUserRoles(String withUserRoles) {
        this.withUserRoles = withUserRoles;
    }

    public void setWithIncludeAuditEvents(String withIncludeAuditEvents) {
        this.withIncludeAuditEvents = withIncludeAuditEvents;
    }

    public void setWithIncludeMonitoringEvents(String withIncludeMonitoringEvents) {
        this.withIncludeMonitoringEvents = withIncludeMonitoringEvents;
    }

    public void setWithRepositoryPermissions(String withRepositoryPermissions) {
        this.withRepositoryPermissions = withRepositoryPermissions;
    }

    public void setWithIncludeAccessEvents(String withIncludeAccessEvents) {
        this.withIncludeAccessEvents = withIncludeAccessEvents;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ExportOperation)) return false;
        final ExportOperation other = (ExportOperation) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$context = this.context;
        final Object other$context = other.context;
        if (this$context == null ? other$context != null : !this$context.equals(other$context)) return false;
        final Object this$all = this.all;
        final Object other$all = other.all;
        if (this$all == null ? other$all != null : !this$all.equals(other$all)) return false;
        final Object this$repositoryPath = this.repositoryPath;
        final Object other$repositoryPath = other.repositoryPath;
        if (this$repositoryPath == null ? other$repositoryPath != null : !this$repositoryPath.equals(other$repositoryPath))
            return false;
        final Object this$to = this.to;
        final Object other$to = other.to;
        if (this$to == null ? other$to != null : !this$to.equals(other$to)) return false;
        final Object this$fileUri = this.fileUri;
        final Object other$fileUri = other.fileUri;
        if (this$fileUri == null ? other$fileUri != null : !this$fileUri.equals(other$fileUri)) return false;
        final Object this$withUserRoles = this.withUserRoles;
        final Object other$withUserRoles = other.withUserRoles;
        if (this$withUserRoles == null ? other$withUserRoles != null : !this$withUserRoles.equals(other$withUserRoles))
            return false;
        final Object this$withIncludeAuditEvents = this.withIncludeAuditEvents;
        final Object other$withIncludeAuditEvents = other.withIncludeAuditEvents;
        if (this$withIncludeAuditEvents == null ? other$withIncludeAuditEvents != null : !this$withIncludeAuditEvents.equals(other$withIncludeAuditEvents))
            return false;
        final Object this$withIncludeMonitoringEvents = this.withIncludeMonitoringEvents;
        final Object other$withIncludeMonitoringEvents = other.withIncludeMonitoringEvents;
        if (this$withIncludeMonitoringEvents == null ? other$withIncludeMonitoringEvents != null : !this$withIncludeMonitoringEvents.equals(other$withIncludeMonitoringEvents))
            return false;
        final Object this$withRepositoryPermissions = this.withRepositoryPermissions;
        final Object other$withRepositoryPermissions = other.withRepositoryPermissions;
        if (this$withRepositoryPermissions == null ? other$withRepositoryPermissions != null : !this$withRepositoryPermissions.equals(other$withRepositoryPermissions))
            return false;
        final Object this$withIncludeAccessEvents = this.withIncludeAccessEvents;
        final Object other$withIncludeAccessEvents = other.withIncludeAccessEvents;
        if (this$withIncludeAccessEvents == null ? other$withIncludeAccessEvents != null : !this$withIncludeAccessEvents.equals(other$withIncludeAccessEvents))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $context = this.context;
        result = result * PRIME + ($context == null ? 0 : $context.hashCode());
        final Object $all = this.all;
        result = result * PRIME + ($all == null ? 0 : $all.hashCode());
        final Object $repositoryPath = this.repositoryPath;
        result = result * PRIME + ($repositoryPath == null ? 0 : $repositoryPath.hashCode());
        final Object $to = this.to;
        result = result * PRIME + ($to == null ? 0 : $to.hashCode());
        final Object $fileUri = this.fileUri;
        result = result * PRIME + ($fileUri == null ? 0 : $fileUri.hashCode());
        final Object $withUserRoles = this.withUserRoles;
        result = result * PRIME + ($withUserRoles == null ? 0 : $withUserRoles.hashCode());
        final Object $withIncludeAuditEvents = this.withIncludeAuditEvents;
        result = result * PRIME + ($withIncludeAuditEvents == null ? 0 : $withIncludeAuditEvents.hashCode());
        final Object $withIncludeMonitoringEvents = this.withIncludeMonitoringEvents;
        result = result * PRIME + ($withIncludeMonitoringEvents == null ? 0 : $withIncludeMonitoringEvents.hashCode());
        final Object $withRepositoryPermissions = this.withRepositoryPermissions;
        result = result * PRIME + ($withRepositoryPermissions == null ? 0 : $withRepositoryPermissions.hashCode());
        final Object $withIncludeAccessEvents = this.withIncludeAccessEvents;
        result = result * PRIME + ($withIncludeAccessEvents == null ? 0 : $withIncludeAccessEvents.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ExportOperation;
    }

    public String toString() {
        return "com.jaspersoft.jasperserver.jrsh.core.operation.impl.ExportOperation(context=" + this.context + ", all=" + this.all + ", repositoryPath=" + this.repositoryPath + ", to=" + this.to + ", fileUri=" + this.fileUri + ", withUserRoles=" + this.withUserRoles + ", withIncludeAuditEvents=" + this.withIncludeAuditEvents + ", withIncludeMonitoringEvents=" + this.withIncludeMonitoringEvents + ", withRepositoryPermissions=" + this.withRepositoryPermissions + ", withIncludeAccessEvents=" + this.withIncludeAccessEvents + ")";
    }
}
