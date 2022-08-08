/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  private final boolean caseSensitive;

  MultiValueAttributeCondition(Attribute<T> attribute, Collection<? extends T> values, Operator operator) {
    this(attribute, values, operator, true);
  }

  MultiValueAttributeCondition(Attribute<T> attribute, Collection<? extends T> values, Operator operator,
                               boolean caseSensitive) {
    super(attribute, operator);
    if (!caseSensitive && !attribute.isString()) {
      throw new IllegalStateException("Case sensitivity only applies to String based attributes: " + attribute);
    }
    this.caseSensitive = caseSensitive;
    this.values = unmodifiableList(new ArrayList<>(values));
    for (int i = 0; i < this.values.size(); i++) {
      requireNonNull(this.values.get(i), "Equal condition values may not be null");
    }
  }

  @Override
  public List<?> values() {
    return values;
  }

  @Override
  public List<Attribute<?>> attributes() {
    if (values.size() == 1) {
      return singletonList(attribute());
    }

    return Collections.nCopies(values.size(), attribute());
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
  protected String toString(String columnExpression) {
    boolean notEqual = operator() == Operator.NOT_EQUAL;
    String identifier = columnExpression;
    if (values.isEmpty()) {
      return identifier + (notEqual ? " is not null" : " is null");
    }
    if (attribute().isString() && !caseSensitive) {
      identifier = "upper(" + identifier + ")";
    }

    String valuePlaceholder = attribute().isString() && !caseSensitive ? "upper(?)" : "?";
    if (values.size() > 1) {
      return createInList(identifier, valuePlaceholder, values.size(), notEqual);
    }
    if (attribute().isString() && containsWildcards((String) values.get(0))) {
      return identifier + (notEqual ? " not like " : " like ") + valuePlaceholder;
    }

    return identifier + (notEqual ? " <> " : " = ") + valuePlaceholder;
  }

  private static String createInList(String columnIdentifier, String valuePlaceholder, int valueCount, boolean negated) {
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
