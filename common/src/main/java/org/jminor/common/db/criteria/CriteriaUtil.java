/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.Util;
import org.jminor.common.model.Conjunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A factory class for Criteria instances
 */
public final class CriteriaUtil {

  private CriteriaUtil() {}

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param <T> the criteria key type
   * @return a new CriteriaSet instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction) {
    return criteriaSet(conjunction, (Criteria) null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param criteria the criteria
   * @param <T> the criteria key type
   * @return a new CriteriaSet instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction, final Criteria<T> criteria) {
    return criteriaSet(conjunction, criteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param <T> the criteria key type
   * @return a new CriteriaSet instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria,
                                               final Criteria<T> secondCriteria) {
    return criteriaSet(conjunction, firstCriteria, secondCriteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param thirdCriteria the third criteria
   * @param <T> the criteria key type
   * @return a new CriteriaSet instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria,
                                               final Criteria<T> secondCriteria, final Criteria<T> thirdCriteria) {
    return criteriaSet(conjunction, firstCriteria, secondCriteria, thirdCriteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param thirdCriteria the third criteria
   * @param fourthCriteria the fourth criteria
   * @param <T> the criteria key type
   * @return a new CriteriaSet instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria,
                                               final Criteria<T> secondCriteria, final Criteria<T> thirdCriteria,
                                               final Criteria<T> fourthCriteria) {
    return criteriaSet(conjunction, firstCriteria, secondCriteria, thirdCriteria, fourthCriteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param thirdCriteria the third criteria
   * @param fourthCriteria the fourth criteria
   * @param fifthCriteria the fifth criteria
   * @param <T> the criteria key type
   * @return a new CriteriaSet instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria,
                                               final Criteria<T> secondCriteria, final Criteria<T> thirdCriteria,
                                               final Criteria<T> fourthCriteria, final Criteria<T> fifthCriteria) {
    return criteriaSet(conjunction, Arrays.asList(firstCriteria, secondCriteria, thirdCriteria, fourthCriteria, fifthCriteria));
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the conjunction to use
   * @param criteria the Criteria objects to be included in this set
   * @param <T> the criteria key type
   * @return a new Criteria instance
   */
  public static <T> CriteriaSet<T> criteriaSet(final Conjunction conjunction, final Collection<Criteria<T>> criteria) {
    return new DefaultCriteriaSet<>(conjunction, criteria);
  }

  /**
   * Instantiates a new Criteria based on the given criteria string
   * @param criteriaString the criteria string without the WHERE keyword
   * @param <T> the criteria key type
   * @return a new Criteria instance
   * @throws IllegalArgumentException in case the criteria string is null
   */
  public static <T> Criteria<T> stringCriteria(final String criteriaString) {
    return stringCriteria(criteriaString, Collections.emptyList(), Collections.<T>emptyList());
  }

  /**
   * Instantiates a new Criteria based on the given criteria string
   * @param criteriaString the criteria string without the WHERE keyword
   * @param values the values required by this criteria string
   * @param keys the keys required by this criteria string, in the same order as their respective values
   * @param <T> the criteria key type
   * @return a new Criteria instance
   * @throws IllegalArgumentException in case any of the parameters are null
   */
  public static <T> Criteria<T> stringCriteria(final String criteriaString, final List values, final List<T> keys) {
    return new StringCriteria<>(criteriaString, values, keys);
  }

  private static final class DefaultCriteriaSet<T> implements CriteriaSet<T>, Serializable {

    private static final long serialVersionUID = 1;

    private Conjunction conjunction;
    private List<Criteria<T>> criteriaList = new ArrayList<>();

    private DefaultCriteriaSet(final Conjunction conjunction, final Collection<Criteria<T>> criteria) {
      this.conjunction = conjunction;
      for (final Criteria<T> criterion : criteria) {
        add(criterion);
      }
    }

    @Override
    public void add(final Criteria<T> criteria) {
      if (criteria != null) {
        criteriaList.add(criteria);
      }
    }

    @Override
    public int getCriteriaCount() {
      return criteriaList.size();
    }

    @Override
    public String getWhereClause() {
      if (criteriaList.isEmpty()) {
        return "";
      }

      final StringBuilder criteriaString = new StringBuilder(criteriaList.size() > 1 ? "(" : "");
      int i = 0;
      for (final Criteria criteria : criteriaList) {
        criteriaString.append(criteria.getWhereClause());
        if (i++ < criteriaList.size() - 1) {
          criteriaString.append(conjunction.toString());
        }
      }

      return criteriaString.append(criteriaList.size() > 1 ? ")" : "").toString();
    }

    @Override
    public List getValues() {
      final List values = new ArrayList<>();
      for (final Criteria<T> criteria : criteriaList) {
        values.addAll(criteria.getValues());
      }

      return values;
    }

    @Override
    public List<T> getValueKeys() {
      final List<T> keys = new ArrayList<>();
      for (final Criteria<T> criteria : criteriaList) {
        keys.addAll(criteria.getValueKeys());
      }

      return keys;
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(conjunction);
      stream.writeInt(criteriaList.size());
      for (final Criteria value : criteriaList) {
        stream.writeObject(value);
      }
    }

    private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
      conjunction = (Conjunction) stream.readObject();
      final int criteriaCount = stream.readInt();
      criteriaList = new ArrayList<>(criteriaCount);
      for (int i = 0; i < criteriaCount; i++) {
        criteriaList.add((Criteria) stream.readObject());
      }
    }
  }

  private static final class StringCriteria<T> implements Criteria<T>, Serializable {

    private static final long serialVersionUID = 1;

    private String criteriaString;
    private List values;
    private List<T> keys;

    private StringCriteria(final String criteriaString, final List values, final List<T> keys) {
      this.criteriaString = Util.rejectNullValue(criteriaString, "criteriaString");
      this.values = Util.rejectNullValue(values, "values");
      this.keys = Util.rejectNullValue(keys, "keys");
    }

    @Override
    public String getWhereClause() {
      return criteriaString;
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
      stream.writeObject(criteriaString);
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
      criteriaString = (String) stream.readObject();
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
