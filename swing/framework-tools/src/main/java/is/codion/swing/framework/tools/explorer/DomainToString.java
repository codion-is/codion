/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

import static is.codion.common.Separators.LINE_SEPARATOR;
import static is.codion.common.Util.nullOrEmpty;
import static java.util.stream.Collectors.toList;

final class DomainToString {

  private DomainToString() {}

  static String toString(EntityDefinition definition) {
    StringBuilder builder = new StringBuilder();
    String interfaceName = getInterfaceName(definition.getTableName(), true);
    builder.append("public interface ").append(interfaceName).append(" {").append(LINE_SEPARATOR);
    builder.append("  ").append("EntityType TYPE = ").append("DOMAIN.entityType(\"")
            .append(definition.getTableName().toLowerCase()).append("\");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    List<Property<?>> columnProperties = definition.getProperties().stream()
            .filter(ColumnProperty.class::isInstance)
            .collect(toList());
    columnProperties.forEach(property -> appendAttribute(builder, property));
    List<Property<?>> foreignKeyProperties = definition.getProperties().stream()
            .filter(ForeignKeyProperty.class::isInstance)
            .collect(toList());
    if (!foreignKeyProperties.isEmpty()) {
      builder.append(LINE_SEPARATOR);
      foreignKeyProperties.forEach(property -> appendAttribute(builder, property));
    }
    builder.append("}").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    builder.append("void ").append(getInterfaceName(definition.getTableName(), false)).append("() {").append(LINE_SEPARATOR);
    builder.append("  add(definition(").append(LINE_SEPARATOR);
    builder.append(String.join("," + LINE_SEPARATOR, getPropertyStrings(definition.getProperties(), interfaceName, definition)));
    builder.append(LINE_SEPARATOR).append("  ));").append(LINE_SEPARATOR);
    builder.append("}").append(LINE_SEPARATOR).append(LINE_SEPARATOR);

    return builder.toString();
  }

  private static void appendAttribute(StringBuilder builder, Property<?> property) {
    if (property instanceof ColumnProperty) {
      ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
      String typeClassName = columnProperty.getAttribute().getTypeClass().getSimpleName();
      builder.append("  ").append("Attribute<").append(typeClassName).append("> ")
              .append(columnProperty.getColumnName().toUpperCase()).append(" = TYPE.").append(getAttributeTypePrefix(typeClassName))
              .append("Attribute(\"").append(columnProperty.getColumnName().toLowerCase()).append("\");").append(LINE_SEPARATOR);
    }
    else if (property instanceof ForeignKeyProperty) {
      ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) property;
      List<String> references = new ArrayList<>();
      foreignKeyProperty.getReferences().forEach(reference -> {
        StringBuilder referenceBuilder = new StringBuilder();
        referenceBuilder.append(reference.getAttribute().getName().toUpperCase()).append(", ")
                .append(getInterfaceName(reference.getReferencedAttribute().getEntityType().getName(), true))
                .append(".").append(reference.getReferencedAttribute().getName().toUpperCase());
        references.add(referenceBuilder.toString());
      });

      //todo wrap references if more than four
      builder.append("  ").append("ForeignKey ")
              .append(property.getAttribute().getName().toUpperCase()).append(" = TYPE.foreignKey(\"")
              .append(property.getAttribute().getName().toLowerCase()).append("\", " + String.join("," + LINE_SEPARATOR, references) + ");").append(LINE_SEPARATOR);
    }
  }

  private static List<String> getPropertyStrings(List<Property<?>> properties, String interfaceName,
                                                 EntityDefinition definition) {
    List<String> strings = new ArrayList<>();
    properties.forEach(property -> {
      if (property instanceof ColumnProperty) {
        strings.add(getColumnProperty(interfaceName, (ColumnProperty<?>) property,
                definition.isForeignKeyAttribute(property.getAttribute()), definition.getPrimaryKeyAttributes().size() > 1));
      }
      else if (property instanceof ForeignKeyProperty) {
        strings.add(getForeignKeyProperty(interfaceName, (ForeignKeyProperty) property));
      }
    });

    return strings;
  }

  private static String getForeignKeyProperty(String interfaceName, ForeignKeyProperty property) {
    StringBuilder builder = new StringBuilder();
    String foreignKey = property.getAttribute().getName().toUpperCase();
    builder.append("          foreignKeyProperty(").append(interfaceName).append(".").append(foreignKey)
            .append(", \"").append(property.getCaption()).append("\")");

    return builder.toString();
  }

  private static String getColumnProperty(String interfaceName, ColumnProperty<?> property,
                                          boolean isForeignKey, boolean compositePrimaryKey) {
    StringBuilder builder = new StringBuilder(getPropertyType(property.getAttribute(),
            property.isPrimaryKeyColumn() && !compositePrimaryKey))
            .append(interfaceName).append(".").append(property.getColumnName().toUpperCase());
    if (!isForeignKey && !property.isPrimaryKeyColumn()) {
      builder.append(", ").append("\"").append(property.getCaption()).append("\")");
    }
    else {
      builder.append(")");
    }
    if (property instanceof BlobProperty && ((BlobProperty) property).isEagerlyLoaded()) {
      builder.append(LINE_SEPARATOR).append("                .eagerlyLoaded()");
    }
    if (property.isPrimaryKeyColumn() && compositePrimaryKey) {
      builder.append(LINE_SEPARATOR).append("                .primaryKeyIndex(")
              .append(property.getPrimaryKeyIndex()).append(")");
    }
    if (property.columnHasDefaultValue()) {
      builder.append(LINE_SEPARATOR).append("                .columnHasDefaultValue(true)");
    }
    if (!property.isNullable() && !property.isPrimaryKeyColumn()) {
      builder.append(LINE_SEPARATOR).append("                .nullable(false)");
    }
    if (String.class.equals(property.getAttribute().getTypeClass())) {
      builder.append(LINE_SEPARATOR).append("                .maximumLength(")
              .append(property.getMaximumLength()).append(")");
    }
    if (Double.class.equals(property.getAttribute().getTypeClass()) && property.getMaximumFractionDigits() >= 1) {
      builder.append(LINE_SEPARATOR).append("                .maximumFractionDigits(")
              .append(property.getMaximumFractionDigits()).append(")");
    }
    if (!nullOrEmpty(property.getDescription())) {
      builder.append(LINE_SEPARATOR).append("                .description(")
              .append("\"").append(property.getDescription()).append("\")");
    }

    return builder.toString();
  }

  private static String getAttributeTypePrefix(String typeClassName) {
    if ("byte[]".equals(typeClassName)) {
      return "byteArray";
    }

    return typeClassName.substring(0, 1).toLowerCase() + typeClassName.substring(1);
  }

  private static String getPropertyType(Attribute<?> attribute, boolean primaryKeyProperty) {
    if (attribute.isByteArray()) {
      return "          blobProperty(";
    }

    return primaryKeyProperty ? "          primaryKeyProperty(" : "          columnProperty(";
  }

  private static String getInterfaceName(String tableName, boolean uppercase) {
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
