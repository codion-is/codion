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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

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
import java.util.function.BooleanSupplier;

import static com.palantir.javapoet.MethodSpec.constructorBuilder;
import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static com.palantir.javapoet.TypeSpec.interfaceBuilder;
import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static javax.lang.model.element.Modifier.*;

/**
 * For instances use the builder provded by {@link #builder()}.
 */
public final class DomainSource {

	private static final String INDENT = "\t";
	private static final String DOUBLE_INDENT = INDENT + INDENT;
	private static final String TRIPLE_INDENT = DOUBLE_INDENT + INDENT;
	private static final String DOMAIN_STRING = "DOMAIN";
	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final Domain domain;
	private final String domainInterfaceName;
	private final List<EntityDefinition> sortedDefinitions;

	private final String domainPackage;
	private final Set<EntityType> dtos;
	private final boolean i18n;

	private DomainSource(DefaultBuilder builder) {
		this.domain = builder.domain;
		this.domainInterfaceName = interfaceName(requireNonNull(domain).type().name(), true);
		this.sortedDefinitions = sortDefinitions(domain);
		this.domainPackage = builder.domainPackage;
		this.dtos = builder.dtos;
		this.i18n = builder.i18n;
	}

	/**
	 * @return a new {@link DomainSource.Builder.DomainStep} instance.
	 */
	public static Builder.DomainStep builder() {
		return new DefaultBuilder.DefaultDomainStep();
	}

	/**
	 * @return the api source code.
	 */
	public String api() {
		return toApiString(domainPackage + ".api", dtos, i18n);
	}

	/**
	 * @return the implementation source code.
	 */
	public String implementation() {
		return toImplementationString(domainPackage, i18n);
	}

	/**
	 * @return the combined source code of the api and implementation.
	 */
	public String combined() {
		return toCombinedString(domainPackage, dtos, i18n);
	}

	/**
	 * Writes the api and implementation source code to the given path.
	 * @param sourcePath the path to write the source files to
	 * @param resourcePath the path to write the resources to
	 * @param overwrite used to confirm overwrite if either of the api or impl files exist
	 * @return true if the files were written, false if overwriting was not confirmed
	 * @throws IOException in case of an I/O error.
	 */
	public boolean writeApiImpl(Path sourcePath, Path resourcePath, BooleanSupplier overwrite) throws IOException {
		String interfaceName = interfaceName(domain.type().name(), true);
		Files.createDirectories(requireNonNull(sourcePath).resolve("api"));
		Path apiPath = sourcePath.resolve("api").resolve(interfaceName + ".java");
		Path implPath = sourcePath.resolve(interfaceName + "Impl.java");
		if ((!apiPath.toFile().exists() && !implPath.toFile().exists()) || requireNonNull(overwrite).getAsBoolean()) {
			Files.write(apiPath, singleton(api()));
			Files.write(implPath, singleton(implementation()));
			if (i18n) {
				writeI18n(resourcePath, true);
			}

			return true;
		}

		return false;
	}

	/**
	 * Writes the combined source code to the given path.
	 * @param sourcePath the path to write the source files to
	 * @param resourcePath the path to write the resources to
	 * @param overwrite used to confirm overwrite if either of the api or impl files exist
	 * @return true if the files were written, false if overwriting was not confirmed
	 * @throws IOException in case of an I/O error.
	 */
	public boolean writeCombined(Path sourcePath, Path resourcePath, BooleanSupplier overwrite) throws IOException {
		String interfaceName = interfaceName(domain.type().name(), true);
		Files.createDirectories(requireNonNull(sourcePath));
		Path combinedFile = sourcePath.resolve(interfaceName + ".java");
		if (!combinedFile.toFile().exists() || requireNonNull(overwrite).getAsBoolean()) {
			Files.write(combinedFile, singleton(combined()));
			if (i18n) {
				writeI18n(resourcePath, false);
			}

			return true;
		}

		return false;
	}

	/**
	 * Builds a {@link DomainSource} instance
	 */
	public interface Builder {

		/**
		 * The first step in building a {@link DomainSource} instance
		 */
		interface DomainStep {

			/**
			 * @param domain the domain model
			 * @return a new {@link Builder}
			 */
			Builder domain(Domain domain);
		}

		/**
		 * @param domainPackage the domain package
		 * @return this builder
		 */
		Builder domainPackage(String domainPackage);

		/**
		 * @param dtos the entity types for which to define dtos
		 * @return this builder
		 */
		Builder dtos(Set<EntityType> dtos);

		/**
		 * @param i18n true if i18n resources are being used
		 * @return this builder
		 */
		Builder i18n(boolean i18n);

		/**
		 * @return a new {@link DomainSource} instance
		 */
		DomainSource build();
	}

	private void writeI18n(Path resourcePath, boolean api) throws IOException {
		requireNonNull(resourcePath);
		if (api) {
			resourcePath = resourcePath.resolve("api");
		}
		Files.createDirectories(resourcePath);
		for (EntityDefinition definition : domain.entities().definitions()) {
			Path filePath = resourcePath.resolve(domainInterfaceName + "$" + interfaceName(definition.table(), true) + ".properties");
			Files.write(filePath, singleton(i18n(definition.type())));
		}
	}

	String i18n(EntityType entityType) {
		EntityDefinition definition = domain.entities().definition(entityType);
		StringBuilder builder = new StringBuilder();
		builder.append(definition.type().name()).append("=").append(definition.caption()).append(LINE_SEPARATOR);
		definition.description().ifPresent(description ->
						builder.append(definition.type().name()).append(".description=")
										.append("=").append(description).append(LINE_SEPARATOR));
		definition.attributes().definitions().stream()
						.filter(attribute -> !generatedPrimaryKeyColumn(definition, attribute))
						.filter(attribute -> !foreignKeyColumn(definition, attribute))
						.forEach(attribute -> {
							builder.append(attribute.attribute().name())
											.append("=").append(attribute.caption()).append(LINE_SEPARATOR);
							attribute.description().ifPresent(description ->
											builder.append(attribute.attribute().name()).append(".description")
															.append("=").append(description).append(LINE_SEPARATOR));
						});

		return builder.toString().trim();
	}

	private String toApiString(String sourcePackage, Set<EntityType> dtos, boolean i18n) {
		TypeSpec.Builder classBuilder = interfaceBuilder(domainInterfaceName)
						.addModifiers(PUBLIC)
						.addField(FieldSpec.builder(DomainType.class, DOMAIN_STRING)
										.addModifiers(PUBLIC, STATIC, FINAL)
										.initializer("domainType($L)", domainInterfaceName + ".class")
										.build());

		sortedDefinitions.forEach(definition -> classBuilder.addType(createInterface(definition, dtos, i18n)));

		return removeInterfaceLineBreaks(JavaFile.builder(sourcePackage.isEmpty() ? "" : sourcePackage, classBuilder.build())
						.addStaticImport(DomainType.class, "domainType")
						.skipJavaLangImports(true)
						.indent(INDENT)
						.build()
						.toString());
	}

	private String toImplementationString(String sourcePackage, boolean i18n) {
		TypeSpec.Builder classBuilder = classBuilder(domainInterfaceName + "Impl")
						.addModifiers(PUBLIC, FINAL)
						.superclass(DomainModel.class);

		Map<EntityDefinition, String> definitionMethods = addDefinitionMethods(classBuilder, i18n);

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
			fileBuilder.addStaticImport(ClassName.bestGuess(sourcePackage + ".api." + domainInterfaceName), DOMAIN_STRING);
		}

		String sourceString = fileBuilder.build().toString();
		if (!sourcePackage.isEmpty()) {
			sourceString = addInterfaceImports(sourceString, sourcePackage + ".api." + domainInterfaceName);
		}

		return sourceString;
	}

	private String toCombinedString(String sourcePackage, Set<EntityType> dtos, boolean i18n) {
		TypeSpec.Builder classBuilder = classBuilder(domainInterfaceName)
						.addModifiers(PUBLIC, FINAL)
						.addField(FieldSpec.builder(DomainType.class, DOMAIN_STRING)
										.addModifiers(PUBLIC, STATIC, FINAL)
										.initializer("domainType($L)", domainInterfaceName + ".class")
										.build())
						.superclass(DomainModel.class);

		Map<EntityDefinition, String> definitionMethods = addDefinitionMethods(classBuilder, i18n);
		sortedDefinitions.forEach(definition -> classBuilder.addType(createInterface(definition, dtos, i18n)));

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
			fileBuilder.addStaticImport(ClassName.bestGuess(sourcePackage + "." + domainInterfaceName), DOMAIN_STRING);
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

	private Map<EntityDefinition, String> addDefinitionMethods(TypeSpec.Builder classBuilder, boolean i18n) {
		Map<EntityDefinition, String> definitionMethods = new LinkedHashMap<>();
		sortedDefinitions.forEach(definition ->
						addDefinition(definition, classBuilder, definitionMethods::put, i18n));

		return definitionMethods;
	}

	private static void addDefinition(EntityDefinition definition,
																		TypeSpec.Builder classBuilder,
																		BiConsumer<EntityDefinition, String> onMethod, boolean i18n) {
		MethodSpec definitionMethod = createDefinitionMethod(definition, i18n);
		classBuilder.addMethod(definitionMethod);
		onMethod.accept(definition, definitionMethod.name());
	}

	private TypeSpec createInterface(EntityDefinition definition, Set<EntityType> dtos, boolean i18n) {
		String interfaceName = interfaceName(definition.table(), true);
		TypeSpec.Builder interfaceBuilder = interfaceBuilder(interfaceName)
						.addModifiers(PUBLIC, STATIC)
						.addField(createEntityType(definition, i18n, interfaceName));
		definition.attributes().get().stream()
						.filter(Column.class::isInstance)
						.forEach(column -> appendAttribute(interfaceBuilder, column));
		definition.attributes().get().stream()
						.filter(ForeignKey.class::isInstance)
						.forEach(foreignKey -> appendAttribute(interfaceBuilder, foreignKey));

		if (dtos.contains(definition.type())) {
			addDtoRecord(definition, interfaceBuilder, dtos);
		}

		return interfaceBuilder.build();
	}

	private static FieldSpec createEntityType(EntityDefinition definition, boolean i18n, String interfaceName) {
		FieldSpec.Builder builder = FieldSpec.builder(EntityType.class, "TYPE")
						.addModifiers(PUBLIC, STATIC, FINAL);
		if (i18n) {
			builder.initializer("DOMAIN.entityType($S, $L)", definition.table().toLowerCase(), interfaceName + ".class.getName()");
		}
		else {
			builder.initializer("DOMAIN.entityType($S)", definition.table().toLowerCase());
		}

		return builder.build();
	}

	private void addDtoRecord(EntityDefinition definition, TypeSpec.Builder interfaceBuilder, Set<EntityType> dtos) {
		List<Attribute<?>> nonForeignKeyColumnAttributes = definition.attributes().get().stream()
						.filter(attribute -> excludeNonDtoForeignKeys(attribute, dtos))
						.filter(attribute -> noneForeignKeyColumn(attribute, definition))
						.collect(toList());

		interfaceBuilder.addType(dtoRecord(nonForeignKeyColumnAttributes));
		interfaceBuilder.addMethod(dtoFromEntityMethod(nonForeignKeyColumnAttributes, definition));
	}

	private TypeSpec dtoRecord(List<Attribute<?>> attributes) {
		TypeSpec.Builder dtoBuilder = TypeSpec.recordBuilder("Dto")
						.addModifiers(PUBLIC, STATIC);
		MethodSpec.Builder constructorBuilder = constructorBuilder();

		attributes.forEach(attribute -> addRecordField(attribute, constructorBuilder));

		return dtoBuilder.recordConstructor(constructorBuilder.addModifiers(PUBLIC).build())
						.addMethod(entityFromDtoMethod(attributes))
						.build();
	}

	private void addRecordField(Attribute<?> attribute, MethodSpec.Builder constructorBuilder) {
		if (attribute instanceof Column<?>) {
			constructorBuilder.addParameter(ParameterSpec.builder(((Column<?>) attribute).type().valueClass(),
							underscoreToCamelCase(attribute.name().toLowerCase())).build());
		}
		else if (attribute instanceof ForeignKey) {
			EntityDefinition referenced = sortedDefinitions.stream()
							.filter(definition -> definition.type().equals(((ForeignKey) attribute).referencedType()))
							.findFirst()
							.orElseThrow();
			constructorBuilder.addParameter(ParameterSpec.builder(dtoName(referenced),
							underscoreToCamelCase(attribute.name().toLowerCase().replace("_fk", "").replace("fk", ""))).build());
		}
	}

	private static MethodSpec entityFromDtoMethod(List<Attribute<?>> attributes) {
		return methodBuilder("entity")
						.addModifiers(PUBLIC)
						.returns(Entity.class)
						.addParameter(ParameterSpec.builder(Entities.class, "entities").build())
						.addCode(entityFromDtoMethodBody(attributes))
						.build();
	}

	private static String entityFromDtoMethodBody(Collection<Attribute<?>> attributes) {
		StringBuilder builder = new StringBuilder("return entities.entity(TYPE)\n");
		attributes.forEach(attribute -> {
			if (attribute instanceof Column<?>) {
				builder.append("\t.with(")
								.append(attribute.name().toUpperCase())
								.append(", ")
								.append(underscoreToCamelCase(attribute.name().toLowerCase()))
								.append(")\n");
			}
			else if (attribute instanceof ForeignKey) {
				builder.append("\t.with(")
								.append(attribute.name().toUpperCase())
								.append(", ")
								.append(underscoreToCamelCase(attribute.name().toLowerCase().replace("_fk", "").replace("fk", "")))
								.append(".entity(entities)")
								.append(")\n");
			}
		});

		return builder.append("\t.build();").toString();
	}

	private MethodSpec dtoFromEntityMethod(List<Attribute<?>> attributes, EntityDefinition definition) {
		return MethodSpec.methodBuilder("dto")
						.addModifiers(PUBLIC, STATIC)
						.returns(ClassName.get("", "Dto"))
						.addParameter(Entity.class, interfaceName(definition.table(), false))
						.addCode(dtoFromEntityMethodBody(attributes, interfaceName(definition.table(), false)))
						.build();
	}

	private String dtoFromEntityMethodBody(List<Attribute<?>> attributes, String parameter) {
		List<String> arguments = new ArrayList<>();
		attributes.forEach(attribute -> {
			if (attribute instanceof Column<?>) {
				arguments.add(parameter + ".get(" + attribute.name().toUpperCase() + ")");
			}
			else if (attribute instanceof ForeignKey) {
				EntityDefinition referenced = sortedDefinitions.stream()
								.filter(definition -> definition.type().equals(((ForeignKey) attribute).referencedType()))
								.findFirst()
								.orElseThrow();
				arguments.add(interfaceName(referenced.table(), true)
								+ ".dto(" + parameter + ".get(" + attribute.name().toUpperCase() + "))");
			}
		});

		return new StringBuilder("return ")
						.append(parameter)
						.append(" == null ? null :\n")
						.append("\tnew Dto(")
						.append(String.join(",\n\t\t", arguments))
						.append(");").toString();
	}

	private static TypeName dtoName(EntityDefinition referenced) {
		return ClassName.get("", interfaceName(referenced.table(), true) + ".Dto");
	}

	private static boolean excludeNonDtoForeignKeys(Attribute<?> attribute, Set<EntityType> dtos) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;

			return dtos.contains(foreignKey.referencedType());
		}

		return true;
	}

	private static boolean noneForeignKeyColumn(Attribute<?> attribute, EntityDefinition entityDefinition) {
		if (attribute instanceof Column) {
			return !entityDefinition.foreignKeys().foreignKeyColumn(((Column<?>) attribute));
		}

		return true;
	}

	private static MethodSpec createDefinitionMethod(EntityDefinition definition, boolean i18n) {
		String interfaceName = interfaceName(definition.table(), true);
		StringBuilder builder = new StringBuilder()
						.append("return ").append(interfaceName).append(".TYPE.define(").append(LINE_SEPARATOR)
						.append(String.join("," + LINE_SEPARATOR,
										createAttributes(definition.attributes().definitions(), definition, interfaceName, i18n)))
						.append(")");
		if (definition.primaryKey().generated()) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".keyGenerator(identity())");
		}
		if (!i18n && !nullOrEmpty(definition.caption())) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".caption(\"").append(definition.caption()).append("\")");
		}
		if (!i18n) {
			definition.description().ifPresent(description ->
							builder.append(LINE_SEPARATOR).append(INDENT).append(".description(\"").append(description).append("\")"));
		}
		if (definition.readOnly()) {
			builder.append(LINE_SEPARATOR).append(INDENT).append(".readOnly(true)");
		}
		builder.append(LINE_SEPARATOR).append(INDENT).append(".build();");

		return methodBuilder(interfaceName(definition.table(), false))
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
																							 EntityDefinition definition, String interfaceName, boolean i18n) {
		return attributeDefinitions.stream()
						.map(attributeDefinition -> createAttribute(attributeDefinition, definition, interfaceName, i18n))
						.collect(toList());
	}

	private static String createAttribute(AttributeDefinition<?> attributeDefinition,
																				EntityDefinition definition, String interfaceName, boolean i18n) {
		if (attributeDefinition instanceof ColumnDefinition) {
			ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;

			return columnDefinition(interfaceName, columnDefinition,
							definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()),
							definition.primaryKey().columns().size() > 1, definition.readOnly(), i18n);
		}

		return foreignKeyDefinition(interfaceName, (ForeignKeyDefinition) attributeDefinition, i18n);
	}

	private static void appendAttribute(TypeSpec.Builder interfaceBuilder, Attribute<?> attribute) {
		if (attribute instanceof Column) {
			Column<?> column = (Column<?>) attribute;
			FieldSpec.Builder columnBuilder = FieldSpec.builder(ParameterizedTypeName.get(Column.class,
															column.type().valueClass()),
											column.name().toUpperCase())
							.addModifiers(PUBLIC, STATIC, FINAL);
			addInitializer(columnBuilder, column);
			interfaceBuilder.addField(columnBuilder.build());
		}
		else if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;
			//todo wrap references if more than four
			interfaceBuilder.addField(FieldSpec.builder(ForeignKey.class,
											attribute.name().toUpperCase())
							.addModifiers(PUBLIC, STATIC, FINAL)
							.initializer("TYPE.foreignKey($S, $L)",
											attribute.name().toLowerCase(),
											createReferences(foreignKey))
							.build());
		}
	}

	private static void addInitializer(FieldSpec.Builder columnBuilder,
																		 Column<?> column) {
		if (Object.class.equals(column.type().valueClass())) {
			//special handling for mapping unknown column data types to Object columns
			columnBuilder.initializer("TYPE.column($S, $L)",
							column.name().toLowerCase(), "Object.class");
		}
		else {
			columnBuilder.initializer("TYPE.$LColumn($S)",
							attributeTypePrefix(column.type().valueClass().getSimpleName()),
							column.name().toLowerCase());
		}
	}

	private static String createReferences(ForeignKey foreignKey) {
		return foreignKey.references().stream()
						.map(reference -> new StringBuilder()
										.append(reference.column().name().toUpperCase()).append(", ")
										.append(interfaceName(reference.foreign().entityType().name(), true))
										.append(".").append(reference.foreign().name().toUpperCase())
										.toString())
						.collect(joining(", "));
	}

	private static String foreignKeyDefinition(String interfaceName, ForeignKeyDefinition definition, boolean i18n) {
		String foreignKeyName = definition.attribute().name().toUpperCase();

		StringBuilder builder = new StringBuilder().append(DOUBLE_INDENT).append(interfaceName)
						.append(".").append(foreignKeyName).append(".define()")
						.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
						.append(".foreignKey()");
		if (!i18n) {
			builder.append(LINE_SEPARATOR)
							.append(TRIPLE_INDENT).append(".caption(\"").append(definition.caption()).append("\")");
		}

		return builder.toString();
	}

	private static String columnDefinition(String interfaceName, ColumnDefinition<?> column,
																				 boolean foreignKeyColumn, boolean compositePrimaryKey, boolean readOnlyEntity, boolean i18n) {
		StringBuilder builder = new StringBuilder(DOUBLE_INDENT)
						.append(interfaceName).append(".").append(column.name().toUpperCase()).append(".define()")
						.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
						.append(".").append(definitionType(column, compositePrimaryKey));
		if (!i18n && !foreignKeyColumn && !column.primaryKey()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".caption(").append("\"").append(column.caption()).append("\")");
		}
		if (!readOnlyEntity) {
			if (column.readOnly()) {
				builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".readOnly(true)");
			}
			else {
				if (!column.nullable() && !column.primaryKey()) {
					builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".nullable(false)");
				}
				if (!column.insertable()) {
					builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".insertable(false)");
				}
				else if (column.withDefault()) {
					builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".withDefault(true)");
				}
				if (!column.updatable() && !column.primaryKey()) {
					builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".updatable(false)");
				}
				if (column.attribute().type().isString() && column.maximumLength() != -1) {
					builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".maximumLength(")
									.append(column.maximumLength()).append(")");
				}
			}
		}
		if (!column.selected()) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".selected(false)");
		}
		if (column.attribute().type().isDecimal() && column.fractionDigits() >= 1) {
			builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT).append(".fractionDigits(")
							.append(column.fractionDigits()).append(")");
		}
		if (!i18n) {
			column.description().ifPresent(description ->
							builder.append(LINE_SEPARATOR).append(TRIPLE_INDENT)
											.append(".description(").append("\"").append(description).append("\")"));
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
			return compositePrimaryKey ? "primaryKey(" + column.keyIndex() + ")" : "primaryKey()";
		}

		return "column()";
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
						.map(definition -> interfaceName(definition.table(), true))
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
		return "interface " + interfaceName(definition.table(), true) + " ";
	}

	public static String implSearchString(EntityDefinition definition) {
		return "EntityDefinition " + interfaceName(definition.table(), false) + "()";
	}

	private static List<EntityDefinition> sortDefinitions(Domain domain) {
		Map<EntityType, Set<EntityType>> dependencies = dependencies(domain);
		Collection<EntityDefinition> definitions = domain.entities().definitions();

		return concat(definitions.stream()
										.filter(definition -> dependencies.get(definition.type()).isEmpty())
										.sorted(comparing(EntityDefinition::table)),
						definitions.stream()
										.filter(definition -> !dependencies.get(definition.type()).isEmpty())
										.sorted(comparing(EntityDefinition::table))
										.sorted(new DependencyOrder(dependencies)))
						.collect(toList());
	}

	private static boolean cyclicalDependencies(Collection<EntityDefinition> definitions) {
		Map<EntityType, EntityDefinition> definitionMap = definitions.stream()
						.collect(toMap(EntityDefinition::type, identity()));
		for (EntityDefinition definition : definitions) {
			Set<EntityType> dependencies = dependencies(definition.foreignKeys().get(), new HashSet<>(), definitionMap);
			if (dependencies.contains(definition.type())) {
				return true;
			}
		}

		return false;
	}

	private static Map<EntityType, Set<EntityType>> dependencies(Domain domain) {
		Map<EntityType, EntityDefinition> definitions = domain.entities().definitions().stream()
						.collect(toMap(EntityDefinition::type, identity()));

		return domain.entities().definitions().stream()
						.collect(toMap(EntityDefinition::type,
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
						.anyMatch(generator -> generator == KeyGenerator.identity());
	}

	static String underscoreToCamelCase(String text) {
		if (!requireNonNull(text).contains("_")) {
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

	private static boolean generatedPrimaryKeyColumn(EntityDefinition definition, AttributeDefinition<?> attribute) {
		return attribute instanceof ColumnDefinition<?> &&
						((ColumnDefinition<?>) attribute).primaryKey() && definition.primaryKey().generated();
	}

	private static boolean foreignKeyColumn(EntityDefinition definition, AttributeDefinition<?> attribute) {
		return attribute instanceof ColumnDefinition<?> &&
						definition.foreignKeys().foreignKeyColumn((Column<?>) attribute.attribute());
	}

	private static final class DependencyOrder implements Comparator<EntityDefinition> {

		private final Map<EntityType, Set<EntityType>> dependencies;

		private DependencyOrder(Map<EntityType, Set<EntityType>> dependencies) {
			this.dependencies = dependencies;
		}

		@Override
		public int compare(EntityDefinition definition1, EntityDefinition definition2) {
			if (dependencies.get(definition1.type()).contains(definition2.type())) {
				return 1;
			}
			else if (dependencies.get(definition2.type()).contains(definition1.type())) {
				return -1;
			}

			return 0;
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final Domain domain;

		private String domainPackage = "no.package";
		private Set<EntityType> dtos = emptySet();
		private boolean i18n = false;

		private DefaultBuilder(Domain domain) {
			this.domain = domain;
		}

		private static final class DefaultDomainStep implements DomainStep {

			@Override
			public Builder domain(Domain domain) {
				return new DefaultBuilder(requireNonNull(domain));
			}
		}

		@Override
		public Builder domainPackage(String domainPackage) {
			this.domainPackage = requireNonNull(domainPackage);
			return this;
		}

		@Override
		public Builder dtos(Set<EntityType> dtos) {
			this.dtos = new HashSet<>(requireNonNull(dtos));
			return this;
		}

		@Override
		public Builder i18n(boolean i18n) {
			this.i18n = i18n;
			return this;
		}

		@Override
		public DomainSource build() {
			return new DomainSource(this);
		}
	}
}
