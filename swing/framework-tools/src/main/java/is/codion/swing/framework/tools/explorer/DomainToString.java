/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.Text;
import is.codion.common.Util;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.List;

import static is.codion.common.Util.nullOrEmpty;

final class DomainToString {

  private DomainToString() {}

  static String toString(final EntityDefinition definition) {
    final StringBuilder builder = new StringBuilder();
    final String interfaceName = getInterfaceName(definition.getTableName(), true);
    builder.append("public interface ").append(interfaceName).append(" {").append(Util.LINE_SEPARATOR);
    builder.append("  ").append("EntityType<Entity> TYPE = ").append("DOMAIN.entityType(\"")
            .append(definition.getTableName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
    definition.getProperties().forEach(property -> appendAttribute(builder, property, interfaceName));
    builder.append("}").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);
    builder.append("void ").append(getInterfaceName(definition.getTableName(), false)).append("() {").append(Util.LINE_SEPARATOR);
    builder.append("  define(").append(interfaceName).append(".TYPE").append(",").append(Util.LINE_SEPARATOR);
    builder.append(String.join("," + Util.LINE_SEPARATOR, getPropertyStrings(definition.getProperties(), interfaceName, definition)));
    builder.append(Util.LINE_SEPARATOR).append("  );").append(Util.LINE_SEPARATOR);
    builder.append("}").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);

    return builder.toString();
  }

  private static void appendAttribute(final StringBuilder builder, final Property<?> property, final String interfaceName) {
    if (property instanceof ColumnProperty) {
      final ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
      final String typeClassName = columnProperty.getAttribute().getTypeClass().getSimpleName();
      builder.append("  ").append("Attribute<").append(typeClassName).append("> ")
              .append(columnProperty.getColumnName().toUpperCase()).append(" = TYPE.").append(getAttributeTypePrefix(typeClassName))
              .append("Attribute(\"").append(columnProperty.getColumnName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
    }
    else if (property instanceof ForeignKeyProperty) {
      final ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) property;
      final List<String> references = new ArrayList<>();
      foreignKeyProperty.getReferences().forEach(reference -> {
        final StringBuilder referenceBuilder = new StringBuilder();
        referenceBuilder.append(interfaceName).append(".")
                .append(reference.getAttribute().getName().toUpperCase()).append(", ")
                .append(getInterfaceName(reference.getReferencedAttribute().getEntityType().getName(), true))
                .append(".").append(reference.getReferencedAttribute().getName().toUpperCase());
        references.add(referenceBuilder.toString());
      });

      builder.append("  ").append("ForeignKeyAttribute ")
              .append(property.getAttribute().getName().toUpperCase()).append(" = TYPE.foreignKey(\"")
              .append(property.getAttribute().getName().toLowerCase()).append("\", " + String.join(Util.LINE_SEPARATOR, references) + ");").append(Util.LINE_SEPARATOR);
    }
  }

  private static List<String> getPropertyStrings(final List<Property<?>> properties, final String interfaceName,
                                                 final EntityDefinition definition) {
    final List<String> strings = new ArrayList<>();
    properties.forEach(property -> {
      if (property instanceof ColumnProperty) {
        strings.add(getColumnProperty(interfaceName, (ColumnProperty<?>) property,
                definition.isForeignKeyAttribute(property.getAttribute())));
      }
      else if (property instanceof ForeignKeyProperty) {
        strings.add(getForeignKeyProperty(interfaceName, (ForeignKeyProperty) property));
      }
    });

    return strings;
  }

  private static String getForeignKeyProperty(final String interfaceName, final ForeignKeyProperty property) {
    final StringBuilder builder = new StringBuilder();
    final String foreignKeyAttribute = property.getAttribute().getName().toUpperCase();
    builder.append("          foreignKeyProperty(").append(interfaceName).append(".").append(foreignKeyAttribute)
            .append(", \"").append(property.getCaption()).append("\")");

    return builder.toString();
  }

  private static String getColumnProperty(final String interfaceName, final ColumnProperty<?> property,
                                          final boolean isForeignKey) {
    final StringBuilder builder = new StringBuilder(getPropertyType(property.getAttribute()))
            .append(interfaceName).append(".").append(property.getColumnName().toUpperCase());
    if (!isForeignKey && !property.isPrimaryKeyColumn()) {
      builder.append(", ").append("\"").append(property.getCaption()).append("\")");
    }
    else {
      builder.append(")");
    }
    if (property.getAttribute().isByteArray()) {
      builder.append(Util.LINE_SEPARATOR).append("                .eagerlyLoaded(").append(((BlobProperty) property)
              .isEagerlyLoaded()).append(")");
    }
    if (property.isPrimaryKeyColumn()) {
      builder.append(Util.LINE_SEPARATOR).append("                .primaryKeyIndex(")
              .append(property.getPrimaryKeyIndex()).append(")");
    }
    if (property.columnHasDefaultValue()) {
      builder.append(Util.LINE_SEPARATOR).append("                .columnHasDefaultValue(true)");
    }
    if (!property.isNullable() && !property.isPrimaryKeyColumn()) {
      builder.append(Util.LINE_SEPARATOR).append("                .nullable(false)");
    }
    if (String.class.equals(property.getAttribute().getTypeClass())) {
      builder.append(Util.LINE_SEPARATOR).append("                .maximumLength(")
              .append(property.getMaximumLength()).append(")");
    }
    if (Double.class.equals(property.getAttribute().getTypeClass()) && property.getMaximumFractionDigits() >= 1) {
      builder.append(Util.LINE_SEPARATOR).append("                .maximumFractionDigits(")
              .append(property.getMaximumFractionDigits()).append(")");
    }
    if (!nullOrEmpty(property.getDescription())) {
      builder.append(Util.LINE_SEPARATOR).append("                .description(")
              .append(property.getDescription()).append(")");
    }

    return builder.toString();
  }

  private static String getAttributeTypePrefix(final String typeClassName) {
    if ("byte[]".equals(typeClassName)) {
      return "byteArray";
    }

    return typeClassName.substring(0, 1).toLowerCase() + typeClassName.substring(1);
  }

  private static String getPropertyType(final Attribute<?> attribute) {
    return attribute.isByteArray() ? "          blobProperty(" : "          columnProperty(";
  }

  private static String getInterfaceName(final String tableName, final boolean uppercase) {
    String name = tableName;
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
