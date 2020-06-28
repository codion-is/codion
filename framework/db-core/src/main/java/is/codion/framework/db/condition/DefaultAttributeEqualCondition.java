/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeEqualCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  private final List<Object> values;
  private final boolean negated;

  private boolean caseSensitive = true;

  DefaultAttributeEqualCondition(final Attribute<T> attribute, final Collection<? extends Object> conditionValues) {
    this(attribute, conditionValues, false);
  }

  DefaultAttributeEqualCondition(final Attribute<T> attribute, final Collection<? extends Object> conditionValues, final boolean negated) {
    super(attribute);
    this.values = new ArrayList<>(requireNonNull(conditionValues));
    this.negated = negated;
    //replace Entity with Entity.Key
    for (int i = 0; i < values.size(); i++) {
      final Object value = values.get(i);
      requireNonNull(value, "value");
      if (value instanceof Entity) {
        values.set(i, ((Entity) value).getKey());
      }
      else {//assume it's all or nothing
        break;
      }
    }
  }

  @Override
  public Operator getOperator() {
    return negated ? Operator.NOT_EQUALS : Operator.EQUALS;
  }

  @Override
  public List<?> getValues() {
    return values;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    if (values.size() == 1) {
      return singletonList(getAttribute());
    }

    return Collections.nCopies(values.size(), getAttribute());
  }

  @Override
  public String getWhereClause(final EntityDefinition definition) {
    final ColumnProperty<?> property = definition.getColumnProperty(getAttribute());
    String columnIdentifier = getColumnIdentifier(property);
    if (values.isEmpty()) {
      return columnIdentifier + (negated ? " is not null" : " is null");
    }
    if (property.getAttribute().isString() && !caseSensitive) {
      columnIdentifier = "upper(" + columnIdentifier + ")";
    }

    return getLikeCondition(property, columnIdentifier);
  }

  @Override
  public AttributeCondition<String> setCaseSensitive(final boolean caseSensitive) {
    if (!getAttribute().isString()) {
      throw new IllegalStateException("Attribute " + getAttribute() + " is not a String attribute");
    }
    this.caseSensitive = caseSensitive;

    return (AttributeCondition<String>) this;
  }

  private String getLikeCondition(final ColumnProperty<?> property, final String columnIdentifier) {
    final String valuePlaceholder = property.getAttribute().isString() && !caseSensitive ? "upper(?)" : "?";
    if (values.size() > 1) {
      return getInList(columnIdentifier, valuePlaceholder, values.size(), negated);
    }
    if (property.getAttribute().isString() && containsWildcards((String) values.get(0))) {
      return columnIdentifier + (negated ? " not like " : " like ") + valuePlaceholder;
    }

    return columnIdentifier + (negated ? " <> " : " = ") + valuePlaceholder;
  }

  private static String getInList(final String columnIdentifier, final String valuePlaceholder, final int valueCount, final boolean negated) {
    final boolean exceedsLimit = valueCount > IN_CLAUSE_LIMIT;
    final StringBuilder stringBuilder = new StringBuilder(exceedsLimit ? "(" : "").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
    int cnt = 1;
    for (int i = 0; i < valueCount; i++) {
      stringBuilder.append(valuePlaceholder);
      if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
        stringBuilder.append(negated ? ") and " : ") or ").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
        cnt = 1;
      }
      else if (i < valueCount - 1) {
        stringBuilder.append(", ");
      }
    }
    stringBuilder.append(")").append(exceedsLimit ? ")" : "");

    return stringBuilder.toString();
  }

  private static boolean containsWildcards(final String value) {
    return value.contains("%") || value.contains("_");
  }
}
