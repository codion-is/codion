/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.DateFormats;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jminor.common.Util.deserialize;
import static org.jminor.common.Util.serialize;
import static org.jminor.framework.db.condition.Conditions.propertyCondition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ConditionsTest {

  private static final Condition.Set AND_SET = Conditions.conditionSet(Conjunction.AND, new TestCondition(), new TestCondition());
  private static final Condition.Set OR_SET = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition());
  private static final Condition.Set AND_OR_AND_SET = Conditions.conditionSet(Conjunction.AND, AND_SET, OR_SET);
  private static final Condition.Set AND_OR_OR_SET = Conditions.conditionSet(Conjunction.OR, AND_SET, OR_SET);

  @Test
  public void andSet() {
    assertEquals("(condition and condition)", AND_SET.getWhereClause(), "AND condition set should be working");
    assertEquals(2, AND_SET.getConditionCount());
  }

  @Test
  public void orSet() {
    assertEquals("(condition or condition)", OR_SET.getWhereClause(), "OR condition set should be working");
    assertEquals(2, OR_SET.getValues().size());
    assertEquals(2, OR_SET.getProperties().size());
  }

  @Test
  public void andOrAndSet() {
    assertEquals("((condition and condition) and (condition or condition))", AND_OR_AND_SET.getWhereClause(), "AND OR AND condition set should be working");
  }

  @Test
  public void andOrOrSet() {
    assertEquals("((condition and condition) or (condition or condition))", AND_OR_OR_SET.getWhereClause(), "AND OR OR condition set should be working");
  }

  @Test
  public void getConditionCount() {
    Condition.Set set = Conditions.conditionSet(Conjunction.OR);
    assertEquals(0, set.getConditionCount());
    assertEquals("", set.getWhereClause());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition());
    assertEquals(1, set.getConditionCount());

    set = Conditions.conditionSet(Conjunction.OR, new TestCondition(), new TestCondition(), null, new TestCondition());
    assertEquals(3, set.getConditionCount());
  }  @Test
  public void conditionString() {
    //string, is null
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.VARCHAR);
    Condition testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    testCrit = propertyCondition(property, ConditionType.LIKE, (Object) null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //string, =
    String value = "value";
    testCrit = propertyCondition(property, ConditionType.LIKE, value);
    assertEquals("colName = ?", testCrit.getWhereClause());

    //string, like
    value = "val%ue";
    testCrit = propertyCondition(property, ConditionType.LIKE, value);
    assertEquals("colName like ?", testCrit.getWhereClause());

    //string, <>
    value = "value";
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, value);
    assertEquals("colName <> ?", testCrit.getWhereClause());

    //string, not like
    value = "val%ue";
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, value);
    assertEquals("colName not like ?", testCrit.getWhereClause());

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = propertyCondition(property, ConditionType.WITHIN_RANGE, asList(value, value2));
    assertEquals("(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = propertyCondition(property, ConditionType.OUTSIDE_RANGE, asList(value, value2));
    assertEquals("(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = propertyCondition(property, ConditionType.LIKE, asList(value, value2, value3));
    assertEquals("(colName in (?, ?, ?))", testCrit.getWhereClause());

    //
    //
    //case insensitive
    //
    //
    testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //string, =
    value = "value";
    testCrit = propertyCondition(property, ConditionType.LIKE, false, value);
    assertEquals("upper(colName) = upper(?)", testCrit.getWhereClause());

    //string, like
    value = "val%ue";
    testCrit = propertyCondition(property, ConditionType.LIKE, false, value);
    assertEquals("upper(colName) like upper(?)", testCrit.getWhereClause());

    //string, <>
    value = "value";
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, false, value);
    assertEquals("upper(colName) <> upper(?)", testCrit.getWhereClause());

    //string, not like
    value = "val%ue";
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, false, value);
    assertEquals("upper(colName) not like upper(?)", testCrit.getWhereClause());

    //string, between
    value = "min";
    value2 = "max";
    testCrit = propertyCondition(property, ConditionType.WITHIN_RANGE, false, asList(value, value2));
    assertEquals("(upper(colName) >= upper(?) and upper(colName) <= upper(?))", testCrit.getWhereClause());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = propertyCondition(property, ConditionType.OUTSIDE_RANGE, false, asList(value, value2));
    assertEquals("(upper(colName) <= upper(?) or upper(colName) >= upper(?))", testCrit.getWhereClause());

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = propertyCondition(property, ConditionType.LIKE, false, asList(value, value2, value3));
    assertEquals("(upper(colName) in (upper(?), upper(?), upper(?)))", testCrit.getWhereClause());
  }

  @Test
  public void conditionInt() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.INTEGER);
    Condition testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = propertyCondition(property, ConditionType.LIKE, 124);
    assertEquals("colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, 124);
    assertEquals("colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = propertyCondition(property, ConditionType.WITHIN_RANGE, asList(2, 4));
    assertEquals("(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = propertyCondition(property, ConditionType.OUTSIDE_RANGE, asList(2, 4));
    assertEquals("(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = propertyCondition(property, ConditionType.LIKE, asList(2, 3, 4));
    assertEquals("(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionDouble() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DOUBLE);
    Condition testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = propertyCondition(property, ConditionType.LIKE, 124.2);
    assertEquals("colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, 124.2);
    assertEquals("colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = propertyCondition(property, ConditionType.WITHIN_RANGE, asList(2.2, 4.2));
    assertEquals("(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = propertyCondition(property, ConditionType.OUTSIDE_RANGE, asList(2.2, 4.2));
    assertEquals("(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = propertyCondition(property, ConditionType.LIKE, asList(2.2, 3.2, 4.2));
    assertEquals("(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionChar() {
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.CHAR);
    Condition testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = propertyCondition(property, ConditionType.LIKE, 'a');
    assertEquals("colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, 'a');
    assertEquals("colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = propertyCondition(property, ConditionType.WITHIN_RANGE, asList('a', 'd'));
    assertEquals("(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = propertyCondition(property, ConditionType.OUTSIDE_RANGE, asList('d', 'f'));
    assertEquals("(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = propertyCondition(property, ConditionType.LIKE, asList('a', 'b', 'c'));
    assertEquals("(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionBoolean() {
    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.BOOLEAN);
    Condition testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //string, =
    testCrit = propertyCondition(property, ConditionType.LIKE, false, false);
    assertEquals("colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, false, false);
    assertEquals("colName <> ?", testCrit.getWhereClause());
  }

  @Test
  public void conditionDate() throws Exception {
    final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DateFormats.SHORT_DASH);

    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DATE);
    Condition testCrit = propertyCondition(property, ConditionType.LIKE, null);
    assertEquals("colName is null", testCrit.getWhereClause());

    testCrit = propertyCondition(property, ConditionType.LIKE, (Object[]) null);
    assertEquals("colName is null", testCrit.getWhereClause());

    //string, =
    final LocalDate value = LocalDate.parse("10-12-2004", dateFormat);
    testCrit = propertyCondition(property, ConditionType.LIKE, value);
    String requiredValue = "colName = ?";
    assertEquals(requiredValue, testCrit.getWhereClause());

    //string, <>
    testCrit = propertyCondition(property, ConditionType.NOT_LIKE, value);
    requiredValue = "colName <> ?";
    assertEquals(requiredValue, testCrit.getWhereClause());

    //string, between
    final LocalDate value2 = LocalDate.parse("10-09-2001", dateFormat);
    testCrit = propertyCondition(property, ConditionType.WITHIN_RANGE, asList(value, value2));
    requiredValue = "(colName >= ? and colName <= ?)";
    assertEquals(requiredValue, testCrit.getWhereClause());

    //string, outside
    testCrit = propertyCondition(property, ConditionType.OUTSIDE_RANGE, asList(value, value2));
    requiredValue = "(colName <= ? or colName >= ?)";
    assertEquals(requiredValue, testCrit.getWhereClause());

    //string, in
    final LocalDate value3 = LocalDate.parse("12-10-2001", dateFormat);
    testCrit = propertyCondition(property, ConditionType.LIKE, asList(value, value2, value3));
    requiredValue = "(colName in (?, ?, ?))";
    assertEquals(requiredValue, testCrit.getWhereClause());
  }

  @Test
  public void conditionSet() {
    final Property.ColumnProperty property1 = Properties.columnProperty("colName1", Types.VARCHAR);
    final Property.ColumnProperty property2 = Properties.columnProperty("colName2", Types.INTEGER);
    final Condition condition1 = propertyCondition(property1, ConditionType.LIKE, "val%ue");
    final Condition condition2 = propertyCondition(property2, ConditionType.LESS_THAN, 10);
    final Condition.Set set = Conditions.conditionSet(Conjunction.OR, condition1, condition2);
    assertEquals("(colName1 like ? or colName2 <= ?)", set.getWhereClause());

    final Property.ColumnProperty property3 = Properties.columnProperty("colName3", Types.DOUBLE);
    final Condition condition3 = propertyCondition(property3, ConditionType.NOT_LIKE, 34.5);
    final Condition.Set set2 = Conditions.conditionSet(Conjunction.AND, set, condition3);
    assertEquals("((colName1 like ? or colName2 <= ?) and colName3 <> ?)", set2.getWhereClause());

    final Property.ColumnProperty property4 = Properties.columnProperty("colName4", Types.CHAR);
    final Condition condition4 = propertyCondition(property4, ConditionType.LIKE, asList('a', 'b', 'c'));
    final Condition.Set set3 = Conditions.conditionSet(Conjunction.OR, set2, condition4);
    assertEquals("(((colName1 like ? or colName2 <= ?) and colName3 <> ?)"
            + " or (colName4 in (?, ?, ?)))", set3.getWhereClause());
  }

  @Test
  public void stringConditionWithoutValue() {
    final String crit = "id = 1";
    final Condition condition = Conditions.stringCondition(crit);
    assertEquals(crit, condition.getWhereClause());
    assertEquals(0, condition.getProperties().size());
    assertEquals(0, condition.getValues().size());
  }

  @Test
  public void stringConditionWithValue() {
    final String crit = "id = ?";
    final Condition condition = Conditions.stringCondition(crit, singletonList(1),
            singletonList(new TestDomain().getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ID)));
    assertEquals(crit, condition.getWhereClause());
    assertEquals(1, condition.getProperties().size());
    assertEquals(1, condition.getValues().size());
  }

  @Test
  public void stringConditionNullConditionString() {
    assertThrows(NullPointerException.class, () -> Conditions.stringCondition(null));
  }

  @Test
  public void stringConditionNullValues() {
    assertThrows(NullPointerException.class, () -> Conditions.stringCondition("some is null", null,
            emptyList()));
  }

  @Test
  public void stringConditionNullKeys() {
    assertThrows(NullPointerException.class, () -> Conditions.stringCondition("some is null", emptyList(), null));
  }

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final Property.ColumnProperty id = new TestDomain().getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_ID);
    final Condition condition = Conditions.conditionSet(Conjunction.AND,
            Conditions.stringCondition("test", asList("val1", "val2"), asList(id, id)),
            Conditions.stringCondition("testing", asList("val1", "val2"), asList(id, id)));
    deserialize(serialize(condition));
  }

  private static class TestCondition implements Condition {
    private final TestDomain domain = new TestDomain();
    @Override
    public String getWhereClause() {
      return "condition";
    }

    @Override
    public List getValues() {
      return singletonList(1);
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      return singletonList(domain.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID));
    }
  }
}
