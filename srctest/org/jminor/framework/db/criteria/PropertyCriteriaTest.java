/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.sql.Types;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class PropertyCriteriaTest {

  public PropertyCriteriaTest() {
    TestDomain.init();
  }

  @Test
  public void conditionString() {
    //string, is null
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.VARCHAR);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, (Object) null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    String value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit", "colName like ?", testCrit.getWhereClause());

    //string, like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit",  "colName like ?", testCrit.getWhereClause());

    //string, <>
    value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit", "colName not like ?", testCrit.getWhereClause());

    //string, not like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit",  "colName not like ?", testCrit.getWhereClause());

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, Arrays.asList(value, value2, value3));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());

    //
    //
    //case insensitive
    //
    //
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, false, value);
    assertEquals("Condition should fit", "upper(colName) like upper(?)", testCrit.getWhereClause());

    //string, like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, false, value);
    assertEquals("Condition should fit",  "upper(colName) like upper(?)", testCrit.getWhereClause());

    //string, <>
    value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, false, value);
    assertEquals("Condition should fit", "upper(colName) not like upper(?)", testCrit.getWhereClause());

    //string, not like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, false, value);
    assertEquals("Condition should fit",  "upper(colName) not like upper(?)", testCrit.getWhereClause());

    //string, between
    value = "min";
    value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, false, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(upper(colName) >= upper(?) and upper(colName) <= upper(?))", testCrit.getWhereClause());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, false, Arrays.asList(value, value2));
    assertEquals("Condition should fit",  "(upper(colName) <= upper(?) or upper(colName) >= upper(?))", testCrit.getWhereClause());

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, false, Arrays.asList(value, value2, value3));
    assertEquals("Condition should fit", "(upper(colName) in (upper(?), upper(?), upper(?)))", testCrit.getWhereClause());
  }

  @Test
  public void conditionInt() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.INTEGER);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 124);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, 124);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, Arrays.asList(2, 4));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, Arrays.asList(2, 4));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, Arrays.asList(2, 3, 4));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionDouble() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DOUBLE);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 124.2);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, 124.2);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, Arrays.asList(2.2, 4.2));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, Arrays.asList(2.2, 4.2));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, Arrays.asList(2.2, 3.2, 4.2));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionChar() {
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.CHAR);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //int, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 'a');
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, 'a');
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());

    //between
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, Arrays.asList('a', 'd'));
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.getWhereClause());

    //outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, Arrays.asList('d', 'f'));
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.getWhereClause());

    //in
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, Arrays.asList('a', 'b', 'c'));
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.getWhereClause());
  }

  @Test
  public void conditionBoolean() {
    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.BOOLEAN);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, false, false);
    assertEquals("Condition should fit", "colName = ?", testCrit.getWhereClause());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, false, false);
    assertEquals("Condition should fit", "colName <> ?", testCrit.getWhereClause());
  }

  @Test
  public void conditionDate() throws Exception {
    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DASH);

    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DATE);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, (Object[]) null);
    assertEquals("Condition should fit", "colName is null", testCrit.getWhereClause());

    //string, =
    final Date value = dateFormat.parse("10-12-2004");
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value);
    String requiredValue = "colName = ?";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, <>
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, value);
    requiredValue = "colName <> ?";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, between
    final Date value2 = dateFormat.parse("10-09-2001");
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, Arrays.asList(value, value2));
    requiredValue = "(colName >= ? and colName <= ?)";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, Arrays.asList(value, value2));
    requiredValue =  "(colName <= ? or colName >= ?)";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());

    //string, in
    final Date value3 = dateFormat.parse("12-10-2001");
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, Arrays.asList(value, value2, value3));
    requiredValue = "(colName in (?, ?, ?))";
    assertEquals("Condition should fit", requiredValue, testCrit.getWhereClause());
  }

  @Test
  public void conditionSet() {
    final Property.ColumnProperty property1 = Properties.columnProperty("colName1", Types.VARCHAR);
    final Property.ColumnProperty property2 = Properties.columnProperty("colName2", Types.INTEGER);
    final Criteria<Property.ColumnProperty> criteria1 = EntityCriteriaUtil.propertyCriteria(property1, SearchType.LIKE, "value");
    final Criteria<Property.ColumnProperty> criteria2 = EntityCriteriaUtil.propertyCriteria(property2, SearchType.LESS_THAN, 10);
    final CriteriaSet<Property.ColumnProperty> set = new CriteriaSet<>(Conjunction.OR, criteria1, criteria2);
    assertEquals("Set condition should fit", "(colName1 like ? or colName2 <= ?)", set.getWhereClause());

    final Property.ColumnProperty property3 = Properties.columnProperty("colName3", Types.DOUBLE);
    final Criteria<Property.ColumnProperty> criteria3 = EntityCriteriaUtil.propertyCriteria(property3, SearchType.NOT_LIKE, 34.5);
    final CriteriaSet<Property.ColumnProperty> set2 = new CriteriaSet<>(Conjunction.AND, set, criteria3);
    assertEquals("Set condition should fit", "((colName1 like ? or colName2 <= ?) and colName3 <> ?)",
            set2.getWhereClause());

    final Property.ColumnProperty property4 = Properties.columnProperty("colName4", Types.CHAR);
    final Criteria<Property.ColumnProperty> criteria4 = EntityCriteriaUtil.propertyCriteria(property4, SearchType.LIKE, Arrays.asList('a', 'b', 'c'));
    final CriteriaSet set3 = new CriteriaSet<>(Conjunction.OR, set2, criteria4);
    assertEquals("Set condition should fit", "(((colName1 like ? or colName2 <= ?) and colName3 <> ?)"
            + " or (colName4 in (?, ?, ?)))", set3.getWhereClause());
  }
}
