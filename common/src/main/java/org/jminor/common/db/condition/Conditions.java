/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

import org.jminor.common.Conjunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
   * Initializes a new {@link ConditionSet} instance
   * @param conjunction the Conjunction to use
   * @param <T> the Condition key type
   * @return a new ConditionSet instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction) {
    return conditionSet(conjunction, (Condition) null);
  }

  /**
   * Initializes a new ConditionSet instance
   * @param conjunction the Conjunction to use
   * @param condition the condition
   * @param <T> the condition key type
   * @return a new ConditionSet instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction, final Condition<T> condition) {
    return conditionSet(conjunction, condition, null);
  }

  /**
   * Initializes a new ConditionSet instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param <T> the condition key type
   * @return a new ConditionSet instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                 final Condition<T> secondCondition) {
    return conditionSet(conjunction, firstCondition, secondCondition, null);
  }

  /**
   * Initializes a new ConditionSet instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param thirdCondition the third condition
   * @param <T> the condition key type
   * @return a new ConditionSet instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                 final Condition<T> secondCondition, final Condition<T> thirdCondition) {
    return conditionSet(conjunction, firstCondition, secondCondition, thirdCondition, null);
  }

  /**
   * Initializes a new ConditionSet instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param thirdCondition the third condition
   * @param fourthCondition the fourth condition
   * @param <T> the condition key type
   * @return a new ConditionSet instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                 final Condition<T> secondCondition, final Condition<T> thirdCondition,
                                                 final Condition<T> fourthCondition) {
    return conditionSet(conjunction, firstCondition, secondCondition, thirdCondition, fourthCondition, null);
  }

  /**
   * Initializes a new ConditionSet instance
   * @param conjunction the Conjunction to use
   * @param firstCondition the first condition
   * @param secondCondition the second condition
   * @param thirdCondition the third condition
   * @param fourthCondition the fourth condition
   * @param fifthCondition the fifth condition
   * @param <T> the condition key type
   * @return a new ConditionSet instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction, final Condition<T> firstCondition,
                                                 final Condition<T> secondCondition, final Condition<T> thirdCondition,
                                                 final Condition<T> fourthCondition, final Condition<T> fifthCondition) {
    return conditionSet(conjunction, Arrays.asList(firstCondition, secondCondition, thirdCondition, fourthCondition,
            fifthCondition));
  }

  /**
   * Initializes a new ConditionSet instance
   * @param conjunction the conjunction to use
   * @param condition the Condition objects to be included in this set
   * @param <T> the condition key type
   * @return a new Condition instance
   */
  public static <T> ConditionSet<T> conditionSet(final Conjunction conjunction, final Collection<Condition<T>> condition) {
    return new DefaultConditionSet<>(conjunction, condition);
  }

  /**
   * Instantiates a new Condition based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @param <T> the condition key type
   * @return a new Condition instance
   * @throws NullPointerException in case the condition string is null
   */
  public static <T> Condition<T> stringCondition(final String conditionString) {
    return stringCondition(conditionString, Collections.emptyList(), Collections.<T>emptyList());
  }

  /**
   * Instantiates a new Condition based on the given condition string
   * @param conditionString the condition string without the WHERE keyword
   * @param values the values required by this condition string
   * @param keys the keys required by this condition string, in the same order as their respective values
   * @param <T> the condition key type
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   */
  public static <T> Condition<T> stringCondition(final String conditionString, final List values, final List<T> keys) {
    return new StringCondition<>(conditionString, values, keys);
  }

  private static final class DefaultConditionSet<T> implements ConditionSet<T>, Serializable {

    private static final long serialVersionUID = 1;

    private Conjunction conjunction;
    private List<Condition<T>> conditionList = new ArrayList<>();

    private DefaultConditionSet(final Conjunction conjunction, final Collection<Condition<T>> condition) {
      this.conjunction = conjunction;
      for (final Condition<T> criterion : condition) {
        add(criterion);
      }
    }

    @Override
    public void add(final Condition<T> condition) {
      if (condition != null) {
        conditionList.add(condition);
      }
    }

    @Override
    public int getConditionCount() {
      return conditionList.size();
    }

    @Override
    public String getWhereClause() {
      if (conditionList.isEmpty()) {
        return "";
      }

      final StringBuilder conditionString = new StringBuilder(conditionList.size() > 1 ? "(" : "");
      int i = 0;
      for (final Condition condition : conditionList) {
        conditionString.append(condition.getWhereClause());
        if (i++ < conditionList.size() - 1) {
          conditionString.append(conjunction.toString());
        }
      }

      return conditionString.append(conditionList.size() > 1 ? ")" : "").toString();
    }

    @Override
    public List getValues() {
      final List values = new ArrayList<>();
      for (final Condition<T> condition : conditionList) {
        values.addAll(condition.getValues());
      }

      return values;
    }

    @Override
    public List<T> getValueKeys() {
      final List<T> keys = new ArrayList<>();
      for (final Condition<T> condition : conditionList) {
        keys.addAll(condition.getValueKeys());
      }

      return keys;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conjunction);
      stream.writeInt(conditionList.size());
      for (final Condition value : conditionList) {
        stream.writeObject(value);
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      conjunction = (Conjunction) stream.readObject();
      final int conditionCount = stream.readInt();
      conditionList = new ArrayList<>(conditionCount);
      for (int i = 0; i < conditionCount; i++) {
        conditionList.add((Condition) stream.readObject());
      }
    }
  }

  private static final class StringCondition<T> implements Condition<T>, Serializable {

    private static final long serialVersionUID = 1;

    private String conditionString;
    private List values;
    private List<T> keys;

    private StringCondition(final String conditionString, final List values, final List<T> keys) {
      this.conditionString = Objects.requireNonNull(conditionString, "conditionString");
      this.values = Objects.requireNonNull(values, "values");
      this.keys = Objects.requireNonNull(keys, "keys");
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
    public List<T> getValueKeys() {
      return keys;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conditionString);
      stream.writeInt(values.size());
      for (final Object value : values) {
        stream.writeObject(value);
      }
      stream.writeInt(keys.size());
      for (final T key : keys) {
        stream.writeObject(key);
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      conditionString = (String) stream.readObject();
      final int valueCount = stream.readInt();
      values = new ArrayList<>(valueCount);
      for (int i = 0; i < valueCount; i++) {
        values.add(stream.readObject());
      }
      final int keyCount = stream.readInt();
      keys = new ArrayList<>(keyCount);
      for (int i = 0; i < keyCount; i++) {
        keys.add((T) stream.readObject());
      }
    }
  }
}
