/*
 * Copyright 2015, The Querydsl Team (http://www.querydsl.com/team)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.querydsl.codegen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import javax.annotation.Generated;

import com.google.common.base.Function;
import com.mysema.codegen.CodeWriter;
import com.mysema.codegen.model.*;
import com.querydsl.core.util.BeanUtils;

/**
 * {@code BeanSerializer} is a {@link Serializer} implementation which serializes {@link EntityType}
 * instances into JavaBean classes
 *
 * @author tiwe
 *
 */
public class BeanSerializer implements Serializer {

    private static final Function<Property, Parameter> propertyToParameter = new Function<Property, Parameter>() {
        @Override
        public Parameter apply(Property input) {
            return new Parameter(input.getName(), input.getType());
        }
    };

    private final boolean propertyAnnotations;

    private final List<Type> interfaces = new ArrayList<>();

    private final String javadocSuffix;

    private boolean addToString, addFullConstructor;

    private boolean printSupertype = false;

    /**
     * Create a new BeanSerializer
     */
    public BeanSerializer() {
        this(true, " is a Querydsl bean type");
    }

    /**
     * Create a new BeanSerializer with the given javadoc suffix
     *
     * @param javadocSuffix suffix to be used after the simple name in class level javadoc
     */
    public BeanSerializer(String javadocSuffix) {
        this(true, javadocSuffix);
    }

    /**
     * Create a new BeanSerializer
     *
     * @param propertyAnnotations true, to serialize property annotations
     */
    public BeanSerializer(boolean propertyAnnotations) {
        this(propertyAnnotations, " is a Querydsl bean type");
    }

    /**
     * Create a new BeanSerializer
     *
     * @param propertyAnnotations true, to serialize property annotations
     * @param javadocSuffix suffix to be used after the simple name in class level javadoc
     */
    public BeanSerializer(boolean propertyAnnotations, String javadocSuffix) {
        this.propertyAnnotations = propertyAnnotations;
        this.javadocSuffix = javadocSuffix;
    }

    @Override
    public void serialize(EntityType model, SerializerConfig serializerConfig,
            CodeWriter writer) throws IOException {
        String simpleName = model.getSimpleName();

        // package
        if (!model.getPackageName().isEmpty()) {
            writer.packageDecl(model.getPackageName());
        }

        // imports
        Set<String> importedClasses = getAnnotationTypes(model);
        for (Type iface : interfaces) {
            importedClasses.add(iface.getFullName());
        }
        importedClasses.add(Generated.class.getName());
        if (model.hasLists()) {
            importedClasses.add(List.class.getName());
        }
        if (model.hasCollections()) {
            importedClasses.add(Collection.class.getName());
        }
        if (model.hasSets()) {
            importedClasses.add(Set.class.getName());
        }
        if (model.hasMaps()) {
            importedClasses.add(Map.class.getName());
        }
        if (addToString && model.hasArrays()) {
            importedClasses.add(Arrays.class.getName());
        }
        writer.importClasses(importedClasses.toArray(new String[0]));

        // javadoc
        writer.javadoc(simpleName + javadocSuffix);

        // header
        for (Annotation annotation : model.getAnnotations()) {
            writer.annotation(annotation);
        }

        writer.line("@Generated(\"", getClass().getName(), "\")");

        if (!interfaces.isEmpty()) {
            Type superType = null;
            if (printSupertype && model.getSuperType() != null) {
                superType = model.getSuperType().getType();
            }
            Type[] ifaces = interfaces.toArray(new Type[0]);
            writer.beginClass(model, superType, ifaces);
        } else if (printSupertype && model.getSuperType() != null) {
            writer.beginClass(model, model.getSuperType().getType());
        } else {
            writer.beginClass(model);
        }


        bodyStart(model, writer);

        if (addFullConstructor) {
            addFullConstructor(model, writer);
        }

        // fields
        for (Property property : model.getProperties()) {
            if (propertyAnnotations) {
                for (Annotation annotation : property.getAnnotations()) {
                    writer.annotation(annotation);
                }
            }
            writer.privateField(property.getType(), property.getEscapedName());
        }

        // accessors
        for (Property property : model.getProperties()) {
            String propertyName = property.getEscapedName();
            // getter
            writer.beginPublicMethod(property.getType(), "get" + BeanUtils.capitalize(propertyName));
            writer.line("return ", propertyName, ";");
            writer.end();
            // setter
            Parameter parameter = new Parameter(propertyName, property.getType());
            writer.beginPublicMethod(Types.VOID, "set" + BeanUtils.capitalize(propertyName), parameter);
            writer.line("this.", propertyName, " = ", propertyName, ";");
            writer.end();
        }

        if (addToString) {
            addToString(model, writer);
        }

        bodyEnd(model, writer);

        writer.end();
    }

    protected void addFullConstructor(EntityType model, CodeWriter writer) throws IOException {
        // public empty constructor
        writer.beginConstructor();
        writer.end();

        // full constructor
        writer.beginConstructor(model.getProperties(), propertyToParameter);
        for (Property property : model.getProperties()) {
            writer.line("this.", property.getEscapedName(), " = ", property.getEscapedName(), ";");
        }
        writer.end();
    }

    protected void addToString(EntityType model, CodeWriter writer) throws IOException {
        writer.line("@Override");
        writer.beginPublicMethod(Types.STRING, "toString");
        StringBuilder builder = new StringBuilder();
        for (Property property : model.getProperties()) {
            String propertyName = property.getEscapedName();
            if (builder.length() > 0) {
                builder.append(" + \", ");
            } else {
                builder.append("\"");
            }
            builder.append(propertyName).append(" = \" + ");
            if (property.getType().getCategory() == TypeCategory.ARRAY) {
                builder.append("Arrays.toString(").append(propertyName).append(")");
            } else {
                builder.append(propertyName);
            }
        }
        writer.line(" return ", builder.toString(), ";");
        writer.end();
    }

    protected void bodyStart(EntityType model, CodeWriter writer) throws IOException {
        // template method
    }

    protected void bodyEnd(EntityType model, CodeWriter writer) throws IOException {
        // template method
    }

    private Set<String> getAnnotationTypes(EntityType model) {
        Set<String> imports = new HashSet<String>();
        for (Annotation annotation : model.getAnnotations()) {
            imports.add(annotation.annotationType().getName());
        }
        if (propertyAnnotations) {
            for (Property property : model.getProperties()) {
                for (Annotation annotation : property.getAnnotations()) {
                    imports.add(annotation.annotationType().getName());
                }
            }
        }
        return imports;
    }

    public void addInterface(Class<?> iface) {
        interfaces.add(new ClassType(iface));
    }

    public void addInterface(Type type) {
        interfaces.add(type);
    }

    public void setAddToString(boolean addToString) {
        this.addToString = addToString;
    }

    public void setAddFullConstructor(boolean addFullConstructor) {
        this.addFullConstructor = addFullConstructor;
    }

    public void setPrintSupertype(boolean printSupertype) {
        this.printSupertype = printSupertype;
    }

}
