package com.jaspersoft.cli.tool.core;

import com.jaspersoft.cli.tool.api.operation.ClientOperation;
import com.jaspersoft.jasperserver.dto.resources.ClientResource;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.importservice.ImportParameter;
import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.importexport.importservice.ImportTaskRequestAdapter;
import com.jaspersoft.jasperserver.jaxrs.client.core.Session;
import com.jaspersoft.jasperserver.jaxrs.client.dto.importexport.StateDto;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import static java.lang.Thread.sleep;

/**
 * @author Alexander Krasnyanskiy
 */
public class ClientSimpleOperation implements ClientOperation {

    protected Session session;

    public ClientSimpleOperation() {
    }

    @Override
    public ClientResource getResource(String uri) {
        return session.resourcesService()
                .resource(uri)
                .details()
                .entity();
    }

    @Override
    public void importResource(InputStream resource) {
        StateDto state = session.importService().newTask().create(resource).entity();
        try {
            waitForUpload(state);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void importResource(InputStream resource, Map<ImportParameter, Boolean> parameters) {
        ImportTaskRequestAdapter task = session.importService().newTask();
        for (ImportParameter importParameter : parameters.keySet()) {
            task.parameter(importParameter, parameters.get(importParameter));
        }
        StateDto state = task.create(resource).entity();
        try {
            waitForUpload(state);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void importResource(File resource) {
        StateDto state = session.importService().newTask().create(resource).entity();
        try {
            waitForUpload(state);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitForUpload(StateDto state) throws InterruptedException {
        String currentPhase = "undefined";
        do {
            currentPhase = getPhaseForState(state);
            if (currentPhase.equals("finished")) {
                break;
            } else {
                sleep(500);
            }
        } while (true);
    }

    private String getPhaseForState(StateDto state) {
        if (state != null) {
            return session.exportService().task(state.getId()).state().entity().getPhase();
        }
        throw new RuntimeException("state is null");
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
