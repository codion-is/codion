package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Collection;

public class PropertyValueProviderTest {

  @Test
  public void test() throws Exception {
    new EmpDept();
    final PropertyValueProvider provider = new PropertyValueProvider(EntityDbConnectionTest.DB_PROVIDER, EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_JOB);
    final Collection<Object> values = provider.getValues();
    assertTrue(values.size() > 0);
    assertTrue(values.contains("ANALYST"));
    assertTrue(values.contains("CLERK"));
    assertTrue(values.contains("MANAGER"));
    assertTrue(values.contains("PRESIDENT"));
    assertTrue(values.contains("SALESMAN"));
  }
}
