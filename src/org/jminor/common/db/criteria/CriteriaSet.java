/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A class encapsulating a set of Criteria objects, that should be either AND'ed
 * or OR'ed together in a query context
 */
public final class CriteriaSet<T> implements Criteria<T>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The conjunction used by this CriteriaSet
   */
  private Conjunction conjunction;

  /**
   * The criteria in this set
   */
  private List<Criteria<T>> criteriaList = new ArrayList<>();

  /**
   * Used for serialization, not for general use
   */
  CriteriaSet() {}

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   */
  public CriteriaSet(final Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param criteria the criteria
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T> criteria) {
    this(conjunction, criteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria, final Criteria<T> secondCriteria) {
    this(conjunction, firstCriteria, secondCriteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param thirdCriteria the third criteria
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria, final Criteria<T> secondCriteria,
                     final Criteria<T> thirdCriteria) {
    this(conjunction, firstCriteria, secondCriteria, thirdCriteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param thirdCriteria the third criteria
   * @param fourthCriteria the fourth criteria
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria, final Criteria<T> secondCriteria,
                     final Criteria<T> thirdCriteria, final Criteria<T> fourthCriteria) {
    this(conjunction, firstCriteria, secondCriteria, thirdCriteria, fourthCriteria, null);
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the Conjunction to use
   * @param firstCriteria the first criteria
   * @param secondCriteria the second criteria
   * @param thirdCriteria the third criteria
   * @param fourthCriteria the fourth criteria
   * @param fifthCriteria the fifth criteria
   */
  public CriteriaSet(final Conjunction conjunction, final Criteria<T> firstCriteria, final Criteria<T> secondCriteria,
                     final Criteria<T> thirdCriteria, final Criteria<T> fourthCriteria, final Criteria<T> fifthCriteria) {
    this(conjunction, Arrays.asList(firstCriteria, secondCriteria, thirdCriteria, fourthCriteria, fifthCriteria));
  }

  /**
   * Initializes a new CriteriaSet instance
   * @param conjunction the conjunction to use
   * @param criteria the Criteria objects to be included in this set
   */
  public CriteriaSet(final Conjunction conjunction, final Collection<Criteria<T>> criteria) {
    this.conjunction = conjunction;
    for (final Criteria<T> criterion : criteria) {
      add(criterion);
    }
  }

  /**
   * Adds a new Criteria object to this set, adding a null criteria has no effect
   * @param criteria the Criteria to add
   */
  public void add(final Criteria<T> criteria) {
    if (criteria != null) {
      criteriaList.add(criteria);
    }
  }

  /**
   * @return the number of criteria in this set
   */
  public int getCriteriaCount() {
    return criteriaList.size();
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public List<Object> getValues() {
    final List<Object> values = new ArrayList<>();
    for (final Criteria<T> criteria : criteriaList) {
      values.addAll(criteria.getValues());
    }

    return values;
  }

  /** {@inheritDoc} */
  @Override
  public List<T> getValueKeys() {
    final List<T> types = new ArrayList<>();
    for (final Criteria<T> criteria : criteriaList) {
      types.addAll(criteria.getValueKeys());
    }

    return types;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(conjunction);
    stream.writeInt(criteriaList.size());
    for (final Criteria<T> value : criteriaList) {
      stream.writeObject(value);
    }
  }

  @SuppressWarnings({"unchecked"})
  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    conjunction = (Conjunction) stream.readObject();
    final int criteriaCount = stream.readInt();
    criteriaList = new ArrayList<>(criteriaCount);
    for (int i = 0; i < criteriaCount; i++) {
      criteriaList.add((Criteria<T>) stream.readObject());
    }
  }
}
