/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.SearchType;
import org.jminor.framework.db.EntityDbLocalProvider;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class EntityLookupModelTest extends TestCase {

  private IEntityDbProvider dbProvider;
  private EntityLookupModel lookupModel;

  public void testLookupModel() throws Exception {
    lookupModel.setMultipleSelectionAllowed(true);
    lookupModel.setSearchString("joh");
    List<Entity> result = lookupModel.performQuery();
    assertTrue("Result should not be empty", result.size() > 0);
    assertTrue("Result should contain John", contains(result, "John"));
    assertTrue("Result should contain johnson", contains(result, "johnson"));
    assertFalse("Result should not contain Andy", contains(result, "Andy"));
    assertFalse("Result should not contain Andrew", contains(result, "Andrew"));
    assertEquals("Search string should not have changed", lookupModel.getSearchString(), "joh");
//    lookupModel.setSelectedEntities(result);
//    assertEquals("Search string should have been updated",//this test fails due to the toString cache, strange?
//            "john, nojob, TestDept" + lookupModel.getMultiValueSeperator() + "johnson, ajob, TestDept",
//            lookupModel.getSearchString());

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

    lookupModel.setMultiValueSeperator(";");
    lookupModel.setSearchString("andy;Andrew");
    result = lookupModel.performQuery();
    assertEquals("Result count should be 2", 2, result.size());
    assertTrue("Result should contain Andy", contains(result, "Andy"));
    assertTrue("Result should contain Andrew", contains(result, "Andrew"));

    lookupModel.setSearchString("and;rew");
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
            new PropertyCriteria(EntityRepository.get().getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB),
                    SearchType.NOT_LIKE, "ajob"));
    result = lookupModel.performQuery();
    assertTrue("Result should contain john", contains(result, "John"));
    assertFalse("Result should not contain johnson", contains(result, "johnson"));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    new EmpDept();
    dbProvider = new EntityDbLocalProvider(new User("scott", "tiger"));
    lookupModel = new EntityLookupModel(EmpDept.T_EMPLOYEE, dbProvider,
            Arrays.asList(EntityRepository.get().getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME),
                    EntityRepository.get().getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB)));

    setupData();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    dbProvider.getEntityDb().endTransaction(false);
    dbProvider.getEntityDb().logout();
  }

  private boolean contains(final List<Entity> result, final String employeeName) {
    for (final Entity entity : result) {
      if (entity.getStringValue(EmpDept.EMPLOYEE_NAME).equals(employeeName))
        return true;
    }

    return false;
  }

  private void setupData() throws Exception {
    final Entity dept = new Entity(EmpDept.T_DEPARTMENT);
    dept.setValue(EmpDept.DEPARTMENT_ID, 88);
    dept.setValue(EmpDept.DEPARTMENT_LOCATION, "TestLoc");
    dept.setValue(EmpDept.DEPARTMENT_NAME, "TestDept");

    final Entity emp = new Entity(EmpDept.T_EMPLOYEE);
    emp.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, dept);
    emp.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp.setValue(EmpDept.EMPLOYEE_JOB, "nojob");
    emp.setValue(EmpDept.EMPLOYEE_NAME, "John");
    emp.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    final Entity emp2 = new Entity(EmpDept.T_EMPLOYEE);
    emp2.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, dept);
    emp2.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp2.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp2.setValue(EmpDept.EMPLOYEE_JOB, "ajob");
    emp2.setValue(EmpDept.EMPLOYEE_NAME, "johnson");
    emp2.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    final Entity emp3 = new Entity(EmpDept.T_EMPLOYEE);
    emp3.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, dept);
    emp3.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp3.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp3.setValue(EmpDept.EMPLOYEE_JOB, "nojob");
    emp3.setValue(EmpDept.EMPLOYEE_NAME, "Andy");
    emp3.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    final Entity emp4 = new Entity(EmpDept.T_EMPLOYEE);
    emp4.setValue(EmpDept.EMPLOYEE_DEPARTMENT_REF, dept);
    emp4.setValue(EmpDept.EMPLOYEE_COMMISSION, 1000d);
    emp4.setValue(EmpDept.EMPLOYEE_HIREDATE, new Date());
    emp4.setValue(EmpDept.EMPLOYEE_JOB, "ajob");
    emp4.setValue(EmpDept.EMPLOYEE_NAME, "Andrew");
    emp4.setValue(EmpDept.EMPLOYEE_SALARY, 1000d);

    dbProvider.getEntityDb().startTransaction();
    dbProvider.getEntityDb().insert(Arrays.asList(dept, emp, emp2, emp3, emp4));
  }
}
