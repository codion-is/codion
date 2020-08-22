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

import java.util.List;

import static is.codion.common.Util.nullOrEmpty;

final class DomainToString {

  static String toString(final EntityDefinition definition) {
    final StringBuilder builder = new StringBuilder();
    final String interfaceName = getInterfaceName(definition.getTableName(), true);
    builder.append("public interface ").append(interfaceName).append(" {").append(Util.LINE_SEPARATOR);
    builder.append("  ").append("EntityType<Entity> TYPE = ").append("DOMAIN.entityType(\"")
            .append(definition.getTableName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
    final List<Property<?>> properties = definition.getProperties();
    properties.forEach(property -> appendAttribute(builder, property));
    builder.append("}").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);
    builder.append("void ").append(getInterfaceName(definition.getTableName(), false)).append("() {").append(Util.LINE_SEPARATOR);
    builder.append("  define(").append(interfaceName).append(".TYPE").append(",").append(Util.LINE_SEPARATOR);
    properties.forEach(property -> appendProperty(interfaceName, property, definition, builder));
    builder.replace(builder.length() - 2, builder.length(), "");
    builder.append(Util.LINE_SEPARATOR).append("  );").append(Util.LINE_SEPARATOR);

    builder.append("}").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);

    return builder.toString();
  }

  private static void appendAttribute(final StringBuilder builder, final Property<?> property) {
    if (property instanceof ColumnProperty) {
      final ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
      final String typeClassName = columnProperty.getAttribute().getTypeClass().getSimpleName();
      builder.append("  ").append("Attribute<").append(typeClassName).append("> ")
              .append(columnProperty.getColumnName().toUpperCase()).append(" = TYPE.").append(getAttributeTypePrefix(typeClassName))
              .append("Attribute(\"").append(columnProperty.getColumnName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
    }
    else if (property instanceof ForeignKeyProperty) {
      builder.append("  ").append("Attribute<Entity> ")
              .append(property.getAttribute().getName().toUpperCase()).append(" = TYPE.entityAttribute(\"")
              .append(property.getAttribute().getName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
    }
  }

  private static void appendProperty(final String interfaceName, final Property<?> property,
                                     final EntityDefinition definition, final StringBuilder builder) {
    if (property instanceof ColumnProperty) {
      builder.append("  ").append(getColumnPropertyDefinition(interfaceName,
              (ColumnProperty<?>) property, definition))
              .append(",").append(Util.LINE_SEPARATOR);
    }
    else if (property instanceof ForeignKeyProperty) {
      builder.append("  ").append(getForeignKeyPropertyDefinition(interfaceName, (ForeignKeyProperty) property))
              .append(",").append(Util.LINE_SEPARATOR);
    }
  }

  private static String getForeignKeyPropertyDefinition(final String interfaceName, final ForeignKeyProperty property) {
    final StringBuilder builder = new StringBuilder();
    final String foreignKeyAttribute = property.getAttribute().getName().toUpperCase();
    final String caption = property.getCaption();
    builder.append("        foreignKeyProperty(").append(interfaceName).append(".").append(foreignKeyAttribute)
            .append(", \"").append(caption).append("\")").append(Util.LINE_SEPARATOR);
    property.getReferences().forEach(reference ->
            builder.append("                .reference(").append(interfaceName).append(".")
                    .append(reference.getAttribute().getName().toUpperCase()).append(", ")
                    .append(getInterfaceName(reference.getReferencedAttribute().getEntityType().getName(), true)).append(".")
                    .append(reference.getReferencedAttribute().getName().toUpperCase()).append(")"));

    return builder.toString();
  }

  private static String getColumnPropertyDefinition(final String interfaceName, final ColumnProperty<?> property,
                                                    final EntityDefinition definition) {
    final StringBuilder builder = new StringBuilder(getPropertyType(property.getAttribute())).append(interfaceName)
            .append(".").append(property.getColumnName().toUpperCase());
    if (!definition.isForeignKeyAttribute(property.getAttribute()) && !property.isPrimaryKeyColumn()) {
      builder.append(", ").append("\"").append(property.getCaption()).append("\")");
    }
    else {
      builder.append(")");
    }
    if (property.getAttribute().isByteArray()) {
      builder.append(Util.LINE_SEPARATOR).append("                .eagerlyLoaded(").append(((BlobProperty) property).isEagerlyLoaded()).append(")");
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
    if (typeClassName.equals("byte[]")) {
      return "byteArray";
    }

    return typeClassName.substring(0, 1).toLowerCase() + typeClassName.substring(1);
  }

  private static String getPropertyType(final Attribute<?> attribute) {
    return attribute.isByteArray() ? "        blobProperty(" : "        columnProperty(";
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
