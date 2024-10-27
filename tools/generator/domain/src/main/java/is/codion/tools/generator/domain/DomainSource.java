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
package is.codion.tools.generator.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.AuditColumnDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static javax.lang.model.element.Modifier.*;

/**
 * For instances use the factory method {@link #domainSource(Domain)}.
 */
public final class DomainSource {

	private static final String INDENT = "\t";
	private static final String DOUBLE_INDENT = INDENT + INDENT;
	private static final String TRIPLE_INDENT = DOUBLE_INDENT + INDENT;
	private static final String DOMAIN = "DOMAIN";
	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final Domain domain;
	private final String domainInterfaceName;
	private final List<EntityDefinition> sortedDefinitions;

	private DomainSource(Domain domain) {
		this.domain = requireNonNull(domain);
		this.domainInterfaceName = interfaceName(requireNonNull(domain).type().name(), true);
		this.sortedDefinitions = sortDefinitions(domain);
	}

	/**
	 * @param domainPackage the domain package.
	 * @return the api source code.
	 */
	public String api(String domainPackage) {
		return toApiString(requireNonNull(domainPackage) + ".api");
	}

	/**
	 * @param domainPackage the domain package.
	 * @return the implementation source code.
	 */
	public String implementation(String domainPackage) {
		return toImplementationString(requireNonNull(domainPackage));
	}

	/**
	 * @param domainPackage the domain package.
	 * @return the combined source code of the api and implementation.
	 */
	public String combined(String domainPackage) {
		return toCombinedString(requireNonNull(domainPackage));
	}

	/**
	 * Writes the api and implementation source code to the given path.
	 * @param domainPackage the domain package.
	 * @param path the path to write the source code to.
	 * @throws IOException in case of an I/O error.
	 */
	public void writeApiImpl(String domainPackage, Path path) throws IOException {
		String interfaceName = interfaceName(domain.type().name(), true);
		Files.createDirectories(requireNonNull(path));
		Path filePath = path.resolve(interfaceName + ".java");
		Files.write(filePath, singleton(api(requireNonNull(domainPackage) + ".api")));
		path = path.resolve("impl");
		Files.createDirectories(path);
		filePath = path.resolve(interfaceName + "Impl.java");
		Files.write(filePath, singleton(implementation(domainPackage)));
	}

	/**
	 * Writes the combined source code to the given path.
	 * @param domainPackage the domain package.
	 * @param path the path to write the source code to.
	 * @throws IOException in case of an I/O error.
	 */
	public void writeCombined(String domainPackage, Path path) throws IOException {
		String interfaceName = interfaceName(domain.type().name(), true);
		Files.createDirectories(requireNonNull(path));
		Files.write(path.resolve(interfaceName + ".java"), singleton(combined(requireNonNull(domainPackage))));
	}

	/**
	 * Instantiates a new {@link DomainSource} instance.
	 * @param domain the domain model for which to generate the source code.
	 * @return a new {@link DomainSource} instance.
	 */
	public static DomainSource domainSource(Domain domain) {
		return new DomainSource(domain);
	}

	private String toApiString(String sourcePackage) {
		TypeSpec.Builder classBuilder = interfaceBuilder(domainInterfaceName)
						.addModifiers(PUBLIC)
						.addField(FieldSpec.builder(DomainType.class, DOMAIN)
										.addModifiers(PUBLIC, STATIC, FINAL)
										.initializer("domainType($L)", domainInterfaceName + ".class")
										.build());

		sortedDefinitions.forEach(definition -> classBuilder.addType(createInterface(definition)));

		return removeInterfaceLineBreaks(JavaFile.builder(sourcePackage.isEmpty() ? "" : sourcePackage, classBuilder.build())
						.addStaticImport(DomainType.class, "domainType")
						.skipJavaLangImports(true)
						.indent(INDENT)
						.build()
						.toString());
	}

	private String toImplementationString(String sourcePackage) {
		TypeSpec.Builder classBuilder = classBuilder(domainInterfaceName + "Impl")
						.addModifiers(PUBLIC, FINAL)
						.superclass(DomainModel.class);

		Map<EntityDefinition, String> definitionMethods = addDefinitionMethods(classBuilder);

		String implementationPackage = sourcePackage.isEmpty() ? "" : sourcePackage;

		JavaFile.Builder fileBuilder = JavaFile.builder(implementationPackage,
										classBuilder.addMethod(createDomainConstructor(definitionMethods))
														.build())
						.skipJavaLangImports(true)
						.indent(INDENT);
		if (identityKeyGeneratorUsed()) {
			fileBuilder.addStaticImport(KeyGenerator.class, "identity");
		}
		if (!implementationPackage.isEmpty()) {
			fileBuilder.addStaticImport(ClassName.bestGuess(sourcePackage + ".api." + domainInterfaceName), DOMAIN);
		}

		String sourceString = fileBuilder.build().toString();
		if (!sourcePackage.isEmpty()) {
			sourceString = addInterfaceImports(sourceString, sourcePackage + "." + domainInterfaceName);
		}

		return sourceString;
	}

	private String toCombinedString(String sourcePackage) {
		TypeSpec.Builder classBuilder = classBuilder(domainInterfaceName)
						.addModifiers(PUBLIC, FINAL)
						.addField(FieldSpec.builder(DomainType.class, DOMAIN)
										.addModifiers(PUBLIC, STATIC, FINAL)
										.initializer("domainType($L)", domainInterfaceName + ".class")
										.build())
						.superclass(DomainModel.class);

		Map<EntityDefinition, String> definitionMethods = addDefinitionMethods(classBuilder);
		sortedDefinitions.forEach(definition -> classBuilder.addType(createInterface(definition)));

		JavaFile.Builder fileBuilder = JavaFile.builder(sourcePackage,
										classBuilder.addMethod(createDomainConstructor(definitionMethods))
														.build())
						.addStaticImport(DomainType.class, "domainType")
						.skipJavaLangImports(true)
						.indent(INDENT);
		if (identityKeyGeneratorUsed()) {
			fileBuilder.addStaticImport(KeyGenerator.class, "identity");
		}
		if (!sourcePackage.isEmpty()) {
			fileBuilder.addStaticImport(ClassName.bestGuess(sourcePackage + "." + domainInterfaceName), DOMAIN);
		}

		String sourceString = fileBuilder.build().toString();
		if (!sourcePackage.isEmpty()) {
			sourceString = addInterfaceImports(sourceString, sourcePackage + "." + domainInterfaceName);
		}

		return removeInterfaceLineBreaks(sourceString);
	}

	private static MethodSpec createDomainConstructor(Map<EntityDefinition, String> definitionMethods) {
		MethodSpec.Builder constructorBuilder = constructorBuilder()
						.addModifiers(PUBLIC)
						.addStatement("super(DOMAIN)");
		StringBuilder addParameters;
		if (cyclicalDependencies(definitionMethods.keySet())) {
			constructorBuilder.addStatement("validateForeignKeys(false)");
		}
		addParameters = createAddParameters(new ArrayList<>(definitionMethods.values()));

		return constructorBuilder
						.addStatement(new StringBuilder()
										.append("add(").append(addParameters).append(")")
										.toString())
						.build();
	}

	private Map<EntityDefinition, String> addDefinitionMethods(TypeSpec.Builder classBuilder) {
		Map<EntityDefinition, String> definitionMethods = new LinkedHashMap<>();
		sortedDefinitions.forEach(definition ->
						addDefinition(definition, classBuilder, definitionMethods::put));

		return definitionMethods;
	}

	private static void addDefinition(EntityDefinition definition,
																		TypeSpec.Builder classBuilder,
																		BiConsumer<EntityDefinition, String> onMethod) {
		MethodSpec definitionMethod = createDefinitionMethod(definition);
		classBuilder.addMethod(definitionMethod);
		onMethod.accept(definition, definitionMethod.name);
	}

	private static TypeSpec createInterface(EntityDefinition definition) {
		String interfaceName = interfaceName(definition.tableName(), true);
		TypeSpec.Builder interfaceBuilder = interfaceBuilder(interfaceName)
						.addModifiers(PUBLIC, STATIC)
						.addField(FieldSpec.builder(EntityType.class, "TYPE")
										.addModifiers(PUBLIC, STATIC, FINAL)
										.initializer("DOMAIN.entityType($S)", definition.tableName().toLowerCase())
										.build())
						.addModifiers(PUBLIC);
		definition.attributes().definitions().stream()
						.filter(ColumnDefinition.class::isInstance)
						.forEach(columnDefinition -> appendAttribute(interfaceBuilder, columnDefinition));
		definition.attributes().definitions().stream()
						.filter(ForeignKeyDefinition.class::isInstance)
						.forEach(foreignKeyDefinition -> appendAttribute(interfaceBuilder, foreignKeyDefinition));

		return interfaceBuilder.build();
	}

	private static MethodSpec createDefinitionMethod(EntityDefinition definition) {
		String interfaceName = interfaceName(definition.tableName(), true);
		StringBuilder builder = new StringBuilder()
						.append("return ").append(interfaceName).append(".TYPE.define(").append(LINE_SEPARATOR)
						.append(String.join("," + LINE_SEPARATOR,
										createAttributes(definition.attributes().definitions(), definition, interfaceName)))
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

	private static StringBuilder createAddParameters(List<String> definitionMethodNames) {
		StringBuilder definitionMethods = new StringBuilder();
		for (int i = 0; i < definitionMethodNames.size(); i++) {
			definitionMethods.append(definitionMethodNames.get(i)).append("()");
			if (i < definitionMethodNames.size() - 1) {
				if ((i + 1) % 3 != 0) { // three per line
					definitionMethods.append(", ");
				}
				else if (i > 0) {
					definitionMethods.append(",\n");
				}
			}
		}
		return definitionMethods;
	}

	private static List<String> createAttributes(Collection<AttributeDefinition<?>> attributeDefinitions,
																							 EntityDefinition definition, String interfaceName) {
		return attributeDefinitions.stream()
						.map(attributeDefinition -> createAttribute(attributeDefinition, definition, interfaceName))
						.collect(toList());
	}

	private static String createAttribute(AttributeDefinition<?> attributeDefinition,
																				EntityDefinition definition, String interfaceName) {
		if (attributeDefinition instanceof ColumnDefinition) {
			ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;

			return columnDefinition(interfaceName, columnDefinition,
							definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()),
							definition.primaryKey().columns().size() > 1, definition.readOnly());
		}

		return foreignKeyDefinition(interfaceName, (ForeignKeyDefinition) attributeDefinition);
	}

	private static void appendAttribute(TypeSpec.Builder interfaceBuilder,
																			AttributeDefinition<?> attributeDefinition) {
		if (attributeDefinition instanceof ColumnDefinition) {
			ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
			FieldSpec.Builder columnBuilder = FieldSpec.builder(ParameterizedTypeName.get(Column.class,
															columnDefinition.attribute().type().valueClass()),
											columnDefinition.name().toUpperCase())
							.addModifiers(PUBLIC, STATIC, FINAL);
			addInitializer(columnBuilder, columnDefinition);
			interfaceBuilder.addField(columnBuilder.build());
		}
		else if (attributeDefinition instanceof ForeignKeyDefinition) {
			ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
			//todo wrap references if more than four
			interfaceBuilder.addField(FieldSpec.builder(ForeignKey.class,
											attributeDefinition.attribute().name().toUpperCase())
							.addModifiers(PUBLIC, STATIC, FINAL)
							.initializer("TYPE.foreignKey($S, $L)",
											attributeDefinition.attribute().name().toLowerCase(),
											createReferences(foreignKeyDefinition))
							.build());
		}
	}

	private static void addInitializer(FieldSpec.Builder columnBuilder,
																		 ColumnDefinition<?> columnDefinition) {
		if (Object.class.equals(columnDefinition.attribute().type().valueClass())) {
			//special handling for mapping unknown column data types to Object columns
			columnBuilder.initializer("TYPE.column($S, $L)",
							columnDefinition.name().toLowerCase(), "Object.class");
		}
		else {
			columnBuilder.initializer("TYPE.$LColumn($S)",
							attributeTypePrefix(columnDefinition.attribute().type().valueClass().getSimpleName()),
							columnDefinition.name().toLowerCase());
		}
	}

	private static String createReferences(ForeignKeyDefinition foreignKeyDefinition) {
		return foreignKeyDefinition.references().stream()
						.map(reference -> new StringBuilder()
										.append(reference.column().name().toUpperCase()).append(", ")
										.append(interfaceName(reference.foreign().entityType().name(), true))
										.append(".").append(reference.foreign().name().toUpperCase())
										.toString())
						.collect(joining(", "));
	}

	private static String foreignKeyDefinition(String interfaceName, ForeignKeyDefinition definition) {
		String foreignKeyName = definition.attribute().name().toUpperCase();

		return new StringBuilder().append(DOUBLE_INDENT).append(interfaceName)
						.append(".").append(foreignKeyName).append(".define()")
						.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
						.append(".foreignKey()")
						.append(LINE_SEPARATOR)
						.append(TRIPLE_INDENT).append(".caption(\"").append(definition.caption()).append("\")")
						.toString();
	}

	private static String columnDefinition(String interfaceName, ColumnDefinition<?> column,
																				 boolean foreignKeyColumn, boolean compositePrimaryKey, boolean readOnly) {
		StringBuilder builder = new StringBuilder(DOUBLE_INDENT)
						.append(interfaceName).append(".").append(column.name().toUpperCase()).append(".define()")
						.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
						.append(".").append(definitionType(column, compositePrimaryKey));
		if (auditColumn(column)) {
			return builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
							.append(".caption(").append("\"").append(column.caption()).append("\")")
							.toString();
		}
		if (!foreignKeyColumn && !column.primaryKey()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".caption(").append("\"").append(column.caption()).append("\")");
		}
		if (!column.nullable() && !column.primaryKey()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".nullable(false)");
		}
		if (column.lazy()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".lazy(true)");
		}
		if (!readOnly) {
			if (column.columnHasDefaultValue()) {
				builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".columnHasDefaultValue(true)");
			}
			if (column.attribute().type().isString() && column.maximumLength() != -1) {
				builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".maximumLength(")
								.append(column.maximumLength()).append(")");
			}
			if (column.attribute().type().isDecimal() && column.maximumFractionDigits() >= 1) {
				builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".maximumFractionDigits(")
								.append(column.maximumFractionDigits()).append(")");
			}
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
		if (auditColumn(column)) {
			AuditColumnDefinition<?> auditColumnDefinition = (AuditColumnDefinition<?>) column;
			switch (auditColumnDefinition.auditAction()) {
				case INSERT:
					return new StringBuilder("auditColumn()").append(LINE_SEPARATOR)
									.append(TRIPLE_INDENT).append(column.attribute().type().isString() ? ".insertUser()" : ".insertTime()").toString();
				case UPDATE:
					return new StringBuilder("auditColumn()").append(LINE_SEPARATOR)
									.append(TRIPLE_INDENT).append(column.attribute().type().isString() ? ".updateUser()" : ".updateTime()").toString();
				default:
					throw new IllegalArgumentException("Uknown audit action: " + auditColumnDefinition.auditAction());
			}
		}

		return "column()";
	}

	private static boolean auditColumn(ColumnDefinition<?> column) {
		return column instanceof AuditColumnDefinition<?>;
	}

	private static String interfaceName(String tableName, boolean uppercase) {
		String name = requireNonNull(tableName).toLowerCase();
		if (name.contains(".")) {
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		name = underscoreToCamelCase(name);
		if (uppercase) {
			name = name.substring(0, 1).toUpperCase() + name.substring(1);
		}

		return name;
	}

	private static String removeInterfaceLineBreaks(String sourceString) {
		String[] lines = sourceString.split("\n");
		for (int i = 1; i < lines.length - 1; i++) {
			String line = lines[i];
			if (line != null && line.trim().isEmpty() && betweenColumnsOrForeignKeys(lines, i)) {
				lines[i] = null;
			}
		}

		return Arrays.stream(lines)
						.filter(Objects::nonNull)
						.collect(joining("\n"));
	}

	private String addInterfaceImports(String sourceString,
																		 String parentInterface) {
		List<String> interfaceNames = sortedDefinitions.stream()
						.map(DomainSource::createInterface)
						.map(interfaceSpec -> interfaceSpec.name)
						.sorted()
						.collect(toList());
		List<String> lines = new ArrayList<>();
		for (String line : sourceString.split("\n")) {
			lines.add(line);
			if (line.endsWith("EntityDefinition;")) {
				interfaceNames.forEach(name -> lines.add("import " + parentInterface + "." + name + ";"));
			}
		}

		return String.join("\n", lines);
	}

	private static boolean betweenColumnsOrForeignKeys(String[] lines, int lineIndex) {
		return betweenLinesStartingWith(lines, lineIndex, "Column")
						|| betweenLinesStartingWith(lines, lineIndex, "ForeignKey");
	}

	private static boolean betweenLinesStartingWith(String[] lines, int lineIndex, String prefix) {
		String previousLine = lines[lineIndex - 1];
		String nextLine = lines[lineIndex + 1];

		return lineStartsWith(previousLine, prefix)
						&& lineStartsWith(nextLine, prefix);
	}

	private static boolean lineStartsWith(String line, String prefix) {
		return line != null && line.trim().startsWith(prefix);
	}

	public static String apiSearchString(EntityDefinition definition) {
		return "interface " + interfaceName(definition.tableName(), true) + " ";
	}

	public static String implSearchString(EntityDefinition definition) {
		return "EntityDefinition " + interfaceName(definition.tableName(), false) + "()";
	}

	private static List<EntityDefinition> sortDefinitions(Domain domain) {
		Map<EntityType, Set<EntityType>> dependencies = dependencies(domain);
		Collection<EntityDefinition> definitions = domain.entities().definitions();

		return concat(definitions.stream()
										.filter(definition -> dependencies.get(definition.entityType()).isEmpty())
										.sorted(comparing(EntityDefinition::tableName)),
						definitions.stream()
										.filter(definition -> !dependencies.get(definition.entityType()).isEmpty())
										.sorted(comparing(EntityDefinition::tableName))
										.sorted(new DependencyOrder(dependencies)))
						.collect(toList());
	}

	private static boolean cyclicalDependencies(Collection<EntityDefinition> definitions) {
		Map<EntityType, EntityDefinition> definitionMap = definitions.stream()
						.collect(toMap(EntityDefinition::entityType, identity()));
		for (EntityDefinition definition : definitions) {
			Set<EntityType> dependencies = dependencies(definition.foreignKeys().get(), new HashSet<>(), definitionMap);
			if (dependencies.contains(definition.entityType())) {
				return true;
			}
		}

		return false;
	}

	private static Map<EntityType, Set<EntityType>> dependencies(Domain domain) {
		Map<EntityType, EntityDefinition> definitions = domain.entities().definitions().stream()
						.collect(toMap(EntityDefinition::entityType, identity()));

		return domain.entities().definitions().stream()
						.collect(toMap(EntityDefinition::entityType,
										definition -> dependencies(definition, definitions)));
	}

	private static Set<EntityType> dependencies(EntityDefinition definition,
																							Map<EntityType, EntityDefinition> definitions) {
		return dependencies(definition.foreignKeys().get(), new HashSet<>(), definitions);
	}

	private static Set<EntityType> dependencies(Collection<ForeignKey> foreignKeys,
																							Set<EntityType> dependencies,
																							Map<EntityType, EntityDefinition> definitions) {
		foreignKeys.stream()
						.filter(foreignKey -> !foreignKey.referencedType().equals(foreignKey.entityType()))
						.filter(foreignKey -> !dependencies.contains(foreignKey.referencedType()))
						.forEach(foreignKey -> {
							dependencies.add(foreignKey.referencedType());
							dependencies.addAll(dependencies(definitions.get(foreignKey.referencedType())
											.foreignKeys().get(), dependencies, definitions));
						});

		return dependencies;
	}

	private boolean identityKeyGeneratorUsed() {
		return sortedDefinitions.stream()
						.map(entityDefinition -> entityDefinition.primaryKey().generator())
						.anyMatch(KeyGenerator.Identity.class::isInstance);
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

	private static final class DependencyOrder implements Comparator<EntityDefinition> {

		private final Map<EntityType, Set<EntityType>> dependencies;

		private DependencyOrder(Map<EntityType, Set<EntityType>> dependencies) {
			this.dependencies = dependencies;
		}

		@Override
		public int compare(EntityDefinition definition1, EntityDefinition definition2) {
			if (dependencies.get(definition1.entityType()).contains(definition2.entityType())) {
				return 1;
			}
			else if (dependencies.get(definition2.entityType()).contains(definition1.entityType())) {
				return -1;
			}

			return 0;
		}
	}
}
