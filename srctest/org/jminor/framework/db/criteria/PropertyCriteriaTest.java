/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.sql.Types;
import java.text.DateFormat;
import java.util.Date;

public class PropertyCriteriaTest {

  public PropertyCriteriaTest() {
    new EmpDept();
  }

  @Test
  public void conditionString() {
    //string, is null
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.VARCHAR);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, (Object) null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    String value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit", "colName like ?", testCrit.asString());

    //string, like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit",  "colName like ?", testCrit.asString());

    //string, <>
    value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit", "colName not like ?", testCrit.asString());

    //string, not like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit",  "colName not like ?", testCrit.asString());

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, value, value2);
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.asString());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2);
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.asString());

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value, value2, value3);
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.asString());

    //
    //
    //case insensitive
    //
    //
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.LIKE, (Object) null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.LIKE, value);
    assertEquals("Condition should fit", "upper(colName) like upper(?)", testCrit.asString());

    //string, like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.LIKE, value);
    assertEquals("Condition should fit",  "upper(colName) like upper(?)", testCrit.asString());

    //string, <>
    value = "value";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit", "upper(colName) not like upper(?)", testCrit.asString());

    //string, not like
    value = "val%ue";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit",  "upper(colName) not like upper(?)", testCrit.asString());

    //string, between
    value = "min";
    value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.WITHIN_RANGE, value, value2);
    assertEquals("Condition should fit",  "(upper(colName) >= upper(?) and upper(colName) <= upper(?))", testCrit.asString());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.OUTSIDE_RANGE, value, value2);
    assertEquals("Condition should fit",  "(upper(colName) <= upper(?) or upper(colName) >= upper(?))", testCrit.asString());

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.LIKE, value, value2, value3);
    assertEquals("Condition should fit", "(upper(colName) in (upper(?), upper(?), upper(?)))", testCrit.asString());
  }

  @Test
  public void conditionInt() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.INTEGER);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //int, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 124);
    assertEquals("Condition should fit", "colName = ?", testCrit.asString());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, 124);
    assertEquals("Condition should fit", "colName <> ?", testCrit.asString());

    //between
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, 2, 4);
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.asString());

    //outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, 2, 4);
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.asString());

    //in
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 2, 3, 4);
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.asString());
  }

  @Test
  public void conditionDouble() {
    //int, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DOUBLE);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //int, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 124.2);
    assertEquals("Condition should fit", "colName = ?", testCrit.asString());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, 124.2);
    assertEquals("Condition should fit", "colName <> ?", testCrit.asString());

    //between
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.asString());

    //outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.asString());

    //in
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 2.2, 3.2, 4.2);
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.asString());
  }

  @Test
  public void conditionChar() {
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.CHAR);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //int, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 'a');
    assertEquals("Condition should fit", "colName = ?", testCrit.asString());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, 'a');
    assertEquals("Condition should fit", "colName <> ?", testCrit.asString());

    //between
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, 'a', 'd');
    assertEquals("Condition should fit",  "(colName >= ? and colName <= ?)", testCrit.asString());

    //outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, 'd', 'f');
    assertEquals("Condition should fit",  "(colName <= ? or colName >= ?)", testCrit.asString());

    //in
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, 'a', 'b', 'c');
    assertEquals("Condition should fit", "(colName in (?, ?, ?))", testCrit.asString());
  }

  @Test
  public void conditionBoolean() {
    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.BOOLEAN);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    testCrit = EntityCriteriaUtil.propertyCriteria(property, false, SearchType.LIKE, false);
    assertEquals("Condition should fit", "colName = ?", testCrit.asString());

    //<=>=
    testCrit = EntityCriteriaUtil.propertyCriteria(property, true, SearchType.NOT_LIKE, false);
    assertEquals("Condition should fit", "colName <> ?", testCrit.asString());
  }

  @Test
  public void conditionDate() throws Exception {
    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DASH);

    //string, =
    final Property.ColumnProperty property = Properties.columnProperty("colName", Types.DATE);
    Criteria<Property.ColumnProperty> testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, (Object[]) null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    Date value = dateFormat.parse("10-12-2004");
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value);
    String requiredValue = "colName = ?";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, <>
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.NOT_LIKE, value);
    requiredValue = "colName <> ?";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, between
    Date value2 = dateFormat.parse("10-09-2001");
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.WITHIN_RANGE, value, value2);
    requiredValue = "(colName >= ? and colName <= ?)";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, outside
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2);
    requiredValue =  "(colName <= ? or colName >= ?)";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, in
    final Date value3 = dateFormat.parse("12-10-2001");
    testCrit = EntityCriteriaUtil.propertyCriteria(property, SearchType.LIKE, value, value2, value3);
    requiredValue = "(colName in (?, ?, ?))";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());
  }

  @Test
  public void conditionSet() {
    final Property.ColumnProperty property1 = Properties.columnProperty("colName1", Types.VARCHAR);
    final Property.ColumnProperty property2 = Properties.columnProperty("colName2", Types.INTEGER);
    final Criteria<Property.ColumnProperty> criteria1 = EntityCriteriaUtil.propertyCriteria(property1, SearchType.LIKE, "value");
    final Criteria<Property.ColumnProperty> criteria2 = EntityCriteriaUtil.propertyCriteria(property2, SearchType.AT_LEAST, 10);
    final CriteriaSet set = new CriteriaSet<Property.ColumnProperty>(Conjunction.OR, criteria1, criteria2);
    assertEquals("Set condition should fit", "(colName1 like ? or colName2 <= ?)", set.asString());

    final Property.ColumnProperty property3 = Properties.columnProperty("colName3", Types.DOUBLE);
    final Criteria<Property.ColumnProperty> criteria3 = EntityCriteriaUtil.propertyCriteria(property3, SearchType.NOT_LIKE, 34.5);
    final CriteriaSet set2 = new CriteriaSet<Property.ColumnProperty>(Conjunction.AND, set, criteria3);
    assertEquals("Set condition should fit", "((colName1 like ? or colName2 <= ?) and colName3 <> ?)",
            set2.asString());

    final Property.ColumnProperty property4 = Properties.columnProperty("colName4", Types.CHAR);
    final Criteria<Property.ColumnProperty> criteria4 = EntityCriteriaUtil.propertyCriteria(property4, SearchType.LIKE, 'a', 'b', 'c');
    final CriteriaSet set3 = new CriteriaSet<Property.ColumnProperty>(Conjunction.OR, set2, criteria4);
    assertEquals("Set condition should fit", "(((colName1 like ? or colName2 <= ?) and colName3 <> ?)"
            + " or (colName4 in (?, ?, ?)))", set3.asString());
  }
}
