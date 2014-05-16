/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public final class DefaultEntityLookupModelTest {

  private DefaultEntityLookupModel lookupModel;
  private Collection<Property.ColumnProperty> lookupProperties;

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new DefaultEntityLookupModel(null, DefaultEntityConnectionTest.CONNECTION_PROVIDER, new ArrayList<Property.ColumnProperty>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new DefaultEntityLookupModel(EmpDept.T_EMPLOYEE, null, new ArrayList<Property.ColumnProperty>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullLookupProperties() {
    new DefaultEntityLookupModel(EmpDept.T_EMPLOYEE, DefaultEntityConnectionTest.CONNECTION_PROVIDER, null);
  }

  @Test
  public void theRest() {
    lookupModel.setDescription("description");
    assertEquals("description", lookupModel.getDescription());
    assertNotNull(lookupModel.getConnectionProvider());
    assertTrue(lookupModel.getLookupProperties().containsAll(lookupProperties));
    assertTrue(lookupModel.isMultipleSelectionAllowed());
    lookupModel.setMultipleSelectionAllowed(false);
    assertFalse(lookupModel.isMultipleSelectionAllowed());
    assertFalse(lookupModel.isCaseSensitive());
    assertTrue(lookupModel.isWildcardPostfix());
    assertTrue(lookupModel.isWildcardPrefix());
    assertNotNull(lookupModel.getWildcard());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSelectedEntitiesMultipleNotAllowed() {
    lookupModel.setMultipleSelectionAllowed(false);
    final Collection<Entity> entities = Arrays.asList(Entities.entity(EmpDept.T_EMPLOYEE), Entities.entity(EmpDept.T_EMPLOYEE));
    lookupModel.setSelectedEntities(entities);
  }

  @Test
  public void setToStringProvider() {
    lookupModel.setToStringProvider(new Entity.ToString() {
      @Override
      public String toString(final Entity entity) {
        return entity.getValueAsString(EmpDept.EMPLOYEE_JOB);
      }
    });
    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_NAME, "Darri");
    employee.setValue(EmpDept.EMPLOYEE_JOB, "Test");
    lookupModel.setSelectedEntities(Arrays.asList(employee));
    assertEquals(lookupModel.getSearchString(), "Test");
    lookupModel.setToStringProvider(null);
    lookupModel.setSelectedEntities(Arrays.asList(employee));
    assertEquals(lookupModel.getSearchString(), "Darri");
  }

  @Test
  public void lookupModel() throws Exception {
    lookupModel.setMultipleSelectionAllowed(true);
    lookupModel.setWildcard("%");
    lookupModel.setSearchString("joh");
    assertFalse(lookupModel.searchStringRepresentsSelected());
    List<Entity> result = lookupModel.performQuery();
    assertTrue("Result should not be empty", result.size() > 0);
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));
    assertEquals("Search string should not have changed", lookupModel.getSearchString(), "joh");
    lookupModel.setSelectedEntities(result);
    assertEquals("Search string should have been updated",//this test fails due to the toString cache, strange?
            "John" + lookupModel.getMultipleValueSeparator() + "johnson", lookupModel.getSearchString());

    lookupModel.setSearchString("jo");
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));

    lookupModel.setWildcardPrefix(false);
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.setCaseSensitive(true);
    result = lookupModel.performQuery();
    assertEquals("Result count should be 1", 1, result.size());
    assertTrue("Result should contain John", contains(result, "John"));
    lookupModel.setCaseSensitive(false);
    result = lookupModel.performQuery();
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));

    lookupModel.setMultipleValueSeparator(";");
    lookupModel.setSearchString("andy ; Andrew ");//spaces should be trimmed away
    result = lookupModel.performQuery();
    assertEquals("Result count should be 2", 2, result.size());
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("andy;Andrew");
    result = lookupModel.performQuery();
    assertEquals("Result count should be 2", 2, result.size());
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));
    lookupModel.setSelectedEntities(result);
    assertTrue("Search string should represent the selected items", lookupModel.searchStringRepresentsSelected());

    lookupModel.setSearchString("and; rew");
    lookupModel.setWildcardPrefix(true);
    lookupModel.setWildcardPostfix(false);
    result = lookupModel.performQuery();
    assertEquals("Result count should be 1", 1, result.size());
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("Joh");
    lookupModel.setCaseSensitive(true);
    lookupModel.setWildcardPostfix(true);
    lookupModel.setAdditionalLookupCriteria(
            EntityCriteriaUtil.propertyCriteria(Entities.getColumnProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB),
                    SearchType.NOT_LIKE, "ajob"));
    result = lookupModel.performQuery();
    assertTrue("Result should contain john", contains(result, "John"));
    assertFalse("Result should not contain johnson", contains(result, "johnson"));
  }

  @Test
  public void setAdditionalLookupCriteria() {
    lookupModel.setMultipleSelectionAllowed(false);
    lookupModel.setWildcard("%");
    lookupModel.setSearchString("johnson");
    List<Entity> result = lookupModel.performQuery();
    assertTrue("A single result should be returned", result.size() == 1);
    lookupModel.setSelectedEntities(result);
    lookupModel.setAdditionalLookupCriteria(new SimpleCriteria("1 = 2"));
    assertEquals(1, lookupModel.getSelectedEntities().size());
    result = lookupModel.performQuery();
    assertTrue("No result should be returned", result.isEmpty());
  }

  @Before
  public void setUp() throws Exception {
    EmpDept.init();
    lookupProperties = Arrays.asList(Entities.getColumnProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME),
                    Entities.getColumnProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB));
    lookupModel = new DefaultEntityLookupModel(EmpDept.T_EMPLOYEE, DefaultEntityConnectionTest.CONNECTION_PROVIDER, lookupProperties);

    DefaultEntityConnectionTest.CONNECTION_PROVIDER.getConnection().beginTransaction();
    setupData();
  }

  @After
  public void tearDown() throws Exception {
    DefaultEntityConnectionTest.CONNECTION_PROVIDER.getConnection().rollbackTransaction();
  }

  private boolean contains(final List<Entity> result, final String employeeName) {
    for (final Entity entity : result) {
      if (entity.getStringValue(EmpDept.EMPLOYEE_NAME).equals(employeeName)) {
        return true;
      }
    }

    return false;
  }

  private void setupData() throws Exception {
    final Entity dept = Entities.entity(EmpDept.T_DEPARTMENT);
    dept.setValue(EmpDept.DEPARTMENT_ID, 88);
    dept.setValue(EmpDept.DEPARTMENT_LOCATION, "TestLoc");
    dept.setValue(EmpDept.DEPARTMENT_NAME, "TestDept");

    final Entity emp = Entities.entity(EmpDept.T_EMPLOYEE);
    emp.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept);
    emp.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp.setValue(EmpDept.EMPLOYEE_JOB, "nojob");
    emp.setValue(EmpDept.EMPLOYEE_NAME, "John");
    emp.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    final Entity emp2 = Entities.entity(EmpDept.T_EMPLOYEE);
    emp2.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept);
    emp2.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp2.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp2.setValue(EmpDept.EMPLOYEE_JOB, "ajob");
    emp2.setValue(EmpDept.EMPLOYEE_NAME, "johnson");
    emp2.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    final Entity emp3 = Entities.entity(EmpDept.T_EMPLOYEE);
    emp3.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept);
    emp3.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp3.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp3.setValue(EmpDept.EMPLOYEE_JOB, "nojob");
    emp3.setValue(EmpDept.EMPLOYEE_NAME, "Andy");
    emp3.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    final Entity emp4 = Entities.entity(EmpDept.T_EMPLOYEE);
    emp4.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept);
    emp4.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp4.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp4.setValue(EmpDept.EMPLOYEE_JOB, "ajob");
    emp4.setValue(EmpDept.EMPLOYEE_NAME, "Andrew");
    emp4.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    DefaultEntityConnectionTest.CONNECTION_PROVIDER.getConnection().insert(Arrays.asList(dept, emp, emp2, emp3, emp4));
  }
}
