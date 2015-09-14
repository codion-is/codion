/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import org.jminor.common.model.Conjunction;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class CriteriaUtilTest {

  private static final CriteriaSet AND_SET = CriteriaUtil.criteriaSet(Conjunction.AND, new TestCriteria(), new TestCriteria());
  private static final CriteriaSet<Object> OR_SET = CriteriaUtil.criteriaSet(Conjunction.OR, new TestCriteria(), new TestCriteria());
  private static final CriteriaSet<Object> AND_OR_AND_SET = CriteriaUtil.criteriaSet(Conjunction.AND, AND_SET, OR_SET);
  private static final CriteriaSet<Object> AND_OR_OR_SET = CriteriaUtil.criteriaSet(Conjunction.OR, AND_SET, OR_SET);

  @Test
  public void andSet() {
    assertEquals("AND criteria set should be working", "(criteria and criteria)", AND_SET.getWhereClause());
    assertEquals(2, AND_SET.getCriteriaCount());
  }

  @Test
  public void orSet() {
    assertEquals("OR criteria set should be working", "(criteria or criteria)", OR_SET.getWhereClause());
    assertEquals(2, OR_SET.getValues().size());
    assertEquals(2, OR_SET.getValueKeys().size());
  }

  @Test
  public void andOrAndSet() {
    assertEquals("AND OR AND criteria set should be working", "((criteria and criteria) and (criteria or criteria))", AND_OR_AND_SET.getWhereClause());
  }

  @Test
  public void andOrOrSet() {
    assertEquals("AND OR OR criteria set should be working", "((criteria and criteria) or (criteria or criteria))", AND_OR_OR_SET.getWhereClause());
  }

  @Test
  public void getCriteriaCount() {
    CriteriaSet<Object> set = CriteriaUtil.criteriaSet(Conjunction.OR);
    assertEquals(0, set.getCriteriaCount());
    assertEquals("", set.getWhereClause());

    set = CriteriaUtil.criteriaSet(Conjunction.OR, new TestCriteria());
    assertEquals(1, set.getCriteriaCount());

    set = CriteriaUtil.criteriaSet(Conjunction.OR, new TestCriteria(), new TestCriteria(), null, new TestCriteria());
    assertEquals(3, set.getCriteriaCount());
  }

  @Test
  public void stringCriteriaWithoutValue() {
    final String crit = "id = 1";
    final Criteria<Object> criteria = CriteriaUtil.stringCriteria(crit);
    assertEquals(crit, criteria.getWhereClause());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(0, criteria.getValues().size());
  }

  @Test
  public void stringCriteriaWithValue() {
    final String crit = "id = ?";
    final Criteria criteria = CriteriaUtil.stringCriteria(crit, Collections.singletonList(1), Collections.singletonList("id"));
    assertEquals(crit, criteria.getWhereClause());
    assertEquals(1, criteria.getValueKeys().size());
    assertEquals(1, criteria.getValues().size());
  }

  @Test (expected = IllegalArgumentException.class)
  public void stringCriteriaNullCriteriaString() {
    CriteriaUtil.stringCriteria(null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void stringCriteriaNullValues() {
    CriteriaUtil.stringCriteria("some is null", null, Collections.<String>emptyList());
  }

  @Test (expected = IllegalArgumentException.class)
  public void stringCriteriaNullKeys() {
    CriteriaUtil.stringCriteria("some is null", Collections.emptyList(), null);
  }

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final Criteria<Integer> criteria = CriteriaUtil.criteriaSet(Conjunction.AND,
            CriteriaUtil.stringCriteria("test", Arrays.asList("val1", "val2"), Arrays.asList(1, 2)),
            CriteriaUtil.stringCriteria("testing", Arrays.asList("val1", "val2"), Arrays.asList(1, 2)));
    deserialize(serialize(criteria));
  }

  private static byte[] serialize(final Object obj) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);

    return out.toByteArray();
  }

  private static Object deserialize(final byte[] data) throws IOException, ClassNotFoundException {
    return new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
  }

  private static class TestCriteria implements Criteria {
    @Override
    public String getWhereClause() {
      return "criteria";
    }

    @Override
    public List<?> getValues() {
      return Collections.singletonList(1);
    }

    @Override
    public List<?> getValueKeys() {
      return Collections.singletonList("key");
    }
  }
}
