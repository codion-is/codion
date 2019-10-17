/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.DateFormats;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.util.Arrays.asList;
import static org.jminor.framework.db.condition.EntityConditions.propertyCondition;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultConditionTest {

  @Test
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
}
