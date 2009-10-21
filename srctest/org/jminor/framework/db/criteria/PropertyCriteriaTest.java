/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.Database;
import org.jminor.common.db.dbms.Dbms;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import junit.framework.TestCase;

import java.util.Date;

public class PropertyCriteriaTest extends TestCase {

  static {
    EntityUtil.initializeDatabase(Database.createInstance());
  }

  public PropertyCriteriaTest() {
    new EmpDept();
  }

  public void testConditionEntity() {
    final Property property = new Property.ForeignKeyProperty("colName", "entity", EmpDept.T_DEPARTMENT,
            new Property("entityId", Type.INT));
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "entityId is null", testCrit.asString());

    final Entity dept = new Entity(EmpDept.T_DEPARTMENT);
    dept.setValue(EmpDept.DEPARTMENT_ID, 42);

    testCrit = new PropertyCriteria(property, SearchType.LIKE, dept);
    assertEquals("Condition should fit", "entityId = 42", testCrit.asString());

    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, dept);
    assertEquals("Condition should fit", "entityId <> 42", testCrit.asString());
  }

  public void testConditionString() {
    //string, is null
    final Property property = new Property("colName", Type.STRING);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    testCrit = new PropertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    String value = "value";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit", "colName = '" + value + "'", testCrit.asString());

    //string, like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit",  "colName like '" + value + "'", testCrit.asString());

    //string, <>
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit", "colName <> '" + value + "'", testCrit.asString());

    //string, not like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit",  "colName not like '" + value + "'", testCrit.asString());

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, value, value2);
    assertEquals("Condition should fit",  "(colName >= '" + value + "' and colName <= '"+value2+"')", testCrit.asString());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2);
    assertEquals("Condition should fit",  "(colName <= '" + value + "' or colName >= '"+value2+"')", testCrit.asString());

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value, value2, value3);
    assertEquals("Condition should fit", "(colName in ('"+value+"', '"+value2+"', '"+value3+"'))", testCrit.asString());

    //
    //
    //case insensitive
    //
    //
    testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null}).setCaseSensitive(false);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    testCrit = new PropertyCriteria(property, SearchType.LIKE, null).setCaseSensitive(false);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) = upper('" + value + "')", testCrit.asString());

    //string, like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit",  "upper(colName) like upper('" + value + "')", testCrit.asString());

    //string, <>
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) <> upper('" + value + "')", testCrit.asString());

    //string, not like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit",  "upper(colName) not like upper('" + value + "')", testCrit.asString());

    //string, between
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, value, value2).setCaseSensitive(false);
    assertEquals("Condition should fit",  "(upper(colName) >= upper('" + value
            + "') and upper(colName) <= upper('" +value2+ "'))", testCrit.asString());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2).setCaseSensitive(false);
    assertEquals("Condition should fit",  "(upper(colName) <= upper('" + value
            + "') or upper(colName) >= upper('"+value2+"'))", testCrit.asString());

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value, value2, value3).setCaseSensitive(false);
    assertEquals("Condition should fit", "(upper(colName) in (upper('" + value + "'), upper('" + value2
            + "'), upper('" + value3 + "')))", testCrit.asString());
  }

  public void testConditionInt() {
    //int, =
    final Property property = new Property("colName", Type.INT);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 124);
    assertEquals("Condition should fit", "colName = 124", testCrit.asString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, 124);
    assertEquals("Condition should fit", "colName <> 124", testCrit.asString());

    //between
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, 2, 4);
    assertEquals("Condition should fit",  "(colName >= 2 and colName <= 4)", testCrit.asString());

    //outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, 2, 4);
    assertEquals("Condition should fit",  "(colName <= 2 or colName >= 4)", testCrit.asString());

    //in
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 2, 3, 4);
    assertEquals("Condition should fit", "(colName in (2, 3, 4))", testCrit.asString());
  }



  public void testConditionDouble() {
    //int, =
    final Property property = new Property("colName", Type.DOUBLE);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 124.2);
    assertEquals("Condition should fit", "colName = 124.2", testCrit.asString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, 124.2);
    assertEquals("Condition should fit", "colName <> 124.2", testCrit.asString());

    //between
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName >= 2.2 and colName <= 4.2)", testCrit.asString());

    //outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName <= 2.2 or colName >= 4.2)", testCrit.asString());

    //in
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 2.2, 3.2, 4.2);
    assertEquals("Condition should fit", "(colName in (2.2, 3.2, 4.2))", testCrit.asString());
  }


  public void testConditionChar() {
    final Property property = new Property("colName", Type.CHAR);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 'a');
    assertEquals("Condition should fit", "colName = 'a'", testCrit.asString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, 'a');
    assertEquals("Condition should fit", "colName <> 'a'", testCrit.asString());

    //between
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, 'a', 'd');
    assertEquals("Condition should fit",  "(colName >= 'a' and colName <= 'd')", testCrit.asString());

    //outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, 'd', 'f');
    assertEquals("Condition should fit",  "(colName <= 'd' or colName >= 'f')", testCrit.asString());

    //in
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 'a', 'b', 'c');
    assertEquals("Condition should fit", "(colName in ('a', 'b', 'c'))", testCrit.asString());
  }

  public void testConditionBoolean() {
    //string, =
    final Property property = new Property("colName", Type.BOOLEAN);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, false);
    assertEquals("Condition should fit", "colName = 0", testCrit.asString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, true);
    assertEquals("Condition should fit", "colName <> 1", testCrit.asString());
  }

  public void testConditionDate() throws Exception {
    final Dbms database = Database.createInstance();
    //string, =
    final Property property = new Property("colName", Type.DATE);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    testCrit = new PropertyCriteria(property, SearchType.LIKE, (Object[]) null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString());

    //string, =
    Date value = new ShortDashDateFormat().parse("10-12-2004");
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value);
    String requiredValue =  "colName = " + database.getSQLDateString(value, false);
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, <>
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value);
    requiredValue =  "colName <> " + database.getSQLDateString(value, false);
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, between
    Date value2 = new ShortDashDateFormat().parse("10-09-2001");
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, value, value2);
    requiredValue =  "(colName >= " + database.getSQLDateString(value, false) + " and " +
            "colName <= " + database.getSQLDateString(value2, false) + ")";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2);
    requiredValue =  "(colName <= " + database.getSQLDateString(value, false) + " or " +
            "colName >= " + database.getSQLDateString(value2, false) + ")";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());

    //string, in
    final Date value3 = new ShortDashDateFormat().parse("12-10-2001");
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value, value2, value3);
    requiredValue = "(colName in ("
            + database.getSQLDateString(value, false) + ", "
            + database.getSQLDateString(value2, false) + ", "
            + database.getSQLDateString(value3, false) + "))";
    assertEquals("Condition should fit", requiredValue, testCrit.asString());
  }

  public void testConditionSet() {
    final Property property1 = new Property("colName1", Type.STRING);
    final Property property2 = new Property("colName2", Type.INT);
    final PropertyCriteria criteria1 = new PropertyCriteria(property1, SearchType.LIKE, "value");
    final PropertyCriteria criteria2 = new PropertyCriteria(property2, SearchType.AT_LEAST, 10);
    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR, criteria1, criteria2);
    assertEquals("Set condition should fit", "(colName1 = 'value' or colName2 <= 10)", set.asString());

    final Property property3 = new Property("colName3", Type.DOUBLE);
    final PropertyCriteria criteria3 = new PropertyCriteria(property3, SearchType.NOT_LIKE, 34.5);
    final CriteriaSet set2 = new CriteriaSet(CriteriaSet.Conjunction.AND, set, criteria3);
    assertEquals("Set condition should fit", "((colName1 = 'value' or colName2 <= 10) and colName3 <> 34.5)",
            set2.asString());

    final Property property4 = new Property("colName4", Type.CHAR);
    final PropertyCriteria criteria4 = new PropertyCriteria(property4, SearchType.LIKE, 'a', 'b', 'c');
    final CriteriaSet set3 = new CriteriaSet(CriteriaSet.Conjunction.OR, set2, criteria4);
    assertEquals("Set condition should fit", "(((colName1 = 'value' or colName2 <= 10) and colName3 <> 34.5)"
            + " or (colName4 in ('a', 'b', 'c')))", set3.asString());
  }
}
