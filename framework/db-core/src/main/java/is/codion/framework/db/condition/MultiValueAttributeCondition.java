/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class MultiValueAttributeCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  private final List<T> values;

  private boolean caseSensitive = true;

  MultiValueAttributeCondition(Attribute<T> attribute, Collection<? extends T> values, Operator operator) {
    super(attribute, operator);
    this.values = unmodifiableList(new ArrayList<>(values));
    for (int i = 0; i < this.values.size(); i++) {
      requireNonNull(this.values.get(i), "Equal condition values may not be null");
    }
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
  public AttributeCondition<String> caseSensitive(boolean caseSensitive) {
    if (!getAttribute().isString()) {
      throw new IllegalStateException("Attribute " + getAttribute() + " is not a String attribute");
    }
    this.caseSensitive = caseSensitive;

    return (AttributeCondition<String>) this;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof MultiValueAttributeCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    MultiValueAttributeCondition<?> that = (MultiValueAttributeCondition<?>) object;
    return caseSensitive == that.caseSensitive &&
            values.equals(that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), values, caseSensitive);
  }

  @Override
  protected String getConditionString(String columnExpression) {
    boolean negated = getOperator() == Operator.NOT_EQUAL;
    String identifier = columnExpression;
    if (values.isEmpty()) {
      return identifier + (negated ? " is not null" : " is null");
    }
    if (getAttribute().isString() && !caseSensitive) {
      identifier = "upper(" + identifier + ")";
    }

    String valuePlaceholder = getAttribute().isString() && !caseSensitive ? "upper(?)" : "?";
    if (values.size() > 1) {
      return getInList(identifier, valuePlaceholder, values.size(), negated);
    }
    if (getAttribute().isString() && containsWildcards((String) values.get(0))) {
      return identifier + (negated ? " not like " : " like ") + valuePlaceholder;
    }

    return identifier + (negated ? " <> " : " = ") + valuePlaceholder;
  }

  private static String getInList(String columnIdentifier, String valuePlaceholder, int valueCount, boolean negated) {
    boolean exceedsLimit = valueCount > IN_CLAUSE_LIMIT;
    StringBuilder stringBuilder = new StringBuilder(exceedsLimit ? "(" : "").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
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

  private static boolean containsWildcards(String value) {
    return value.contains("%") || value.contains("_");
  }
}
