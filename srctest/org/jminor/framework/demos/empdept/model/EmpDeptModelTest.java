/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.empdept.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.model.AbstractEntityTestFixture;
import org.jminor.framework.model.Entity;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashMap;

public class EmpDeptModelTest extends TestCase {

  private static IEntityDb db;

  static {
    new EmpDept();
    FrameworkSettings.get().useQueryRange = false;
    FrameworkSettings.get().useSmartRefresh = false;
    try {
      db = new AbstractEntityTestFixture() {
        public User getTestUser() throws UserCancelException {
          return new User("scott", "tiger");
        }

        public HashMap<String, Entity> initReferenceEntities(final Collection<String> classes) throws Exception {
          return null;
        }
      }.getIEntityDbProvider().getEntityDb();
    }
    catch (UserException e) {
      e.printStackTrace();
      throw e.getRuntimeException();
    }
  }

  public EmpDeptModelTest(String name) {
    super(name);
  }

  public void testDepartment() throws Exception {
    Util.printListContents(db.selectAll(EmpDept.T_DEPARTMENT));
  }

  public void testEmployee() throws Exception {
    Util.printListContents(db.selectAll(EmpDept.T_EMPLOYEE));
  }
}
