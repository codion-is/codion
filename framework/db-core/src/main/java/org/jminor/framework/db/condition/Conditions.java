/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.Conjunction.OR;
import static org.jminor.common.db.ConditionType.LIKE;
import static org.jminor.framework.domain.Entities.getValues;

/**
 * A factory class for {@link Condition} instances
 */
public final class Conditions {

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";
  static final String CONDITION_TYPE_PARAM = "conditionType";

  private Conditions() {}

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction) {
    return conditionSet(conjunction, (Condition) null);
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param condition the condition
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Condition condition) {
    return conditionSet(conjunction, condition, null);
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Condition firstCondition,
                                           final Condition secondCondition) {
    return conditionSet(conjunction, firstCondition, secondCondition, null);
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param thirdCondition the third condition
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Condition firstCondition,
                                           final Condition secondCondition, final Condition thirdCondition) {
    return conditionSet(conjunction, firstCondition, secondCondition, thirdCondition, null);
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param thirdCondition the third condition
   * @param fourthCondition the fourth condition
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Condition firstCondition,
                                           final Condition secondCondition, final Condition thirdCondition,
                                           final Condition fourthCondition) {
    return conditionSet(conjunction, firstCondition, secondCondition, thirdCondition, fourthCondition, null);
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param thirdCondition the third condition
   * @param fourthCondition the fourth condition
   * @param fifthCondition the fifth condition
   * @param <T> the condition column type
   * @return a {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Condition firstCondition,
                                           final Condition secondCondition, final Condition thirdCondition,
                                           final Condition fourthCondition, final Condition fifthCondition) {
    return conditionSet(conjunction, asList(firstCondition, secondCondition, thirdCondition, fourthCondition, fifthCondition));
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the conjunction to use
   * @param condition the Condition objects to be included in this set
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Collection<Condition> condition) {
    return new DefaultSet(conjunction, condition);
  }

  /**
   * Creates a new {@link Condition} based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @return a new Condition instance
   * @throws NullPointerException in case the condition string is null
   */
  public static Condition stringCondition(final String conditionString) {
    return stringCondition(conditionString, emptyList(), emptyList());
  }

  /**
   * Creates a new {@link Condition} based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @param values the values used by this condition string
   * @param properties the properties representing the values used by this condition, in the same order as their respective values
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   */
  public static Condition stringCondition(final String conditionString, final List values,
                                          final List<Property.ColumnProperty> properties) {
    return new StringCondition(conditionString, values, properties);
  }

  /**
   * Creates a {@link Condition} for the given property, with the operator specified by the {@code conditionType}
   * and {@code value}. Note that {@code value} may be a single value, a Collection of values or null.
   * @param property the property
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static Condition propertyCondition(final Property.ColumnProperty property,
                                            final ConditionType conditionType, final Object value) {
    return propertyCondition(property, conditionType, true, value);
  }

  /**
   * Creates a {@link Condition} for the given property, with the operator specified by the {@code conditionType}
   * and {@code value}. Note that {@code value} may be a single value, a Collection of values or null.
   * @param property the property
   * @param conditionType the search type
   * @param caseSensitive true if the condition should be case sensitive, only applicable to string properties
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static Condition propertyCondition(final Property.ColumnProperty property,
                                            final ConditionType conditionType, final boolean caseSensitive,
                                            final Object value) {
    return new DefaultCondition(property, conditionType, value).setCaseSensitive(caseSensitive);
  }

  /**
   * Creates a {@link Condition} for the given foreign key property, with the operator specified by the {@code conditionType}
   * and {@code value}.
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value, may be null
   * @return a foreign key condition based on the given value
   */
  public static Condition foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                              final ConditionType conditionType, final Entity value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, singletonList(value));
  }

  /**
   * Creates a {@link Condition} for the given foreign key property, with the operator specified by the {@code conditionType}
   * and {@code value}.
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param value the condition value
   * @return a foreign key condition based on the given value
   */
  public static Condition foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                              final ConditionType conditionType, final Entity.Key value) {
    return foreignKeyCondition(foreignKeyProperty, conditionType, singletonList(value));
  }

  /**
   * Creates a {@link Condition} for the given foreign key property, with the operator specified by
   * the {@code conditionType} and {@code values}.
   * {@code values} may contain either instances of {@link Entity} or {@link Entity.Key}
   * @param foreignKeyProperty the foreign key property
   * @param conditionType the search type
   * @param values the condition values
   * @return a foreign key condition based on the given values
   */
  public static Condition foreignKeyCondition(final Property.ForeignKeyProperty foreignKeyProperty,
                                              final ConditionType conditionType, final Collection values) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    requireNonNull(conditionType, CONDITION_TYPE_PARAM);
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return createCompositeKeyCondition(foreignKeyProperty.getProperties(), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);
      return propertyCondition(foreignKeyProperty.getProperties().get(0), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return propertyCondition(foreignKeyProperty.getProperties().get(0), conditionType, getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  static Condition createCompositeKeyCondition(final List<Property.ColumnProperty> properties,
                                               final ConditionType conditionType,
                                               final List<Entity.Key> keys) {
    if (keys.size() == 1) {
      return createSingleCompositeCondition(properties, conditionType, keys.get(0));
    }

    return createMultipleCompositeCondition(properties, conditionType, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition createMultipleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                            final ConditionType conditionType,
                                                            final List<Entity.Key> keys) {
    final Condition.Set conditionSet = conditionSet(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionSet.add(createSingleCompositeCondition(properties, conditionType, keys.get(i)));
    }

    return conditionSet;
  }

  private static Condition createSingleCompositeCondition(final List<Property.ColumnProperty> properties,
                                                          final ConditionType conditionType,
                                                          final Entity.Key entityKey) {
    final Condition.Set conditionSet = conditionSet(AND);
    for (int i = 0; i < properties.size(); i++) {
      conditionSet.add(new DefaultCondition(properties.get(i), conditionType,
              entityKey == null ? null : entityKey.get(entityKey.getProperties().get(i))));
    }

    return conditionSet;
  }

  private static List<Entity.Key> getKeys(final Object value) {
    final List<Entity.Key> keys = new ArrayList<>();
    if (value instanceof Collection) {
      if (((Collection) value).isEmpty()) {
        keys.add(null);
      }
      else {
        for (final Object object : (Collection) value) {
          keys.add(getKey(object));
        }
      }
    }
    else {
      keys.add(getKey(value));
    }

    return keys;
  }

  private static Entity.Key getKey(final Object value) {
    if (value == null || value instanceof Entity.Key) {
      return (Entity.Key) value;
    }
    else if (value instanceof Entity) {
      return ((Entity) value).getKey();
    }

    throw new IllegalArgumentException("Foreign key condition uses only Entity or Entity.Key instances for values");
  }

  /** Assumes {@code keys} is not empty. */
  static Condition createKeyCondition(final List<Entity.Key> keys) {
    final Entity.Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return createCompositeKeyCondition(firstKey.getProperties(), LIKE, keys);
    }

    return propertyCondition(firstKey.getFirstProperty(), LIKE, getValues(keys));
  }

  private static final class DefaultSet implements Condition.Set {

    private static final long serialVersionUID = 1;

    private Conjunction conjunction;
    private List<Condition> conditions = new ArrayList<>();

    private DefaultSet(final Conjunction conjunction, final Collection<Condition> conditions) {
      this.conjunction = conjunction;
      for (final Condition condition : conditions) {
        add(condition);
      }
    }

    @Override
    public void add(final Condition condition) {
      if (condition != null) {
        conditions.add(condition);
      }
    }

    @Override
    public int getConditionCount() {
      return conditions.size();
    }

    @Override
    public String getWhereClause() {
      if (conditions.isEmpty()) {
        return "";
      }

      final StringBuilder conditionString = new StringBuilder(conditions.size() > 1 ? "(" : "");
      for (int i = 0; i < conditions.size(); i++) {
        conditionString.append(conditions.get(i).getWhereClause());
        if (i < conditions.size() - 1) {
          conditionString.append(toString(conjunction));
        }
      }

      return conditionString.append(conditions.size() > 1 ? ")" : "").toString();
    }

    @Override
    public List getValues() {
      final List values = new ArrayList<>();
      for (int i = 0; i < conditions.size(); i++) {
        values.addAll(conditions.get(i).getValues());
      }

      return values;
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      final List<Property.ColumnProperty> columns = new ArrayList<>();
      for (int i = 0; i < conditions.size(); i++) {
        columns.addAll(conditions.get(i).getProperties());
      }

      return columns;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conjunction);
      stream.writeInt(conditions.size());
      for (int i = 0; i < conditions.size(); i++) {
        stream.writeObject(conditions.get(i));
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      conjunction = (Conjunction) stream.readObject();
      final int conditionCount = stream.readInt();
      conditions = new ArrayList<>(conditionCount);
      for (int i = 0; i < conditionCount; i++) {
        conditions.add((Condition) stream.readObject());
      }
    }

    private static String toString(final Conjunction conjunction) {
      switch (conjunction) {
        case AND: return " and ";
        case OR: return " or ";
        default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
      }
    }
  }

  /**
   * A object for encapsulating a query condition with a single property and one or more values.
   */
  static final class DefaultCondition implements Condition {

    private static final long serialVersionUID = 1;

    /**
     * The property used in this condition
     */
    private Property.ColumnProperty property;

    /**
     * The values used in this condition
     */
    private List values;

    /**
     * True if this condition tests for null
     */
    private boolean isNullCondition;

    /**
     * The search type used in this condition
     */
    private ConditionType conditionType;

    /**
     * True if this condition should be case sensitive, only applies to condition based on string properties
     */
    private boolean caseSensitive = true;

    /**
     * Instantiates a new PropertyCondition instance
     * @param property the property
     * @param conditionType the search type
     * @param value the value, can be a Collection
     */
    DefaultCondition(final Property.ColumnProperty property, final ConditionType conditionType, final Object value) {
      requireNonNull(property, "property");
      requireNonNull(conditionType, CONDITION_TYPE_PARAM);
      this.property = property;
      this.conditionType = conditionType;
      this.isNullCondition = value == null;
      this.values = initializeValues(value, property);
    }

    @Override
    public List getValues() {
      if (isNullCondition) {
        return emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return values;
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      if (isNullCondition) {
        return emptyList();
      }//null condition, uses 'x is null', not 'x = ?'

      return Collections.nCopies(values.size(), property);
    }

    @Override
    public String getWhereClause() {
      return getConditionString();
    }

    /**
     * @return the number of values contained in this condition.
     */
    private int getValueCount() {
      if (isNullCondition) {
        return 0;
      }

      return values.size();
    }

    /**
     * Sets whether this condition should be case sensitive, only applies to condition based on string properties
     * @param caseSensitive if true then this condition is case sensitive, false otherwise
     * @return this PropertyCondition instance
     */
    DefaultCondition setCaseSensitive(final boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    private String getConditionString() {
      final String columnIdentifier = initializeColumnIdentifier(property.isString());
      if (isNullCondition) {
        return columnIdentifier + (conditionType == LIKE ? " is null" : " is not null");
      }

      final String valuePlaceholder = getValuePlaceholder();
      final String value2Placeholder = getValueCount() == 2 ? getValuePlaceholder() : null;

      switch (conditionType) {
        case LIKE:
          return getLikeCondition(columnIdentifier, valuePlaceholder, false);
        case NOT_LIKE:
          return getLikeCondition(columnIdentifier, valuePlaceholder, true);
        case LESS_THAN:
          return columnIdentifier + " <= " + valuePlaceholder;
        case GREATER_THAN:
          return columnIdentifier + " >= " + valuePlaceholder;
        case WITHIN_RANGE:
          return "(" + columnIdentifier + " >= " + valuePlaceholder + " and " + columnIdentifier + " <= " + value2Placeholder + ")";
        case OUTSIDE_RANGE:
          return "(" + columnIdentifier + " <= " + valuePlaceholder + " or " + columnIdentifier + " >= " + value2Placeholder + ")";
        default:
          throw new IllegalArgumentException("Unknown search type" + conditionType);
      }
    }

    private String getValuePlaceholder() {
      return property.isString() && !caseSensitive ? "upper(?)" : "?";
    }

    private String getLikeCondition(final String columnIdentifier, final String valuePlaceholder, final boolean notLike) {
      if (getValueCount() > 1) {
        return getInList(columnIdentifier, valuePlaceholder, getValueCount(), notLike);
      }

      if (property.isString() && containsWildcards((String) values.get(0))) {
        return columnIdentifier + (notLike ? " not like " : " like ") + valuePlaceholder;
      }
      else {
        return columnIdentifier + (notLike ? " <> " : " = ") + valuePlaceholder;
      }
    }

    private static List initializeValues(final Object value, final Property.ColumnProperty property) {
      final List values;
      if (value instanceof List) {
        values = (List) value;
      }
      else if (value instanceof Collection) {
        values = new ArrayList((Collection) value);
      }
      else {
        values = singletonList(value);
      }
      if (values.isEmpty()) {
        throw new IllegalArgumentException("No values specified for PropertyCondition: " + property);
      }
      for (int i = 0; i < values.size(); i++) {
        property.validateType(values.get(i));
      }

      return values;
    }

    private static String getInList(final String columnIdentifier, final String valuePlaceholder, final int valueCount, final boolean not) {
      final StringBuilder stringBuilder = new StringBuilder("(").append(columnIdentifier).append(not ? NOT_IN_PREFIX : IN_PREFIX);
      int cnt = 1;
      for (int i = 0; i < valueCount; i++) {
        stringBuilder.append(valuePlaceholder);
        if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
          stringBuilder.append(not ? ") and " : ") or ").append(columnIdentifier).append(not ? NOT_IN_PREFIX : IN_PREFIX);
          cnt = 1;
        }
        else if (i < valueCount - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("))");

      return stringBuilder.toString();
    }

    private String initializeColumnIdentifier(final boolean isStringProperty) {
      String columnName;
      if (property instanceof Property.SubqueryProperty) {
        columnName = "(" + ((Property.SubqueryProperty) property).getSubQuery() + ")";
      }
      else {
        columnName = property.getColumnName();
      }

      if (!isNullCondition && isStringProperty && !caseSensitive) {
        columnName = "upper(" + columnName + ")";
      }

      return columnName;
    }

    private static boolean containsWildcards(final String value) {
      return value.contains("%") || value.contains("_");
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(property.getDomainId());
      stream.writeObject(property.getEntityId());
      stream.writeObject(property.getPropertyId());
      stream.writeObject(conditionType);
      stream.writeBoolean(isNullCondition);
      stream.writeBoolean(caseSensitive);
      stream.writeInt(values.size());
      for (int i = 0; i < values.size(); i++) {
        stream.writeObject(values.get(i));
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      final String domainId = (String) stream.readObject();
      final String entityId = (String) stream.readObject();
      final String propertyId = (String) stream.readObject();
      property = Domain.getDomain(domainId).getColumnProperty(entityId, propertyId);
      conditionType = (ConditionType) stream.readObject();
      isNullCondition = stream.readBoolean();
      caseSensitive = stream.readBoolean();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
    }
  }

  private static final class StringCondition implements Condition {

    private static final long serialVersionUID = 1;

    private String conditionString;
    private List values;
    private List<Property.ColumnProperty> properties;

    private StringCondition(final String conditionString, final List values, final List<Property.ColumnProperty> properties) {
      this.conditionString = requireNonNull(conditionString, "conditionString");
      this.values = requireNonNull(values, "values");
      this.properties = requireNonNull(properties, "properties");
    }

    @Override
    public String getWhereClause() {
      return conditionString;
    }

    @Override
    public List getValues() {
      return values;
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      return properties;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conditionString);
      stream.writeInt(values.size());
      for (int i = 0; i < values.size(); i++) {
        stream.writeObject(values.get(i));
      }
      stream.writeInt(properties.size());
      for (int i = 0; i < properties.size(); i++) {
        final Property.ColumnProperty property = properties.get(i);
        stream.writeObject(property.getDomainId());
        stream.writeObject(property.getEntityId());
        stream.writeObject(property.getPropertyId());
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      conditionString = (String) stream.readObject();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
      final int columnCount = stream.readInt();
      properties = new ArrayList<>(columnCount);
      for (int i = 0; i < columnCount; i++) {
        final String domainId = (String) stream.readObject();
        final String entityId = (String) stream.readObject();
        final String propertyId = (String) stream.readObject();
        properties.add(Domain.getDomain(domainId).getColumnProperty(entityId, propertyId));
      }
    }
  }
}
