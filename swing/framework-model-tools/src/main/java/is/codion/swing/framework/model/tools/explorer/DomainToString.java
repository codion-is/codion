/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.explorer;

import is.codion.common.Text;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import java.util.ArrayList;
import java.util.List;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.Separators.LINE_SEPARATOR;
import static java.util.stream.Collectors.toList;

final class DomainToString {

  private static final String INDENT = "\t";
  private static final String DOUBLE_INDENT = INDENT + INDENT;
  private static final String TRIPLE_INDENT = INDENT + INDENT + INDENT;

  private DomainToString() {}

  static String toString(EntityDefinition definition) {
    StringBuilder builder = new StringBuilder();
    String interfaceName = interfaceName(definition.tableName(), true);
    builder.append("public interface ").append(interfaceName).append(" {").append(LINE_SEPARATOR);
    builder.append(INDENT).append("EntityType TYPE = ").append("DOMAIN.entityType(\"")
            .append(definition.tableName().toLowerCase()).append("\");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    List<AttributeDefinition<?>> columnDefinitions = definition.attributes().definitions().stream()
            .filter(ColumnDefinition.class::isInstance)
            .collect(toList());
    columnDefinitions.forEach(columnDefinition -> appendAttribute(builder, columnDefinition));
    List<AttributeDefinition<?>> foreignKeyDefinitions = definition.attributes().definitions().stream()
            .filter(ForeignKeyDefinition.class::isInstance)
            .collect(toList());
    if (!foreignKeyDefinitions.isEmpty()) {
      builder.append(LINE_SEPARATOR);
      foreignKeyDefinitions.forEach(foreignKeyDefinition -> appendAttribute(builder, foreignKeyDefinition));
    }
    builder.append("}").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    builder.append("void ").append(interfaceName(definition.tableName(), false)).append("() {").append(LINE_SEPARATOR);
    builder.append(INDENT).append("add(").append(interfaceName).append(".TYPE.define(").append(LINE_SEPARATOR);
    builder.append(String.join("," + LINE_SEPARATOR, attributeStrings(definition.attributes().definitions(), interfaceName, definition))).append(")");
    if (definition.description() != null) {
      builder.append(LINE_SEPARATOR).append(DOUBLE_INDENT).append(".description(\"").append(definition.description()).append("\")");
    }
    builder.append(");");
    builder.append(LINE_SEPARATOR);
    builder.append("}");

    return builder.toString();
  }

  private static void appendAttribute(StringBuilder builder, AttributeDefinition<?> attributeDefinition) {
    if (attributeDefinition instanceof ColumnDefinition) {
      ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
      String valueClassName = columnDefinition.attribute().type().valueClass().getSimpleName();
      builder.append(INDENT).append("Column<").append(valueClassName).append("> ")
              .append(columnDefinition.name().toUpperCase()).append(" = TYPE.").append(attributeTypePrefix(valueClassName))
              .append("Column(\"").append(columnDefinition.name().toLowerCase()).append("\");").append(LINE_SEPARATOR);
    }
    else if (attributeDefinition instanceof ForeignKeyDefinition) {
      ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
      List<String> references = new ArrayList<>();
      foreignKeyDefinition.references().forEach(reference -> {
        StringBuilder referenceBuilder = new StringBuilder();
        referenceBuilder.append(reference.column().name().toUpperCase()).append(", ")
                .append(interfaceName(reference.foreign().entityType().name(), true))
                .append(".").append(reference.foreign().name().toUpperCase());
        references.add(referenceBuilder.toString());
      });

      //todo wrap references if more than four
      builder.append(INDENT).append("ForeignKey ")
              .append(attributeDefinition.attribute().name().toUpperCase()).append(" = TYPE.foreignKey(\"")
              .append(attributeDefinition.attribute().name().toLowerCase()).append("\", " + String.join("," + LINE_SEPARATOR, references) + ");").append(LINE_SEPARATOR);
    }
  }

  private static List<String> attributeStrings(List<AttributeDefinition<?>> attributeDefinitions, String interfaceName,
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
    if (column.attribute().type().isByteArray()) {
      return "blobColumn()";
    }
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
    name = Text.underscoreToCamelCase(name);
    if (uppercase) {
      name = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    return name;
  }
}
