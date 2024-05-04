/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.generator;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static is.codion.common.Separators.LINE_SEPARATOR;
import static is.codion.common.Text.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;
import static javax.lang.model.element.Modifier.*;

final class DomainToString {

	private static final String INDENT = "\t";
	private static final String DOUBLE_INDENT = INDENT + INDENT;
	private static final String TRIPLE_INDENT = DOUBLE_INDENT + INDENT;

	private DomainToString() {}

	static String toString(List<DefinitionRow> rows, String packageName) {
		return rows.stream()
						.collect(groupingBy(DefinitionRow::domain, LinkedHashMap::new, toList()))
						.entrySet().stream()
						.map(entry -> toString(entry.getKey(), entry.getValue(), packageName))
						.collect(joining(LINE_SEPARATOR + LINE_SEPARATOR));
	}

	static String toString(Domain domain, Collection<DefinitionRow> definitions, String packageName) {
		String className = interfaceName(domain.type().name(), true);
		TypeSpec.Builder classBuilder = classBuilder(className)
						.addModifiers(PUBLIC, FINAL)
						.superclass(DomainModel.class)
						.addField(FieldSpec.builder(DomainType.class, "DOMAIN")
										.initializer("domainType($L)", className + ".class")
										.addModifiers(PUBLIC, STATIC, FINAL)
										.build());

		List<String> definitionMethodNames = new ArrayList<>();
		definitions.forEach(definitionRow -> {
			classBuilder.addType(createInterface(definitionRow.definition));
			MethodSpec definitionMethod = createDefinitionMethod(definitionRow.definition);
			classBuilder.addMethod(definitionMethod);
			definitionMethodNames.add(definitionMethod.name);
		});

		StringBuilder addMethods = new StringBuilder();
		for (int i = 0; i < definitionMethodNames.size(); i++) {
			addMethods.append(definitionMethodNames.get(i)).append("()");
			if (i < definitionMethodNames.size() - 1) {
				if ((i + 1) % 3 != 0) { // three per line
					addMethods.append(", ");
				}
				else if (i > 0) {
					addMethods.append(",\n");
				}
			}
		}

		return JavaFile.builder(packageName, classBuilder.addMethod(constructorBuilder()
														.addModifiers(PUBLIC)
														.addStatement("super(DOMAIN)")
														.addStatement(new StringBuilder()
																		.append("add(")
																		.append(addMethods)
																		.append(")").toString())
														.build())
										.build())
						.addStaticImport(DomainType.class, "domainType")
						.addStaticImport(KeyGenerator.class, "identity")
						.skipJavaLangImports(true)
						.indent(INDENT)
						.build().toString();
	}

	private static TypeSpec createInterface(EntityDefinition definition) {
		String interfaceName = interfaceName(definition.tableName(), true);
		TypeSpec.Builder interfaceBuilder = interfaceBuilder(interfaceName)
						.addField(FieldSpec.builder(EntityType.class, "TYPE")
										.addModifiers(PUBLIC, STATIC, FINAL)
										.initializer("DOMAIN.entityType($S)", definition.tableName().toLowerCase())
										.build())
						.addModifiers(PUBLIC);
		List<AttributeDefinition<?>> columnDefinitions = definition.attributes().definitions().stream()
						.filter(ColumnDefinition.class::isInstance)
						.collect(toList());
		columnDefinitions.forEach(columnDefinition -> appendAttribute(interfaceBuilder, columnDefinition));
		List<AttributeDefinition<?>> foreignKeyDefinitions = definition.attributes().definitions().stream()
						.filter(ForeignKeyDefinition.class::isInstance)
						.collect(toList());
		if (!foreignKeyDefinitions.isEmpty()) {
			foreignKeyDefinitions.forEach(foreignKeyDefinition -> appendAttribute(interfaceBuilder, foreignKeyDefinition));
		}

		return interfaceBuilder.build();
	}

	private static MethodSpec createDefinitionMethod(EntityDefinition definition) {
		String interfaceName = interfaceName(definition.tableName(), true);
		StringBuilder builder = new StringBuilder()
						.append("return ").append(interfaceName).append(".TYPE.define(").append(LINE_SEPARATOR)
						.append(String.join("," + LINE_SEPARATOR,
										attributeStrings(definition.attributes().definitions(), interfaceName, definition)))
						.append(")");
		if (definition.primaryKey().generated()) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".keyGenerator(identity())");
		}
		if (!nullOrEmpty(definition.caption())) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".caption(\"").append(definition.caption()).append("\")");
		}
		if (!nullOrEmpty(definition.description())) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".description(\"").append(definition.description()).append("\")");
		}
		if (definition.readOnly()) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".readOnly(true)");
		}
		builder.append(LINE_SEPARATOR).append(INDENT).append(".build();");

		return methodBuilder(interfaceName(definition.tableName(), false))
						.addModifiers(STATIC)
						.returns(EntityDefinition.class)
						.addCode(builder.toString())
						.build();
	}

	private static void appendAttribute(TypeSpec.Builder interfaceBuilder,
																			AttributeDefinition<?> attributeDefinition) {
		String valueClassName = attributeDefinition.attribute().type().valueClass().getSimpleName();
		if (attributeDefinition instanceof ColumnDefinition) {
			ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
			FieldSpec.Builder fieldBuilder = FieldSpec.builder(ParameterizedTypeName.get(Column.class,
															columnDefinition.attribute().type().valueClass()),
											columnDefinition.name().toUpperCase())
							.addModifiers(PUBLIC, STATIC, FINAL);
			if ("Object".equals(valueClassName)) {
				//special handling for mapping unknown column data types to Object columns
				fieldBuilder.initializer("TYPE.column($S, $L)", columnDefinition.name().toLowerCase(), "Object.class");
			}
			else {
				fieldBuilder.initializer("TYPE.$LColumn($S)",
								attributeTypePrefix(valueClassName), columnDefinition.name().toLowerCase());
			}
			interfaceBuilder.addField(fieldBuilder.build());
		}
		else if (attributeDefinition instanceof ForeignKeyDefinition) {
			ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
			String references = foreignKeyDefinition.references().stream()
							.map(reference -> new StringBuilder()
											.append(reference.column().name().toUpperCase()).append(", ")
											.append(interfaceName(reference.foreign().entityType().name(), true))
											.append(".").append(reference.foreign().name().toUpperCase()).toString())
							.collect(joining(", "));
			//todo wrap references if more than four
			interfaceBuilder.addField(FieldSpec.builder(ForeignKey.class, attributeDefinition.attribute().name().toUpperCase())
							.addModifiers(PUBLIC, STATIC, FINAL)
							.initializer("TYPE.foreignKey($S, $L)",
											attributeDefinition.attribute().name().toLowerCase(), references)
							.build());
		}
	}

	private static List<String> attributeStrings(Collection<AttributeDefinition<?>> attributeDefinitions, String interfaceName,
																							 EntityDefinition definition) {
		List<String> strings = new ArrayList<>();
		attributeDefinitions.forEach(attributeDefinition -> {
			if (attributeDefinition instanceof ColumnDefinition) {
				ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
				strings.add(columnDefinition(interfaceName, columnDefinition,
								definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()), definition.primaryKey().columns().size() > 1));
			}
			else if (attributeDefinition instanceof ForeignKeyDefinition) {
				strings.add(foreignKeyDefinition(interfaceName, (ForeignKeyDefinition) attributeDefinition));
			}
		});

		return strings;
	}

	private static String foreignKeyDefinition(String interfaceName, ForeignKeyDefinition definition) {
		StringBuilder builder = new StringBuilder();
		String foreignKey = definition.attribute().name().toUpperCase();
		builder.append(DOUBLE_INDENT).append(interfaceName).append(".").append(foreignKey).append(".define()")
						.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
						.append(".foreignKey()")
						.append(LINE_SEPARATOR)
						.append(TRIPLE_INDENT).append(".caption(\"").append(definition.caption()).append("\")");

		return builder.toString();
	}

	private static String columnDefinition(String interfaceName, ColumnDefinition<?> column,
																				 boolean isForeignKey, boolean compositePrimaryKey) {
		StringBuilder builder = new StringBuilder(DOUBLE_INDENT)
						.append(interfaceName).append(".").append(column.name().toUpperCase()).append(".define()")
						.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
						.append(".").append(definitionType(column, compositePrimaryKey));
		if (!isForeignKey && !column.primaryKey()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".caption(").append("\"").append(column.caption()).append("\")");
		}
		if (column.columnHasDefaultValue()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".columnHasDefaultValue(true)");
		}
		if (!column.nullable() && !column.primaryKey()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".nullable(false)");
		}
		if (column.lazy()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".lazy(true)");
		}
		if (column.attribute().type().isString() && column.maximumLength() != -1) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".maximumLength(")
							.append(column.maximumLength()).append(")");
		}
		if (column.attribute().type().isDecimal() && column.maximumFractionDigits() >= 1) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".maximumFractionDigits(")
							.append(column.maximumFractionDigits()).append(")");
		}
		if (!nullOrEmpty(column.description())) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".description(")
							.append("\"").append(column.description()).append("\")");
		}

		return builder.toString();
	}

	private static String attributeTypePrefix(String valueClassName) {
		if ("byte[]".equals(valueClassName)) {
			return "byteArray";
		}

		return valueClassName.substring(0, 1).toLowerCase() + valueClassName.substring(1);
	}

	private static String definitionType(ColumnDefinition<?> column, boolean compositePrimaryKey) {
		if (column.primaryKey()) {
			return compositePrimaryKey ? "primaryKey(" + column.primaryKeyIndex() + ")" : "primaryKey()";
		}

		return "column()";
	}

	private static String interfaceName(String tableName, boolean uppercase) {
		String name = tableName.toLowerCase();
		if (name.contains(".")) {
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		name = underscoreToCamelCase(name);
		if (uppercase) {
			name = name.substring(0, 1).toUpperCase() + name.substring(1);
		}

		return name;
	}

	static String underscoreToCamelCase(String text) {
		if (!requireNonNull(text, "text").contains("_")) {
			return text;
		}
		StringBuilder builder = new StringBuilder();
		boolean firstDone = false;
		List<String> strings = Arrays.stream(text.toLowerCase().split("_"))
						.filter(string -> !string.isEmpty())
						.collect(toList());
		if (strings.size() == 1) {
			return strings.get(0);
		}
		for (String split : strings) {
			if (!firstDone) {
				builder.append(Character.toLowerCase(split.charAt(0)));
				firstDone = true;
			}
			else {
				builder.append(Character.toUpperCase(split.charAt(0)));
			}
			if (split.length() > 1) {
				builder.append(split.substring(1).toLowerCase());
			}
		}

		return builder.toString();
	}
}
