/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

import org.jminor.common.Conjunction;

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

public final class ConditionsTest {

  private static final Condition.Set AND_SET = Conditions.conditionSet(Conjunction.AND, new TestCondition(), new TestCondition());
  private static final Condition.Set<Object> OR_SET = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition());
  private static final Condition.Set<Object> AND_OR_AND_SET = Conditions.conditionSet(Conjunction.AND, AND_SET, OR_SET);
  private static final Condition.Set<Object> AND_OR_OR_SET = Conditions.conditionSet(Conjunction.OR, AND_SET, OR_SET);

  @Test
  public void andSet() {
    assertEquals("AND condition set should be working", "(condition and condition)", AND_SET.getWhereClause());
    assertEquals(2, AND_SET.getConditionCount());
  }

  @Test
  public void orSet() {
    assertEquals("OR condition set should be working", "(condition or condition)", OR_SET.getWhereClause());
    assertEquals(2, OR_SET.getValues().size());
    assertEquals(2, OR_SET.getValueKeys().size());
  }

  @Test
  public void andOrAndSet() {
    assertEquals("AND OR AND condition set should be working", "((condition and condition) and (condition or condition))", AND_OR_AND_SET.getWhereClause());
  }

  @Test
  public void andOrOrSet() {
    assertEquals("AND OR OR condition set should be working", "((condition and condition) or (condition or condition))", AND_OR_OR_SET.getWhereClause());
  }

  @Test
  public void getConditionCount() {
    Condition.Set<Object> set = Conditions.conditionSet(Conjunction.OR);
    assertEquals(0, set.getConditionCount());
    assertEquals("", set.getWhereClause());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition());
    assertEquals(1, set.getConditionCount());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition(), null, new TestCondition());
    assertEquals(3, set.getConditionCount());
  }

  @Test
  public void stringConditionWithoutValue() {
    final String crit = "id = 1";
    final Condition<Object> condition = Conditions.stringCondition(crit);
    assertEquals(crit, condition.getWhereClause());
    assertEquals(0, condition.getValueKeys().size());
    assertEquals(0, condition.getValues().size());
  }

  @Test
  public void stringConditionWithValue() {
    final String crit = "id = ?";
    final Condition condition = Conditions.stringCondition(crit, Collections.singletonList(1), Collections.singletonList("id"));
    assertEquals(crit, condition.getWhereClause());
    assertEquals(1, condition.getValueKeys().size());
    assertEquals(1, condition.getValues().size());
  }

  @Test (expected = NullPointerException.class)
  public void stringConditionNullConditionString() {
    Conditions.stringCondition(null);
  }

  @Test (expected = NullPointerException.class)
  public void stringConditionNullValues() {
    Conditions.stringCondition("some is null", null, Collections.<String>emptyList());
  }

  @Test (expected = NullPointerException.class)
  public void stringConditionNullKeys() {
    Conditions.stringCondition("some is null", Collections.emptyList(), null);
  }

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final Condition<Integer> condition = Conditions.conditionSet(Conjunction.AND,
            Conditions.stringCondition("test", Arrays.asList("val1", "val2"), Arrays.asList(1, 2)),
            Conditions.stringCondition("testing", Arrays.asList("val1", "val2"), Arrays.asList(1, 2)));
    deserialize(serialize(condition));
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

  private static class TestCondition implements Condition {
    @Override
    public String getWhereClause() {
      return "condition";
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
