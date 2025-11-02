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
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.Column.Generator.Identity;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.SourceVersion;
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
import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static javax.lang.model.element.Modifier.*;

/**
 * For instances use the builder provided by {@link #builder()}.
 */
public final class DomainSource {

	private static final String INDENT = "\t";
	private static final String DOUBLE_INDENT = INDENT + INDENT;
	private static final String TRIPLE_INDENT = DOUBLE_INDENT + INDENT;
	private static final String DOMAIN_STRING = "DOMAIN";
	private static final String TYPE_FIELD_NAME = "TYPE";
	private static final String DTO_CLASS_NAME = "Dto";
	private static final String DTO_METHOD_NAME = "dto";
	private static final String ENTITY_METHOD_NAME = "entity";
	private static final String ENTITIES_PARAM_NAME = "entities";
	private static final String FK_SUFFIX = "_fk";
	private static final String FK_ALTERNATE_SUFFIX = "fk";
	private static final String PROPERTIES_SUFFIX = ".properties";
	private static final String API_PACKAGE_NAME = "api";
	private static final String IMPL_CLASS_SUFFIX = "Impl";
	private static final String JAVA = ".java";
	private static final String RETURN = "return ";

	private final Domain domain;
	private final String domainInterfaceName;
	private final List<EntityDefinition> sortedDefinitions;
	private final Map<EntityType, EntityDefinition> definitionsByType;
	private final Collection<Attribute<?>> invalidNames;

	private final String domainPackage;
	private final Set<EntityType> dtos;
	private final boolean i18n;
	private final boolean test;

	private DomainSource(DefaultBuilder builder) {
		this.domain = builder.domain;
		this.domainInterfaceName = interfaceName(requireNonNull(domain).type().name(), true);
		this.sortedDefinitions = sortDefinitions(domain);
		this.definitionsByType = sortedDefinitions.stream()
						.collect(collectingAndThen(toMap(EntityDefinition::type, identity()), Map::copyOf));
		this.domainPackage = builder.domainPackage;
		this.dtos = builder.dtos;
		this.i18n = builder.i18n;
		this.test = builder.test;
		this.invalidNames = collectInvalidNames(sortedDefinitions);
		if (!invalidNames.isEmpty()) {
			System.err.println("Invalid domain attribute names: " + invalidNames);
		}
	}

	/**
	 * @return a new {@link DomainSource.Builder.DomainStep} instance.
	 */
	public static Builder.DomainStep builder() {
		return new DefaultBuilder.DefaultDomainStep();
	}

	// ========================================
	// Public API - Generation Methods
	// ========================================

	/**
	 * @return the api source code.
	 */
	public String api() {
		return toApiString(domainPackage + "." + API_PACKAGE_NAME, dtos, i18n);
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
	 * @return the i18n properties for all entities
	 */
	public String i18n() {
		return domain.entities().definitions().stream()
						.map(definition -> i18n(definition.type()))
						.collect(joining("\n\n"));
	}

	/**
	 * @return the domain model test implementation for api/impl
	 */
	public String testApiImpl() {
		return toTestString(domainPackage, true);
	}

	/**
	 * @return the domain model test implementation for combined
	 */
	public String testCombined() {
		return toTestString(domainPackage, false);
	}

	/**
	 * Writes the api and implementation source code to the given path.
	 * @param apiSourcePath the path to write the api source files to
	 * @param implSourcePath the path to write the implementation source files to
	 * @param apiResourcePath the path to write the api resources to
	 * @param testPath the path to write the unit test to
	 * @param overwrite used to confirm overwrite if either of the api or impl files exist
	 * @return true if the files were written, false if overwriting was not confirmed
	 * @throws IOException in case of an I/O error.
	 */
	public boolean writeApiImpl(Path apiSourcePath, Path implSourcePath, Path apiResourcePath, Path testPath, BooleanSupplier overwrite) throws IOException {
		String interfaceName = interfaceName(domain.type().name(), true);
		Files.createDirectories(requireNonNull(apiSourcePath).resolve(API_PACKAGE_NAME));
		Path apiPath = apiSourcePath.resolve(API_PACKAGE_NAME).resolve(interfaceName + JAVA);
		Files.createDirectories(requireNonNull(implSourcePath));
		Files.createDirectories(requireNonNull(testPath));
		Path implPath = implSourcePath.resolve(interfaceName + IMPL_CLASS_SUFFIX + JAVA);
		if ((!apiPath.toFile().exists() && !implPath.toFile().exists()) || requireNonNull(overwrite).getAsBoolean()) {
			Files.write(apiPath, singleton(api()));
			Files.write(implPath, singleton(implementation()));
			if (i18n) {
				writeI18n(apiResourcePath, true);
			}
			if (test) {
				Files.write(testPath.resolve(interfaceName + "Test" + JAVA), singleton(testApiImpl()));
			}

			return true;
		}

		return false;
	}

	/**
	 * Writes the combined source code to the given path.
	 * @param sourcePath the path to write the source files to
	 * @param resourcePath the path to write the resources to
	 * @param testPath the path to write the unit test to
	 * @param overwrite used to confirm overwrite if either of the api or impl files exist
	 * @return true if the files were written, false if overwriting was not confirmed
	 * @throws IOException in case of an I/O error.
	 */
	public boolean writeCombined(Path sourcePath, Path resourcePath, Path testPath, BooleanSupplier overwrite) throws IOException {
		String interfaceName = interfaceName(domain.type().name(), true);
		Files.createDirectories(requireNonNull(sourcePath));
		Files.createDirectories(requireNonNull(testPath));
		Path combinedFile = sourcePath.resolve(interfaceName + JAVA);
		if (!combinedFile.toFile().exists() || requireNonNull(overwrite).getAsBoolean()) {
			Files.write(combinedFile, singleton(combined()));
			if (i18n) {
				writeI18n(resourcePath, false);
			}
			if (test) {
				Files.write(testPath.resolve(interfaceName + "Test" + JAVA), singleton(testCombined()));
			}

			return true;
		}

		return false;
	}

	// ========================================
	// Builder Interface
	// ========================================

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
		 * @param test true if domain unit test should be generated
		 * @return this builder
		 */
		Builder test(boolean test);

		/**
		 * @return a new {@link DomainSource} instance
		 */
		DomainSource build();
	}

	// ========================================
	// i18n Resource Generation
	// ========================================

	private void writeI18n(Path resourcePath, boolean api) throws IOException {
		requireNonNull(resourcePath);
		if (api) {
			resourcePath = resourcePath.resolve(API_PACKAGE_NAME);
		}
		Files.createDirectories(resourcePath);
		for (EntityDefinition definition : domain.entities().definitions()) {
			Path filePath = resourcePath.resolve(domainInterfaceName + "$" + interfaceName(definition, true) + PROPERTIES_SUFFIX);
			Files.write(filePath, singleton(i18n(definition.type())));
		}
	}

	String i18n(EntityType entityType) {
		EntityDefinition definition = domain.entities().definition(entityType);
		StringBuilder builder = new StringBuilder();
		builder.append(definition.type().name()).append("=").append(definition.caption()).append("\n");
		definition.description().ifPresent(description ->
						builder.append(definition.type().name()).append(".description=")
										.append("=").append(description).append("\n"));
		definition.attributes().definitions().stream()
						.filter(attribute -> !generatedPrimaryKeyColumn(attribute))
						.filter(attribute -> !foreignKeyColumn(definition, attribute))
						.forEach(attribute -> {
							builder.append(attribute.attribute().name())
											.append("=").append(attribute.caption()).append("\n");
							attribute.description().ifPresent(description ->
											builder.append(attribute.attribute().name()).append(".description")
															.append("=").append(description).append("\n"));
						});

		return builder.toString().trim();
	}

	// ========================================
	// API Generation
	// ========================================

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

	// ========================================
	// Implementation Generation
	// ========================================

	private String toImplementationString(String sourcePackage, boolean i18n) {
		TypeSpec.Builder classBuilder = classBuilder(domainInterfaceName + IMPL_CLASS_SUFFIX)
						.addModifiers(PUBLIC, FINAL)
						.superclass(DomainModel.class);

		Map<EntityDefinition, String> definitionMethods = addDefinitionMethods(classBuilder, i18n);

		String implementationPackage = sourcePackage.isEmpty() ? "" : sourcePackage;

		JavaFile.Builder fileBuilder = JavaFile.builder(implementationPackage,
										classBuilder.addMethod(createDomainConstructor(definitionMethods))
														.build())
						.skipJavaLangImports(true)
						.indent(INDENT);
		if (identityGeneratorUsed()) {
			fileBuilder.addStaticImport(Generator.class, "identity");
		}
		if (!implementationPackage.isEmpty()) {
			ClassName parentInterface = ClassName.get(sourcePackage + "." + API_PACKAGE_NAME, domainInterfaceName);
			fileBuilder.addStaticImport(parentInterface, DOMAIN_STRING);
			// Add static import for nested interfaces
			sortedDefinitions.forEach(definition ->
							fileBuilder.addStaticImport(parentInterface, interfaceName(definition, true)));
		}

		return fileBuilder.build().toString().trim();
	}

	// ========================================
	// Combined Generation (API + Implementation)
	// ========================================

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
		if (identityGeneratorUsed()) {
			fileBuilder.addStaticImport(Generator.class, "identity");
		}
		if (!sourcePackage.isEmpty()) {
			ClassName parentInterface = ClassName.get(sourcePackage, domainInterfaceName);
			fileBuilder.addStaticImport(parentInterface, DOMAIN_STRING);
			// Add static import for nested interfaces
			sortedDefinitions.forEach(definition ->
							fileBuilder.addStaticImport(parentInterface, interfaceName(definition, true)));
		}

		return removeInterfaceLineBreaks(fileBuilder.build().toString());
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
		String interfaceName = interfaceName(definition, true);
		TypeSpec.Builder interfaceBuilder = interfaceBuilder(interfaceName)
						.addModifiers(PUBLIC, STATIC)
						.addField(createEntityType(definition, i18n, interfaceName));
		definition.attributes().get().stream()
						.filter(Column.class::isInstance)
						.filter(column -> !invalidNames.contains(column))
						.forEach(column -> appendAttribute(interfaceBuilder, column));
		definition.attributes().get().stream()
						.filter(ForeignKey.class::isInstance)
						.filter(foreignKey -> !invalidNames.contains(foreignKey))
						.forEach(foreignKey -> appendAttribute(interfaceBuilder, foreignKey));

		if (dtos.contains(definition.type())) {
			addDtoRecord(definition, interfaceBuilder, dtos);
		}

		return interfaceBuilder.build();
	}

	private static FieldSpec createEntityType(EntityDefinition definition, boolean i18n, String interfaceName) {
		CaptionStrategy captionStrategy = i18n ? new I18nCaptionStrategy() : new LiteralCaptionStrategy();

		return FieldSpec.builder(EntityType.class, TYPE_FIELD_NAME)
						.addModifiers(PUBLIC, STATIC, FINAL)
						.initializer(captionStrategy.entityTypeInitializer(definition, interfaceName))
						.build();
	}

	// ========================================
	// Domain Test Generation
	// ========================================

	private String toTestString(String domainPackage, boolean apiImpl) {
		String testClassName = domainInterfaceName + "Test";
		// For api/impl separation, use DomainImpl; for combined, just use Domain
		String domainClassName = domainInterfaceName + (apiImpl ? IMPL_CLASS_SUFFIX : "");

		TypeSpec.Builder testClassBuilder = classBuilder(testClassName)
						.addModifiers(PUBLIC, FINAL)
						.superclass(ClassName.get("is.codion.framework.domain.test", "DomainTest"))
						.addMethod(createTestConstructor(domainClassName));

		sortedDefinitions.forEach(definition ->
						testClassBuilder.addMethod(createTestMethod(definition)));

		ClassName domainClass = domainPackage.isEmpty() ?
						ClassName.get("", domainInterfaceName) :
						ClassName.get(domainPackage + (apiImpl ? "." + API_PACKAGE_NAME : ""), domainInterfaceName);

		JavaFile.Builder fileBuilder = JavaFile.builder(domainPackage, testClassBuilder.build())
						.addStaticImport(domainClass, "*")
						.skipJavaLangImports(true)
						.indent(INDENT);

		return fileBuilder.build().toString().trim();
	}

	private static MethodSpec createTestConstructor(String domainClassName) {
		return constructorBuilder()
						.addModifiers(PUBLIC)
						.addStatement("super(new $L())", domainClassName)
						.build();
	}

	private static MethodSpec createTestMethod(EntityDefinition definition) {
		String interfaceName = interfaceName(definition, true);
		String methodName = interfaceName(definition, false);

		return methodBuilder(methodName)
						.addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
						.addModifiers(PUBLIC)
						.returns(void.class)
						.addStatement("test($L.TYPE)", interfaceName)
						.build();
	}

	// ========================================
	// DTO Generation
	// ========================================

	private void addDtoRecord(EntityDefinition definition, TypeSpec.Builder interfaceBuilder, Set<EntityType> dtos) {
		List<Attribute<?>> nonForeignKeyColumnAttributes = definition.attributes().get().stream()
						.filter(attribute -> excludeNonDtoForeignKeys(attribute, dtos))
						.filter(attribute -> noneForeignKeyColumn(attribute, definition))
						.collect(toList());

		interfaceBuilder.addType(dtoRecord(nonForeignKeyColumnAttributes));
		interfaceBuilder.addMethod(dtoFromEntityMethod(nonForeignKeyColumnAttributes, definition));
	}

	private TypeSpec dtoRecord(List<Attribute<?>> attributes) {
		TypeSpec.Builder dtoBuilder = TypeSpec.recordBuilder(DTO_CLASS_NAME)
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
			EntityDefinition referenced = referencedDefinition((ForeignKey) attribute);
			constructorBuilder.addParameter(ParameterSpec.builder(dtoName(referenced),
							underscoreToCamelCase(attribute.name().toLowerCase().replace(FK_SUFFIX, "").replace(FK_ALTERNATE_SUFFIX, ""))).build());
		}
	}

	private static MethodSpec entityFromDtoMethod(List<Attribute<?>> attributes) {
		return methodBuilder(ENTITY_METHOD_NAME)
						.addModifiers(PUBLIC)
						.returns(Entity.class)
						.addParameter(ParameterSpec.builder(Entities.class, ENTITIES_PARAM_NAME).build())
						.addCode(entityFromDtoMethodBody(attributes))
						.build();
	}

	private static String entityFromDtoMethodBody(Collection<Attribute<?>> attributes) {
		StringBuilder builder = new StringBuilder(RETURN + ENTITIES_PARAM_NAME + "." + ENTITY_METHOD_NAME + "(" + TYPE_FIELD_NAME + ")\n");
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
								.append(underscoreToCamelCase(attribute.name().toLowerCase().replace(FK_SUFFIX, "").replace(FK_ALTERNATE_SUFFIX, "")))
								.append("." + ENTITY_METHOD_NAME + "(" + ENTITIES_PARAM_NAME + ")")
								.append(")\n");
			}
		});

		return builder.append("\t.build();").toString();
	}

	private MethodSpec dtoFromEntityMethod(List<Attribute<?>> attributes, EntityDefinition definition) {
		return MethodSpec.methodBuilder(DTO_METHOD_NAME)
						.addModifiers(PUBLIC, STATIC)
						.returns(ClassName.get("", DTO_CLASS_NAME))
						.addParameter(Entity.class, interfaceName(definition, false))
						.addCode(dtoFromEntityMethodBody(attributes, interfaceName(definition, false)))
						.build();
	}

	private String dtoFromEntityMethodBody(List<Attribute<?>> attributes, String parameter) {
		List<String> arguments = new ArrayList<>();
		attributes.forEach(attribute -> {
			if (attribute instanceof Column<?>) {
				arguments.add(parameter + ".get(" + attribute.name().toUpperCase() + ")");
			}
			else if (attribute instanceof ForeignKey) {
				EntityDefinition referenced = referencedDefinition((ForeignKey) attribute);
				arguments.add(interfaceName(referenced, true)
								+ "." + DTO_METHOD_NAME + "(" + parameter + ".get(" + attribute.name().toUpperCase() + "))");
			}
		});

		return new StringBuilder(RETURN)
						.append(parameter)
						.append(" == null ? null :\n")
						.append("\tnew " + DTO_CLASS_NAME + "(")
						.append(String.join(",\n\t\t", arguments))
						.append(");").toString();
	}

	private static TypeName dtoName(EntityDefinition referenced) {
		return ClassName.get("", interfaceName(referenced, true) + "." + DTO_CLASS_NAME);
	}

	private EntityDefinition referencedDefinition(ForeignKey foreignKey) {
		EntityDefinition definition = definitionsByType.get(foreignKey.referencedType());
		if (definition == null) {
			throw new IllegalStateException("Referenced entity not found: " + foreignKey.referencedType());
		}
		return definition;
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

	// ========================================
	// Entity Definition Generation
	// ========================================

	private static MethodSpec createDefinitionMethod(EntityDefinition definition, boolean i18n) {
		String interfaceName = interfaceName(definition, true);
		CaptionStrategy captionStrategy = i18n ? new I18nCaptionStrategy() : new LiteralCaptionStrategy();
		StringBuilder builder = new StringBuilder()
						.append(RETURN).append(interfaceName).append(".TYPE.define(").append("\n")
						.append(String.join("," + "\n",
										createAttributes(definition.attributes().definitions(), definition, interfaceName, i18n)))
						.append(")");
		builder.append(captionStrategy.entityCaption(definition));
		builder.append(captionStrategy.entityDescription(definition));
		if (definition.readOnly()) {
			builder.append("\n").append(INDENT).append(".readOnly(true)");
		}
		builder.append("\n").append(INDENT).append(".build();");

		return methodBuilder(interfaceName(definition, false))
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
		CaptionStrategy captionStrategy = i18n ? new I18nCaptionStrategy() : new LiteralCaptionStrategy();
		AttributeDefinitionFormatter formatter = new AttributeDefinitionFormatter(interfaceName, captionStrategy);
		if (attributeDefinition instanceof ColumnDefinition) {
			ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
			ColumnContext context = new ColumnContext(
							definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()),
							definition.primaryKey().columns().size() > 1,
							definition.readOnly());

			return formatter.formatColumn(columnDefinition, context);
		}

		return formatter.formatForeignKey((ForeignKeyDefinition) attributeDefinition);
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

	private static String attributeTypePrefix(String valueClassName) {
		if ("byte[]".equals(valueClassName)) {
			return "byteArray";
		}

		return valueClassName.substring(0, 1).toLowerCase() + valueClassName.substring(1);
	}

	// ========================================
	// Utility Methods
	// ========================================

	private static String interfaceName(EntityDefinition definition, boolean uppercase) {
		// For views, derive interface name from caption and append "View" to avoid collisions
		if (definition.readOnly()) {
			// Convert "Country city" to "CountryCity" by capitalizing each word
			// Append "View" to distinguish from tables with the same name
			String name = Arrays.stream(definition.caption().trim().split(" "))
							.map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
							.collect(joining("", "", "View"));
			if (!uppercase) {
				name = name.substring(0, 1).toLowerCase() + name.substring(1);
			}

			return name;
		}

		// For tables, use the table name as before
		return interfaceName(definition.table(), uppercase);
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

	/**
	 * Removes blank lines between field declarations in generated interfaces.
	 * JavaPoet does not provide control over field spacing, so we use post-processing
	 * to achieve compact field declarations without empty lines between them.
	 */
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
		return "interface " + interfaceName(definition, true) + " ";
	}

	public static String implSearchString(EntityDefinition definition) {
		return "EntityDefinition " + interfaceName(definition, false) + "()";
	}

	public static String i18nSearchString(EntityDefinition definition) {
		return definition.type().name() + "=";
	}

	// ========================================
	// Dependency Analysis
	// ========================================

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

	private boolean identityGeneratorUsed() {
		return sortedDefinitions.stream()
						.flatMap(entityDefinition -> entityDefinition.columns().definitions().stream())
						.anyMatch(columnDefinition ->
										columnDefinition.generated() && columnDefinition.generator() instanceof Identity<?>);
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

	private static boolean generatedPrimaryKeyColumn(AttributeDefinition<?> attribute) {
		return attribute instanceof ColumnDefinition<?> &&
						((ColumnDefinition<?>) attribute).primaryKey() && ((ColumnDefinition<?>) attribute).generated();
	}

	private static boolean foreignKeyColumn(EntityDefinition definition, AttributeDefinition<?> attribute) {
		return attribute instanceof ColumnDefinition<?> &&
						definition.foreignKeys().foreignKeyColumn((Column<?>) attribute.attribute());
	}

	private static Collection<Attribute<?>> collectInvalidNames(List<EntityDefinition> definitions) {
		return unmodifiableSet(definitions.stream().flatMap(definition -> definition.attributes().get().stream()
										.filter(DomainSource::columnOrForeignKey)
										.filter(attribute -> !SourceVersion.isName(attribute.name())))
						.collect(toSet()));
	}

	private static boolean columnOrForeignKey(Attribute<?> attribute) {
		return attribute instanceof Column || attribute instanceof ForeignKey;
	}

	// ========================================
	// Inner Classes
	// ========================================

	/**
	 * Strategy for handling captions and descriptions in generated code.
	 * Used to support both literal (embedded) and i18n (resource-based) approaches.
	 */
	private interface CaptionStrategy {

		/**
		 * @return the EntityType initializer code
		 */
		String entityTypeInitializer(EntityDefinition definition, String interfaceName);

		/**
		 * @return entity caption code, or empty string if not needed
		 */
		String entityCaption(EntityDefinition definition);

		/**
		 * @return entity description code, or empty string if not needed
		 */
		String entityDescription(EntityDefinition definition);

		/**
		 * Adds attribute caption configuration if needed.
		 */
		void addAttributeCaption(CodeBlock.Builder builder, String caption);

		/**
		 * Adds attribute description configuration if needed.
		 */
		void addAttributeDescription(CodeBlock.Builder builder, String description);
	}

	/**
	 * Strategy that embeds captions and descriptions directly in generated code.
	 */
	private static final class LiteralCaptionStrategy implements CaptionStrategy {

		@Override
		public String entityTypeInitializer(EntityDefinition definition, String interfaceName) {
			return "DOMAIN.entityType(\"" + definition.table().toLowerCase() + "\")";
		}

		@Override
		public String entityCaption(EntityDefinition definition) {
			if (!nullOrEmpty(definition.caption())) {
				return "\n" + INDENT + ".caption(\"" + definition.caption() + "\")";
			}
			return "";
		}

		@Override
		public String entityDescription(EntityDefinition definition) {
			return definition.description()
							.map(description -> "\n" + INDENT + ".description(\"" + description + "\")")
							.orElse("");
		}

		@Override
		public void addAttributeCaption(CodeBlock.Builder builder, String caption) {
			builder.add("\n$L.caption($S)", TRIPLE_INDENT, caption);
		}

		@Override
		public void addAttributeDescription(CodeBlock.Builder builder, String description) {
			builder.add("\n$L.description($S)", TRIPLE_INDENT, description);
		}
	}

	/**
	 * Strategy for i18n mode - captions/descriptions come from resource bundles.
	 */
	private static final class I18nCaptionStrategy implements CaptionStrategy {

		@Override
		public String entityTypeInitializer(EntityDefinition definition, String interfaceName) {
			return "DOMAIN.entityType(\"" + definition.table().toLowerCase() + "\", " + interfaceName + ".class)";
		}

		@Override
		public String entityCaption(EntityDefinition definition) {
			return ""; // i18n mode - caption comes from resource bundle
		}

		@Override
		public String entityDescription(EntityDefinition definition) {
			return ""; // i18n mode - description comes from resource bundle
		}

		@Override
		public void addAttributeCaption(CodeBlock.Builder builder, String caption) {
			// i18n mode - caption comes from resource bundle
		}

		@Override
		public void addAttributeDescription(CodeBlock.Builder builder, String description) {
			// i18n mode - description comes from resource bundle
		}
	}

	/**
	 * Context information needed for formatting column definitions.
	 */
	private static final class ColumnContext {

		private final boolean foreignKeyColumn;
		private final boolean compositePrimaryKey;
		private final boolean readOnlyEntity;

		private ColumnContext(boolean foreignKeyColumn, boolean compositePrimaryKey, boolean readOnlyEntity) {
			this.foreignKeyColumn = foreignKeyColumn;
			this.compositePrimaryKey = compositePrimaryKey;
			this.readOnlyEntity = readOnlyEntity;
		}
	}

	/**
	 * Formats attribute definitions as source code using JavaPoet CodeBlock.
	 */
	private static final class AttributeDefinitionFormatter {

		private final String interfaceName;
		private final CaptionStrategy captionStrategy;

		private AttributeDefinitionFormatter(String interfaceName, CaptionStrategy captionStrategy) {
			this.interfaceName = interfaceName;
			this.captionStrategy = captionStrategy;
		}

		private String formatColumn(ColumnDefinition<?> column, ColumnContext context) {
			CodeBlock.Builder builder = CodeBlock.builder()
							.add("$L$L.$L.define()\n", DOUBLE_INDENT, interfaceName, column.name().toUpperCase())
							.add("$L.$L", TRIPLE_INDENT, definitionType(column, context.compositePrimaryKey));

			if (!context.foreignKeyColumn && !column.primaryKey()) {
				captionStrategy.addAttributeCaption(builder, column.caption());
			}
			if (!context.readOnlyEntity) {
				if (column.readOnly()) {
					builder.add("\n$L.readOnly(true)", TRIPLE_INDENT);
				}
				else {
					if (column.generated() && column.generator() instanceof Identity<?>) {
						builder.add("\n$L.generator(identity())", TRIPLE_INDENT);
					}
					if (!column.nullable() && !column.primaryKey()) {
						builder.add("\n$L.nullable(false)", TRIPLE_INDENT);
					}
					if (!column.insertable()) {
						builder.add("\n$L.insertable(false)", TRIPLE_INDENT);
					}
					else if (column.withDefault()) {
						builder.add("\n$L.withDefault(true)", TRIPLE_INDENT);
					}
					if (!column.updatable() && !column.primaryKey()) {
						builder.add("\n$L.updatable(false)", TRIPLE_INDENT);
					}
					if (column.attribute().type().isString() && column.maximumLength() != -1) {
						builder.add("\n$L.maximumLength($L)", TRIPLE_INDENT, column.maximumLength());
					}
				}
			}
			if (!column.selected()) {
				builder.add("\n$L.selected(false)", TRIPLE_INDENT);
			}
			if (column.attribute().type().isDecimal() && column.fractionDigits() >= 1) {
				builder.add("\n$L.fractionDigits($L)", TRIPLE_INDENT, column.fractionDigits());
			}
			column.description().ifPresent(description ->
							captionStrategy.addAttributeDescription(builder, description));

			return builder.build().toString();
		}

		private String formatForeignKey(ForeignKeyDefinition definition) {
			String foreignKeyName = definition.attribute().name().toUpperCase();
			CodeBlock.Builder builder = CodeBlock.builder()
							.add("$L$L.$L.define()\n", DOUBLE_INDENT, interfaceName, foreignKeyName)
							.add("$L.foreignKey()", TRIPLE_INDENT);

			captionStrategy.addAttributeCaption(builder, definition.caption());

			return builder.build().toString();
		}

		private static String definitionType(ColumnDefinition<?> column, boolean compositePrimaryKey) {
			if (column.primaryKey()) {
				return compositePrimaryKey ? "primaryKey(" + column.keyIndex() + ")" : "primaryKey()";
			}

			return "column()";
		}
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

		private String domainPackage = "none";
		private Set<EntityType> dtos = emptySet();
		private boolean i18n = false;
		private boolean test = false;

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
		public Builder test(boolean test) {
			this.test = test;
			return this;
		}

		@Override
		public DomainSource build() {
			return new DomainSource(this);
		}
	}
}
