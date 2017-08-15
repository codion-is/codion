/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.Column;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A factory class for {@link Condition} instances
 */
public final class Conditions {

  private Conditions() {}

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction) {
    return conditionSet(conjunction, (Condition) null);
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param condition the condition
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction, final Condition<T> condition) {
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
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                                 final Condition<T> secondCondition) {
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
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                                 final Condition<T> secondCondition, final Condition<T> thirdCondition) {
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
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                                 final Condition<T> secondCondition, final Condition<T> thirdCondition,
                                                                 final Condition<T> fourthCondition) {
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
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                                 final Condition<T> secondCondition, final Condition<T> thirdCondition,
                                                                 final Condition<T> fourthCondition, final Condition<T> fifthCondition) {
    return conditionSet(conjunction, Arrays.asList(firstCondition, secondCondition, thirdCondition, fourthCondition,
            fifthCondition));
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the conjunction to use
   * @param condition the Condition objects to be included in this set
   * @param <T> the condition column type
   * @return a new {@link Condition.Set} instance
   */
  public static <T extends Column> Condition.Set<T> conditionSet(final Conjunction conjunction, final Collection<Condition<T>> condition) {
    return new DefaultSet<>(conjunction, condition);
  }

  /**
   * Instantiates a new Condition based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @param <T> the condition column type
   * @return a new Condition instance
   * @throws NullPointerException in case the condition string is null
   */
  public static <T extends Column> Condition<T> stringCondition(final String conditionString) {
    return stringCondition(conditionString, Collections.emptyList(), Collections.<T>emptyList());
  }

  /**
   * Instantiates a new Condition based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @param values the values required by this condition string
   * @param keys the keys required by this condition string, in the same order as their respective values
   * @param <T> the condition column type
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   */
  public static <T extends Column> Condition<T> stringCondition(final String conditionString, final List values, final List<T> keys) {
    return new StringCondition<>(conditionString, values, keys);
  }

  private static final class DefaultSet<T extends Column> implements Condition.Set<T> {

    private static final long serialVersionUID = 1;

    private Conjunction conjunction;
    private List<Condition<T>> conditions = new ArrayList<>();

    private DefaultSet(final Conjunction conjunction, final Collection<Condition<T>> conditions) {
      this.conjunction = conjunction;
      for (final Condition<T> condition : conditions) {
        add(condition);
      }
    }

    @Override
    public void add(final Condition<T> condition) {
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
      int i = 0;
      for (final Condition condition : conditions) {
        conditionString.append(condition.getWhereClause());
        if (i++ < conditions.size() - 1) {
          conditionString.append(conjunction.toString());
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
    public List<T> getColumns() {
      final List<T> columns = new ArrayList<>();
      for (int i = 0; i < conditions.size(); i++) {
        columns.addAll(conditions.get(i).getColumns());
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
  }

  private static final class StringCondition<T extends Column> implements Condition<T> {

    private static final long serialVersionUID = 1;

    private String conditionString;
    private List values;
    private List<T> columns;

    private StringCondition(final String conditionString, final List values, final List<T> columns) {
      this.conditionString = Objects.requireNonNull(conditionString, "conditionString");
      this.values = Objects.requireNonNull(values, "values");
      this.columns = Objects.requireNonNull(columns, "keys");
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
    public List<T> getColumns() {
      return columns;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conditionString);
      stream.writeInt(values.size());
      for (int i = 0; i < values.size(); i++) {
        stream.writeObject(values.get(i));
      }
      stream.writeInt(columns.size());
      for (int i = 0; i < columns.size(); i++) {
        stream.writeObject(columns.get(i));
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
      columns = new ArrayList<>(columnCount);
      for (int i = 0; i < columnCount; i++) {
        columns.add((T) stream.readObject());
      }
    }
  }
}
