package com.jaspersoft.jasperserver.jrsh.core.operation;

import com.jaspersoft.jasperserver.jrsh.core.operation.annotation.Master;
import com.jaspersoft.jasperserver.jrsh.core.operation.parser.exception.OperationNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.FilterBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Alexander Krasnyanskiy
 */
//@Log4j
@SuppressWarnings("unchecked")
public class OperationFactory {

    private static final Map<String, Class<? extends Operation>> AVAILABLE_OPERATIONS;

    static {
        AVAILABLE_OPERATIONS = new HashMap<>();
        Set<Class<? extends Operation>> types = getOperationTypes();
        for (Class<? extends Operation> operationType : types) {
            Master annotation = operationType.getAnnotation(Master.class);
            if (annotation != null) {
                String operationName = annotation.name();
                AVAILABLE_OPERATIONS.put(operationName, operationType);
            }
        }
    }

    public static Operation createOperationByName(String operationName) {
        Class<? extends Operation> operationType = AVAILABLE_OPERATIONS.get(operationName);
        if (operationType == null) {
            throw new OperationNotFoundException();
        }
        return createInstance(operationType);
    }

    public static Set<Operation> createOperationsByAvailableTypes() {
        Set<Operation> set = new HashSet<>();
        for (Class<? extends Operation> type : AVAILABLE_OPERATIONS.values()) {
            set.add(createInstance(type));
        }
        return set;
    }

    protected static Operation createInstance(Class<? extends Operation> operationType) {
        Operation instance;
        try {
            instance = operationType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CouldNotCreateOperationInstance();
        }
        return instance;
    }

    protected static Set<Class<? extends Operation>> getOperationTypes() {
        Set<Class<? extends Operation>> operationTypes = new HashSet<>();
        //
        // Read YAML config and get package info
        //
        Map<String, Object> config = getConfig();
        List<String> packages = (List<String>) config.get("packages-to-scan");
        List<String> classes = (List<String>) config.get("classes");
        //
        // Use default package to limit the search area and thus
        // to speed up scanning ClassPath via Reflection
        //
        FilterBuilder filter = new FilterBuilder().includePackage("com.jaspersoft.jasperserver.jrsh.core.operation.impl");
        //
        // Add specific package names to filter
        //
        if (packages != null) {
            for (String aPackage : packages) {
                aPackage = StringUtils.chomp(aPackage, ".*");
                filter.includePackage(aPackage);
            }
        }

        if (classes != null) {
            for (String aClass : classes) {
                try {
                    Class clz = Class.forName(aClass);
                    if (!Modifier.isAbstract(clz.getModifiers())
                            && Operation.class.isAssignableFrom(clz)) {
                        operationTypes.add(clz);
                    }
                } catch (ClassNotFoundException ignored) {
                    // NOP
                }
            }
        }
        //
        // Scan CP and retrieve operation classes
        //
        Reflections ref = new Reflections(new SubTypesScanner(), filter);
        for (Class<? extends Operation> subType : ref.getSubTypesOf(Operation.class)) {
            if (!Modifier.isAbstract(subType.getModifiers())) {
                operationTypes.add(subType);
            }
        }
        return operationTypes;
    }

    protected static Map<String, Object> getConfig() {
        Yaml yaml = new Yaml();
        //
        // To avoid NPE we may return an empty config
        //
        Map<String, Object> config = new HashMap<>();
        try {
            try (InputStream file = OperationFactory.class.getClassLoader()
                    .getResourceAsStream("config.yml")) {
                config.putAll((Map<String, Object>) yaml.load(file));
            }
        } catch (IOException ignored) {
            // NOP
        }
        return config;
    }

}
