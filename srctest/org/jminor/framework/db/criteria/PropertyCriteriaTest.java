/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.sql.Types;
import java.text.DateFormat;
import java.util.Date;

public class PropertyCriteriaTest {

  private static final Database DATABASE = DatabaseProvider.createInstance();
  private static final Criteria.ValueProvider VALUE_PROVIDER = EntityCriteriaUtil.getCriteriaValueProvider();

  public PropertyCriteriaTest() {
    new EmpDept();
  }

  @Test
  public void conditionEntity() {
    final Property property = new Property.ForeignKeyProperty("colName", "entity", EmpDept.T_DEPARTMENT,
            new Property("entityId", Types.INTEGER));
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "entityId is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    final Entity dept = new Entity(EmpDept.T_DEPARTMENT);
    dept.setValue(EmpDept.DEPARTMENT_ID, 42);

    testCrit = new PropertyCriteria(property, SearchType.LIKE, dept);
    assertEquals("Condition should fit", "entityId = 42", testCrit.asString(DATABASE, VALUE_PROVIDER));

    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, dept);
    assertEquals("Condition should fit", "entityId <> 42", testCrit.asString(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void conditionString() {
    //string, is null
    final Property property = new Property("colName", Types.VARCHAR);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    testCrit = new PropertyCriteria(property, SearchType.LIKE, null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, =
    String value = "value";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit", "colName = '" + value + "'", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value);
    assertEquals("Condition should fit",  "colName like '" + value + "'", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, <>
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit", "colName <> '" + value + "'", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, not like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value);
    assertEquals("Condition should fit",  "colName not like '" + value + "'", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, between
    value = "min";
    String value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, value, value2);
    assertEquals("Condition should fit",  "(colName >= '" + value + "' and colName <= '"+value2+"')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2);
    assertEquals("Condition should fit",  "(colName <= '" + value + "' or colName >= '"+value2+"')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, in
    value = "min";
    value2 = "max";
    String value3 = "bla";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value, value2, value3);
    assertEquals("Condition should fit", "(colName in ('"+value+"', '"+value2+"', '"+value3+"'))", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //
    //
    //case insensitive
    //
    //
    testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null}).setCaseSensitive(false);
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    testCrit = new PropertyCriteria(property, SearchType.LIKE, null).setCaseSensitive(false);
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, =
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) = upper('" + value + "')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit",  "upper(colName) like upper('" + value + "')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, <>
    value = "value";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit", "upper(colName) <> upper('" + value + "')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, not like
    value = "val%ue";
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value).setCaseSensitive(false);
    assertEquals("Condition should fit",  "upper(colName) not like upper('" + value + "')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, between
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, value, value2).setCaseSensitive(false);
    assertEquals("Condition should fit",  "(upper(colName) >= upper('" + value
            + "') and upper(colName) <= upper('" +value2+ "'))", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, outside
    value = "min";
    value2 = "max";
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2).setCaseSensitive(false);
    assertEquals("Condition should fit",  "(upper(colName) <= upper('" + value
            + "') or upper(colName) >= upper('"+value2+"'))", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, in
    value = "min";
    value2 = "max";
    value3 = "bla";
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value, value2, value3).setCaseSensitive(false);
    assertEquals("Condition should fit", "(upper(colName) in (upper('" + value + "'), upper('" + value2
            + "'), upper('" + value3 + "')))", testCrit.asString(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void conditionInt() {
    //int, =
    final Property property = new Property("colName", Types.INTEGER);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 124);
    assertEquals("Condition should fit", "colName = 124", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, 124);
    assertEquals("Condition should fit", "colName <> 124", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //between
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, 2, 4);
    assertEquals("Condition should fit",  "(colName >= 2 and colName <= 4)", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, 2, 4);
    assertEquals("Condition should fit",  "(colName <= 2 or colName >= 4)", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //in
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 2, 3, 4);
    assertEquals("Condition should fit", "(colName in (2, 3, 4))", testCrit.asString(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void conditionDouble() {
    //int, =
    final Property property = new Property("colName", Types.DOUBLE);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 124.2);
    assertEquals("Condition should fit", "colName = 124.2", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, 124.2);
    assertEquals("Condition should fit", "colName <> 124.2", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //between
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName >= 2.2 and colName <= 4.2)", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, 2.2, 4.2);
    assertEquals("Condition should fit",  "(colName <= 2.2 or colName >= 4.2)", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //in
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 2.2, 3.2, 4.2);
    assertEquals("Condition should fit", "(colName in (2.2, 3.2, 4.2))", testCrit.asString(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void conditionChar() {
    final Property property = new Property("colName", Types.CHAR);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //int, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 'a');
    assertEquals("Condition should fit", "colName = 'a'", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, 'a');
    assertEquals("Condition should fit", "colName <> 'a'", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //between
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, 'a', 'd');
    assertEquals("Condition should fit",  "(colName >= 'a' and colName <= 'd')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, 'd', 'f');
    assertEquals("Condition should fit",  "(colName <= 'd' or colName >= 'f')", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //in
    testCrit = new PropertyCriteria(property, SearchType.LIKE, 'a', 'b', 'c');
    assertEquals("Condition should fit", "(colName in ('a', 'b', 'c'))", testCrit.asString(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void conditionBoolean() {
    //string, =
    final Property property = new Property("colName", Types.BOOLEAN);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //string, =
    testCrit = new PropertyCriteria(property, SearchType.LIKE, false);
    assertEquals("Condition should fit", "colName = 0", testCrit.asString(DATABASE, VALUE_PROVIDER));

    //<=>=
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, true);
    assertEquals("Condition should fit", "colName <> 1", testCrit.asString(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void conditionDate() throws Exception {
    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DASH);

    final Database database = DatabaseProvider.createInstance();
    //string, =
    final Property property = new Property("colName", Types.DATE);
    PropertyCriteria testCrit = new PropertyCriteria(property, SearchType.LIKE, new Object[] {null});
    assertEquals("Condition should fit", "colName is null", testCrit.asString(database, VALUE_PROVIDER));

    testCrit = new PropertyCriteria(property, SearchType.LIKE, (Object[]) null);
    assertEquals("Condition should fit", "colName is null", testCrit.asString(database, VALUE_PROVIDER));

    //string, =
    Date value = dateFormat.parse("10-12-2004");
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value);
    String requiredValue =  "colName = " + database.getSQLDateString(value, false);
    assertEquals("Condition should fit", requiredValue, testCrit.asString(database, VALUE_PROVIDER));

    //string, <>
    testCrit = new PropertyCriteria(property, SearchType.NOT_LIKE, value);
    requiredValue =  "colName <> " + database.getSQLDateString(value, false);
    assertEquals("Condition should fit", requiredValue, testCrit.asString(database, VALUE_PROVIDER));

    //string, between
    Date value2 = dateFormat.parse("10-09-2001");
    testCrit = new PropertyCriteria(property, SearchType.WITHIN_RANGE, value, value2);
    requiredValue =  "(colName >= " + database.getSQLDateString(value, false) + " and " +
            "colName <= " + database.getSQLDateString(value2, false) + ")";
    assertEquals("Condition should fit", requiredValue, testCrit.asString(database, VALUE_PROVIDER));

    //string, outside
    testCrit = new PropertyCriteria(property, SearchType.OUTSIDE_RANGE, value, value2);
    requiredValue =  "(colName <= " + database.getSQLDateString(value, false) + " or " +
            "colName >= " + database.getSQLDateString(value2, false) + ")";
    assertEquals("Condition should fit", requiredValue, testCrit.asString(database, VALUE_PROVIDER));

    //string, in
    final Date value3 = dateFormat.parse("12-10-2001");
    testCrit = new PropertyCriteria(property, SearchType.LIKE, value, value2, value3);
    requiredValue = "(colName in ("
            + database.getSQLDateString(value, false) + ", "
            + database.getSQLDateString(value2, false) + ", "
            + database.getSQLDateString(value3, false) + "))";
    assertEquals("Condition should fit", requiredValue, testCrit.asString(database, VALUE_PROVIDER));
  }

  @Test
  public void conditionSet() {
    final Property property1 = new Property("colName1", Types.VARCHAR);
    final Property property2 = new Property("colName2", Types.INTEGER);
    final PropertyCriteria criteria1 = new PropertyCriteria(property1, SearchType.LIKE, "value");
    final PropertyCriteria criteria2 = new PropertyCriteria(property2, SearchType.AT_LEAST, 10);
    final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.OR, criteria1, criteria2);
    assertEquals("Set condition should fit", "(colName1 = 'value' or colName2 <= 10)", set.asString(DATABASE, VALUE_PROVIDER));

    final Property property3 = new Property("colName3", Types.DOUBLE);
    final PropertyCriteria criteria3 = new PropertyCriteria(property3, SearchType.NOT_LIKE, 34.5);
    final CriteriaSet set2 = new CriteriaSet(CriteriaSet.Conjunction.AND, set, criteria3);
    assertEquals("Set condition should fit", "((colName1 = 'value' or colName2 <= 10) and colName3 <> 34.5)",
            set2.asString(DATABASE, VALUE_PROVIDER));

    final Property property4 = new Property("colName4", Types.CHAR);
    final PropertyCriteria criteria4 = new PropertyCriteria(property4, SearchType.LIKE, 'a', 'b', 'c');
    final CriteriaSet set3 = new CriteriaSet(CriteriaSet.Conjunction.OR, set2, criteria4);
    assertEquals("Set condition should fit", "(((colName1 = 'value' or colName2 <= 10) and colName3 <> 34.5)"
            + " or (colName4 in ('a', 'b', 'c')))", set3.asString(DATABASE, VALUE_PROVIDER));
  }
}
