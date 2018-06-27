/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.DateFormats;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class PropertyConditionTest {

  private static final TestDomain entities = new TestDomain();
  private static final EntityConditions entityConditions = new EntityConditions(entities);

  @Test
  public void conditionString() {
    //string, is null
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.VARCHAR);
    Condition<Property.ColumnProperty> testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, (Object) null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    String value = "value";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, value);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //string, like
    value = "val%ue";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, value);
    assertEquals("Condition should fit",  "colName like ?", testCrit.getWhereClause());

    //string, <>
    value = "value";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, value);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //string, not like
    value = "val%ue";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, value);
    assertEquals("Condition should fit",  "colName not like ?", testCrit.getWhereClause());

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.WITHIN_RANGE, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.OUTSIDE_RANGE, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, Arrays.asList(value, value2, value3));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());

    //
    //
    //case insensitive
    //
    //
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    value = "value";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, false, value);
    assertEquals("Condition should fit", "upper(colName) = upper(?)", testCrit.getWhereClause());

    //string, like
    value = "val%ue";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, false, value);
    assertEquals("Condition should fit",  "upper(colName) like upper(?)", testCrit.getWhereClause());

    //string, <>
    value = "value";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, false, value);
    assertEquals("Condition should fit", "upper(colName) <> upper(?)", testCrit.getWhereClause());

    //string, not like
    value = "val%ue";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, false, value);
    assertEquals("Condition should fit",  "upper(colName) not like upper(?)", testCrit.getWhereClause());

    //string, between
    value = "min";
    value2 = "max";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.WITHIN_RANGE, false, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(upper(colName) >= upper(?) and upper(colName) <= upper(?))", testCrit.getWhereClause());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.OUTSIDE_RANGE, false, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(upper(colName) <= upper(?) or upper(colName) >= upper(?))", testCrit.getWhereClause());

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, false, Arrays.asList(value, value2, value3));
    assertEquals("Condition should fit", "(upper(colName) in (upper(?), upper(?), upper(?)))", testCrit.getWhereClause());
  }

  @Test
  public void conditionInt() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.INTEGER);
    Condition<Property.ColumnProperty> testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, 124);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, 124);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = entityConditions.propertyCondition(property, Condition.Type.WITHIN_RANGE, Arrays.asList(2, 4));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = entityConditions.propertyCondition(property, Condition.Type.OUTSIDE_RANGE, Arrays.asList(2, 4));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, Arrays.asList(2, 3, 4));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionDouble() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DOUBLE);
    Condition<Property.ColumnProperty> testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, 124.2);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, 124.2);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = entityConditions.propertyCondition(property, Condition.Type.WITHIN_RANGE, Arrays.asList(2.2, 4.2));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = entityConditions.propertyCondition(property, Condition.Type.OUTSIDE_RANGE, Arrays.asList(2.2, 4.2));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, Arrays.asList(2.2, 3.2, 4.2));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionChar() {
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.CHAR);
    Condition<Property.ColumnProperty> testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, 'a');
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, 'a');
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = entityConditions.propertyCondition(property, Condition.Type.WITHIN_RANGE, Arrays.asList('a', 'd'));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = entityConditions.propertyCondition(property, Condition.Type.OUTSIDE_RANGE, Arrays.asList('d', 'f'));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, Arrays.asList('a', 'b', 'c'));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionBoolean() {
    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.BOOLEAN);
    Condition<Property.ColumnProperty> testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, false, false);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, false, false);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());
  }

  @Test
  public void conditionDate() throws Exception {
    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DASH);

    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DATE);
    Condition<Property.ColumnProperty> testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, (Object[]) null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    final Date value = dateFormat.parse("10-12-2004");
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, value);
    String requiredValue = "colName = ?";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, <>
    testCrit = entityConditions.propertyCondition(property, Condition.Type.NOT_LIKE, value);
    requiredValue = "colName <> ?";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, between
    final Date value2 = dateFormat.parse("10-09-2001");
    testCrit = entityConditions.propertyCondition(property, Condition.Type.WITHIN_RANGE, Arrays.asList(value, value2));
    requiredValue = "(colName >= ? and colName <= ?)";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, outside
    testCrit = entityConditions.propertyCondition(property, Condition.Type.OUTSIDE_RANGE, Arrays.asList(value, value2));
    requiredValue =  "(colName <= ? or colName >= ?)";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, in
    final Date value3 = dateFormat.parse("12-10-2001");
    testCrit = entityConditions.propertyCondition(property, Condition.Type.LIKE, Arrays.asList(value, value2, value3));
    requiredValue = "(colName in (?, ?, ?))";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());
  }

  @Test
  public void conditionSet() {
    final Property.ColumnProperty property1 = Properties.columnProperty("colName1", Types.VARCHAR);
    final Property.ColumnProperty property2 = Properties.columnProperty("colName2", Types.INTEGER);
    final Condition<Property.ColumnProperty> condition1 = entityConditions.propertyCondition(property1, Condition.Type.LIKE, "val%ue");
    final Condition<Property.ColumnProperty> condition2 = entityConditions.propertyCondition(property2, Condition.Type.LESS_THAN, 10);
    final Condition.Set<Property.ColumnProperty> set = Conditions.conditionSet(Conjunction.OR, condition1, condition2);
    assertEquals("Set condition should fit", "(colName1 like ? or colName2 <= ?)", set.getWhereClause());

    final Property.ColumnProperty property3 = Properties.columnProperty("colName3", Types.DOUBLE);
    final Condition<Property.ColumnProperty> condition3 = entityConditions.propertyCondition(property3, Condition.Type.NOT_LIKE, 34.5);
    final Condition.Set<Property.ColumnProperty> set2 = Conditions.conditionSet(Conjunction.AND, set, condition3);
    assertEquals("Set condition should fit", "((colName1 like ? or colName2 <= ?) and colName3 <> ?)",
            set2.getWhereClause());

    final Property.ColumnProperty property4 = Properties.columnProperty("colName4", Types.CHAR);
    final Condition<Property.ColumnProperty> condition4 = entityConditions.propertyCondition(property4, Condition.Type.LIKE, Arrays.asList('a', 'b', 'c'));
    final Condition.Set set3 = Conditions.conditionSet(Conjunction.OR, set2, condition4);
    assertEquals("Set condition should fit", "(((colName1 like ? or colName2 <= ?) and colName3 <> ?)"
            + " or (colName4 in (?, ?, ?)))", set3.getWhereClause());
  }
}
