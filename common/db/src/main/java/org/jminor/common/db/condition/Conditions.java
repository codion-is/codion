/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.List;

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

    private static String toString(final Conjunction conjunction) {
      switch (conjunction) {
        case AND: return " and ";
        case OR: return " or ";
        default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
      }
    }
  }
}
