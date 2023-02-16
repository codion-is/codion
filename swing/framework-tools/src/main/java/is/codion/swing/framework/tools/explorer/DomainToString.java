/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.Text;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.List;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.Separators.LINE_SEPARATOR;
import static java.util.stream.Collectors.toList;

final class DomainToString {

  private DomainToString() {}

  static String toString(EntityDefinition definition) {
    StringBuilder builder = new StringBuilder();
    String interfaceName = interfaceName(definition.tableName(), true);
    builder.append("public interface ").append(interfaceName).append(" {").append(LINE_SEPARATOR);
    builder.append("  ").append("EntityType TYPE = ").append("DOMAIN.entityType(\"")
            .append(definition.tableName().toLowerCase()).append("\");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    List<Property<?>> columnProperties = definition.properties().stream()
            .filter(ColumnProperty.class::isInstance)
            .collect(toList());
    columnProperties.forEach(property -> appendAttribute(builder, property));
    List<Property<?>> foreignKeyProperties = definition.properties().stream()
            .filter(ForeignKeyProperty.class::isInstance)
            .collect(toList());
    if (!foreignKeyProperties.isEmpty()) {
      builder.append(LINE_SEPARATOR);
      foreignKeyProperties.forEach(property -> appendAttribute(builder, property));
    }
    builder.append("}").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    builder.append("void ").append(interfaceName(definition.tableName(), false)).append("() {").append(LINE_SEPARATOR);
    builder.append("  add(definition(").append(LINE_SEPARATOR);
    builder.append(String.join("," + LINE_SEPARATOR, propertyStrings(definition.properties(), interfaceName, definition)));
    builder.append(LINE_SEPARATOR).append("  ));").append(LINE_SEPARATOR);
    builder.append("}");

    return builder.toString();
  }

  private static void appendAttribute(StringBuilder builder, Property<?> property) {
    if (property instanceof ColumnProperty) {
      ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
      String valueClassName = columnProperty.attribute().valueClass().getSimpleName();
      builder.append("  ").append("Attribute<").append(valueClassName).append("> ")
              .append(columnProperty.columnName().toUpperCase()).append(" = TYPE.").append(attributeTypePrefix(valueClassName))
              .append("Attribute(\"").append(columnProperty.columnName().toLowerCase()).append("\");").append(LINE_SEPARATOR);
    }
    else if (property instanceof ForeignKeyProperty) {
      ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) property;
      List<String> references = new ArrayList<>();
      foreignKeyProperty.references().forEach(reference -> {
        StringBuilder referenceBuilder = new StringBuilder();
        referenceBuilder.append(reference.attribute().name().toUpperCase()).append(", ")
                .append(interfaceName(reference.referencedAttribute().entityType().name(), true))
                .append(".").append(reference.referencedAttribute().name().toUpperCase());
        references.add(referenceBuilder.toString());
      });

      //todo wrap references if more than four
      builder.append("  ").append("ForeignKey ")
              .append(property.attribute().name().toUpperCase()).append(" = TYPE.foreignKey(\"")
              .append(property.attribute().name().toLowerCase()).append("\", " + String.join("," + LINE_SEPARATOR, references) + ");").append(LINE_SEPARATOR);
    }
  }

  private static List<String> propertyStrings(List<Property<?>> properties, String interfaceName,
                                              EntityDefinition definition) {
    List<String> strings = new ArrayList<>();
    properties.forEach(property -> {
      if (property instanceof ColumnProperty) {
        strings.add(columnProperty(interfaceName, (ColumnProperty<?>) property,
                definition.isForeignKeyAttribute(property.attribute()), definition.primaryKeyAttributes().size() > 1));
      }
      else if (property instanceof ForeignKeyProperty) {
        strings.add(foreignKeyProperty(interfaceName, (ForeignKeyProperty) property));
      }
    });

    return strings;
  }

  private static String foreignKeyProperty(String interfaceName, ForeignKeyProperty property) {
    StringBuilder builder = new StringBuilder();
    String foreignKey = property.attribute().name().toUpperCase();
    builder.append("          foreignKeyProperty(").append(interfaceName).append(".").append(foreignKey)
            .append(", \"").append(property.caption()).append("\")");

    return builder.toString();
  }

  private static String columnProperty(String interfaceName, ColumnProperty<?> property,
                                       boolean isForeignKey, boolean compositePrimaryKey) {
    StringBuilder builder = new StringBuilder(propertyType(property.attribute(),
            property.isPrimaryKeyColumn() && !compositePrimaryKey))
            .append(interfaceName).append(".").append(property.columnName().toUpperCase());
    if (!isForeignKey && !property.isPrimaryKeyColumn()) {
      builder.append(", ").append("\"").append(property.caption()).append("\")");
    }
    else {
      builder.append(")");
    }
    if (property instanceof BlobProperty && ((BlobProperty) property).isEagerlyLoaded()) {
      builder.append(LINE_SEPARATOR).append("                .eagerlyLoaded()");
    }
    if (property.isPrimaryKeyColumn() && compositePrimaryKey) {
      builder.append(LINE_SEPARATOR).append("                .primaryKeyIndex(")
              .append(property.primaryKeyIndex()).append(")");
    }
    if (property.columnHasDefaultValue()) {
      builder.append(LINE_SEPARATOR).append("                .columnHasDefaultValue(true)");
    }
    if (!property.isNullable() && !property.isPrimaryKeyColumn()) {
      builder.append(LINE_SEPARATOR).append("                .nullable(false)");
    }
    if (String.class.equals(property.attribute().valueClass())) {
      builder.append(LINE_SEPARATOR).append("                .maximumLength(")
              .append(property.maximumLength()).append(")");
    }
    if (Double.class.equals(property.attribute().valueClass()) && property.maximumFractionDigits() >= 1) {
      builder.append(LINE_SEPARATOR).append("                .maximumFractionDigits(")
              .append(property.maximumFractionDigits()).append(")");
    }
    if (!nullOrEmpty(property.description())) {
      builder.append(LINE_SEPARATOR).append("                .description(")
              .append("\"").append(property.description()).append("\")");
    }

    return builder.toString();
  }

  private static String attributeTypePrefix(String valueClassName) {
    if ("byte[]".equals(valueClassName)) {
      return "byteArray";
    }

    return valueClassName.substring(0, 1).toLowerCase() + valueClassName.substring(1);
  }

  private static String propertyType(Attribute<?> attribute, boolean primaryKeyProperty) {
    if (attribute.isByteArray()) {
      return "          blobProperty(";
    }

    return primaryKeyProperty ? "          primaryKeyProperty(" : "          columnProperty(";
  }

  private static String interfaceName(String tableName, boolean uppercase) {
    String name = tableName.toLowerCase();
    if (name.contains(".")) {
      name = name.substring(name.lastIndexOf('.') + 1);
    }
    name = Text.underscoreToCamelCase(name);
    if (uppercase) {
      name = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    return name;
  }
}
