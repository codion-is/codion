/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.model;

import org.jminor.common.Constants;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.DbUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.framework.demos.empdept.model.EmpDept;

import junit.framework.TestCase;

import java.util.Date;

public class TestPropertyCriteria extends TestCase {

  public TestPropertyCriteria(String name) {
    super(name);
    new EmpDept();
  }

  public void testConditionEntity() {
    final Property property = new Property.EntityProperty("colName", "entity", EmpDept.T_DEPARTMENT,
            new Property("entityId", Type.INT));
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "entityId is null", testCrit.toString());

    final Entity dept = new Entity(EmpDept.T_DEPARTMENT);
    dept.setValue(EmpDept.DEPARTMENT_ID, 42);

    testCrit = new PropertyCriteria(property, SearchType.EXACT, dept);
    assertEquals("Condition should fit", "entityId = 42", testCrit.toString());

    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, dept);
    assertEquals("Condition should fit", "entityId <> 42", testCrit.toString());
  }

  public void testConditionString() {
    //string, is null
    final Property property = new Property("colName", Type.STRING);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    testCrit = new PropertyCriteria(property, SearchType.EXACT, "");
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //string, =
    String value = "value";
    testCrit = new PropertyCriteria(property, SearchType.EXACT, value);
    assertEquals("Condition should fit", "colName = '" + value + "'", testCrit.toString());

    //string, like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.EXACT, value);
    assertEquals("Condition should fit",  "colName like '" + value + "'", testCrit.getConditionString());

    //string, <>
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, value);
    assertEquals("Condition should fit", "colName <> '" + value + "'", testCrit.getConditionString());

    //string, not like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, value);
    assertEquals("Condition should fit",  "colName not like '" + value + "'", testCrit.getConditionString());

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_INSIDE, value, value2);
    assertEquals("Condition should fit",  "(colName >= '" + value + "' and colName <= '"+value2+"')", testCrit.toString());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_OUTSIDE, value, value2);
    assertEquals("Condition should fit",  "(colName <= '" + value + "' or colName >= '"+value2+"')", testCrit.toString());

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = new PropertyCriteria(property, SearchType.IN_LIST, value, value2, value3);
    assertEquals("Condition should fit", "colName in ('"+value+"', '"+value2+"', '"+value3+"')", testCrit.toString());

    //
    //
    //case insensitive
    //
    //
    testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null}).setCaseSensitive(false);
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    testCrit = new PropertyCriteria(property, SearchType.EXACT, "").setCaseSensitive(false);
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //string, =
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.EXACT, value).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) = upper('" + value + "')", testCrit.toString());

    //string, like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.EXACT, value).setCaseSensitive(false);
    assertEquals("Condition should fit",  "upper(colName) like upper('" + value + "')", testCrit.getConditionString());

    //string, <>
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, value).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) <> upper('" + value + "')", testCrit.getConditionString());

    //string, not like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, value).setCaseSensitive(false);
    assertEquals("Condition should fit",  "upper(colName) not like upper('" + value + "')", testCrit.getConditionString());

    //string, between
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_INSIDE, value, value2).setCaseSensitive(false);
    assertEquals("Condition should fit",  "(upper(colName) >= upper('" + value
            + "') and upper(colName) <= upper('" +value2+ "'))", testCrit.toString());

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_OUTSIDE, value, value2).setCaseSensitive(false);
    assertEquals("Condition should fit",  "(upper(colName) <= upper('" + value
            + "') or upper(colName) >= upper('"+value2+"'))", testCrit.toString());

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = new PropertyCriteria(property, SearchType.IN_LIST, value, value2, value3).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) in (upper('" + value + "'), upper('" + value2
            + "'), upper('" + value3 + "'))", testCrit.toString());
  }

  public void testConditionInt() {
    //int, =
    final Property property = new Property("colName", Type.INT);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.EXACT, 124);
    assertEquals("Condition should fit", "colName = 124", testCrit.toString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, 124);
    assertEquals("Condition should fit", "colName <> 124", testCrit.toString());

    //between
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_INSIDE, 2, 4);
    assertEquals("Condition should fit",  "(colName >= 2 and colName <= 4)", testCrit.toString());

    //outside
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_OUTSIDE, 2, 4);
    assertEquals("Condition should fit",  "(colName <= 2 or colName >= 4)", testCrit.toString());

    //in
    testCrit = new PropertyCriteria(property, SearchType.IN_LIST, 2, 3, 4);
    assertEquals("Condition should fit", "colName in (2, 3, 4)", testCrit.toString());
  }



  public void testConditionDouble() {
    //int, =
    final Property property = new Property("colName", Type.DOUBLE);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.EXACT, 124.2);
    assertEquals("Condition should fit", "colName = 124.2", testCrit.toString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, 124.2);
    assertEquals("Condition should fit", "colName <> 124.2", testCrit.toString());

    //between
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_INSIDE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName >= 2.2 and colName <= 4.2)", testCrit.toString());

    //outside
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_OUTSIDE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName <= 2.2 or colName >= 4.2)", testCrit.toString());

    //in
    testCrit = new PropertyCriteria(property, SearchType.IN_LIST, 2.2, 3.2, 4.2);
    assertEquals("Condition should fit", "colName in (2.2, 3.2, 4.2)", testCrit.toString());
  }


  public void testConditionChar() {
    final Property property = new Property("colName", Type.CHAR);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.EXACT, 'a');
    assertEquals("Condition should fit", "colName = 'a'", testCrit.toString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, 'a');
    assertEquals("Condition should fit", "colName <> 'a'", testCrit.toString());

    //between
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_INSIDE, 'a', 'd');
    assertEquals("Condition should fit",  "(colName >= 'a' and colName <= 'd')", testCrit.toString());

    //outside
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_OUTSIDE, 'd', 'f');
    assertEquals("Condition should fit",  "(colName <= 'd' or colName >= 'f')", testCrit.toString());

    //in
    testCrit = new PropertyCriteria(property, SearchType.IN_LIST, 'a', 'b', 'c');
    assertEquals("Condition should fit", "colName in ('a', 'b', 'c')", testCrit.toString());
  }

  public void testConditionBoolean() {
    //string, =
    final Property property = new Property("colName", Type.BOOLEAN);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //string, =
    testCrit = new PropertyCriteria(property, SearchType.EXACT, Type.Boolean.FALSE);
    assertEquals("Condition should fit", "colName = 0", testCrit.toString());

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, Type.Boolean.TRUE);
    assertEquals("Condition should fit", "colName <> 1", testCrit.toString());
  }

  public void testConditionDate() throws Exception {
    //string, =
    final Property property = new Property("colName", Type.SHORT_DATE);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.EXACT, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    testCrit = new PropertyCriteria(property, SearchType.EXACT, Constants.TIMESTAMP_NULL_VALUE);
    assertEquals("Condition should fit", "colName is null", testCrit.toString());

    //string, =
    Date value = ShortDashDateFormat.get().parse("10-12-2004");
    testCrit = new PropertyCriteria(property, SearchType.EXACT, value);
    String requiredValue =  DbUtil.isMySQL()
            ? "colName = str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y')"
            : "colName = to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
    assertEquals("Condition should fit", requiredValue, testCrit.toString());

    //string, <>
    testCrit = new PropertyCriteria(property, SearchType.NOT_EXACT, value);
    requiredValue =  DbUtil.isMySQL()
            ? "colName <> str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y')"
            : "colName <> to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
    assertEquals("Condition should fit", requiredValue, testCrit.toString());

    //string, between
    Date value2 = ShortDashDateFormat.get().parse("10-09-2001");
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_INSIDE, value, value2);
    requiredValue =  DbUtil.isMySQL()
            ? "(colName >= str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y') and "
      + "colName <= str_to_date('" + ShortDashDateFormat.get().format(value2) + "', '%d-%m-%Y'))"
            : "(colName >= to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY') and "
      + "colName <= to_date('" + ShortDashDateFormat.get().format(value2) + "', 'DD-MM-YYYY'))";
    assertEquals("Condition should fit", requiredValue, testCrit.toString());

    //string, outside
    testCrit = new PropertyCriteria(property, SearchType.MIN_MAX_OUTSIDE, value, value2);
    requiredValue =  DbUtil.isMySQL()
            ? "(colName <= str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y') or "
      + "colName >= str_to_date('" + ShortDashDateFormat.get().format(value2) + "', '%d-%m-%Y'))"
            : "(colName <= to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY') or "
      + "colName >= to_date('" + ShortDashDateFormat.get().format(value2) + "', 'DD-MM-YYYY'))";
    assertEquals("Condition should fit", requiredValue, testCrit.toString());

    //string, in
    final Date value3 = ShortDashDateFormat.get().parse("12-10-2001");
    testCrit = new PropertyCriteria(property, SearchType.IN_LIST, value, value2, value3);
    final String expected = DbUtil.isMySQL()
            ? "colName in (str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y')," +
                 " str_to_date('" + ShortDashDateFormat.get().format(value2) + "', '%d-%m-%Y')," +
                 " str_to_date('" + ShortDashDateFormat.get().format(value3) + "', '%d-%m-%Y'))"
            : "colName in (to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')," +
                 " to_date('" + ShortDashDateFormat.get().format(value2) + "', 'DD-MM-YYYY')," +
                 " to_date('" + ShortDashDateFormat.get().format(value3) + "', 'DD-MM-YYYY'))";
    assertEquals("Condition should fit", expected, testCrit.toString());
  }

  public void testConditionSet() {
    final Property property1 = new Property("colName1", Type.STRING);
    final Property property2 = new Property("colName2", Type.INT);
    final PropertyCriteria criteria1 = new PropertyCriteria(property1, SearchType.EXACT, "value");
    final PropertyCriteria criteria2 = new PropertyCriteria(property2, SearchType.MAX, 10);
    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR, criteria1, criteria2);
    assertEquals("Set condition should fit", "(colName1 = 'value' or colName2 <= 10)", set.toString());

    final Property property3 = new Property("colName3", Type.DOUBLE);
    final PropertyCriteria criteria3 = new PropertyCriteria(property3, SearchType.NOT_EXACT, 34.5);
    final CriteriaSet set2 = new CriteriaSet(CriteriaSet.Conjunction.AND, set, criteria3);
    assertEquals("Set condition should fit", "((colName1 = 'value' or colName2 <= 10) and colName3 <> 34.5)",
            set2.toString());

    final Property property4 = new Property("colName4", Type.CHAR);
    final PropertyCriteria criteria4 = new PropertyCriteria(property4, SearchType.IN_LIST, 'a', 'b', 'c');
    final CriteriaSet set3 = new CriteriaSet(CriteriaSet.Conjunction.OR, set2, criteria4);
    assertEquals("Set condition should fit", "(((colName1 = 'value' or colName2 <= 10) and colName3 <> 34.5)"
            + " or colName4 in ('a', 'b', 'c'))", set3.toString());
  }
}
