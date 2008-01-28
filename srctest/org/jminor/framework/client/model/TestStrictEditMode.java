package org.jminor.framework.client.model;

import junit.framework.TestCase;
import org.jminor.common.db.User;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.EntityKey;

public class TestStrictEditMode extends TestCase {

  private final static IEntityDbProvider dbProvider = EntityDbProviderFactory.createEntityDbProvider(
          new User("scott", "tiger"), TestStrictEditMode.class.getSimpleName());
  private static DepartmentModel model;

  public TestStrictEditMode() {
    super("TestStrictEditMode");
  }

  protected void setUp() throws Exception {
    FrameworkSettings.get().useSmartRefresh = false;
    FrameworkSettings.get().useQueryRange = false;
    new EmpDept();
  }

  protected void tearDown() throws Exception {
    dbProvider.getEntityDb().logout();
    model.getDbConnectionProvider().getEntityDb().logout();
  }

  public void testStrictEditModel() throws Exception {
    model = new DepartmentModel(EntityDbProviderFactory.createEntityDbProvider(
          new User("scott", "tiger"), TestStrictEditMode.class.getSimpleName()));
    model.refresh();
    model.setStrictEditMode(true);

    //select entity and change a value
    model.getTableModel().setSelectedItemIdx(0);
    final EntityKey primaryKey = model.getActiveEntityCopy().getPrimaryKey();
    final Object originalValue = model.getValue(EmpDept.DEPARTMENT_LOCATION);
    model.uiSetValue(EmpDept.DEPARTMENT_LOCATION, "None really");
    //assert row is locked
    boolean locked = false;
    try {
      dbProvider.getEntityDb().selectForUpdate(primaryKey);
    }
    catch (Exception e) {
      locked = true;
    }

    assertTrue("Row should be locked after modification", locked);
    //revert value to original
    model.uiSetValue(EmpDept.DEPARTMENT_LOCATION, originalValue);
    //assert row is not locked, and unlock it please
    try {
      dbProvider.getEntityDb().selectForUpdate(primaryKey);
      locked = false;
      dbProvider.getEntityDb().endTransaction(true);
    }
    catch (Exception e) {
      locked = true;
    }

    assertFalse("Row should not be locked after value has been reverted", locked);
    //change value
    model.uiSetValue(EmpDept.DEPARTMENT_LOCATION, "Hello world");
    //assert row is locked
    locked = false;
    try {
      dbProvider.getEntityDb().selectForUpdate(primaryKey);
    }
    catch (Exception e) {
      locked = true;
    }

    assertTrue("Row should be locked after modification", locked);
    //do update
    model.update();
    //assert row is not locked
    try {
      dbProvider.getEntityDb().selectForUpdate(primaryKey);
      locked = false;
      dbProvider.getEntityDb().endTransaction(true);
    }
    catch (Exception e) {
      locked = true;
    }

    assertFalse("Row should not be locked after update", locked);

    model.uiSetValue(EmpDept.DEPARTMENT_LOCATION, "None really");
    //assert row is locked
    locked = false;
    try {
      dbProvider.getEntityDb().selectForUpdate(primaryKey);
    }
    catch (Exception e) {
      locked = true;
    }

    assertTrue("Row should be locked after modification", locked);

    model.getTableModel().setSelectedItemIdx(1);

    try {
      dbProvider.getEntityDb().selectForUpdate(primaryKey);
      locked = false;
      dbProvider.getEntityDb().endTransaction(true);
    }
    catch (Exception e) {
      locked = true;
    }

    assertFalse("Row should not be locked after another has been selected", locked);

    //clean up by resetting the value
    model.getTableModel().setSelectedItemIdx(0);
    model.uiSetValue(EmpDept.DEPARTMENT_LOCATION, originalValue);
    model.update();
  }
}
