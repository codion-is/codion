/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

import static javax.swing.SwingConstants.HORIZONTAL;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditPanelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void test() throws DatabaseException {
    final SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    final TestEditPanel editPanel = new TestEditPanel(editModel);
    editPanel.createHorizontalControlPanel();
    editPanel.createVerticalControlPanel();
    editPanel.createControlToolBar(HORIZONTAL);
    assertFalse(editPanel.isPanelInitialized());
    editPanel.initializePanel();
    assertTrue(editPanel.isPanelInitialized());

    assertEquals(editModel, editPanel.getEditModel());
    assertFalse(editPanel.isActive());
    editPanel.setActive(true);
    assertTrue(editPanel.isActive());

    final Entity martin = editModel.getConnectionProvider().getConnection().selectSingle(TestDomain.EMP_NAME, "MARTIN");
    editModel.setEntity(martin);
    assertFalse(editModel.isEntityNew());
    editPanel.clearAndRequestFocus();
    assertTrue(editModel.isEntityNew());
    assertEquals(7, editPanel.getComponentAttributes().size());

    editPanel.setClearAfterInsert(true);
    assertTrue(editPanel.isClearAfterInsert());
    editPanel.setRequestFocusAfterInsert(true);
    assertTrue(editPanel.isRequestFocusAfterInsert());

    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.SAVE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.UPDATE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.DELETE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.REFRESH));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.CLEAR));
    assertNotNull(editPanel.createInsertControl());
  }

  private static final class TestEditPanel extends EntityEditPanel {

    public TestEditPanel(final SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      createTextField(TestDomain.EMP_NAME);
      createValueListComboBox(TestDomain.EMP_JOB);
      createForeignKeyComboBox(TestDomain.EMP_MGR_FK);
      createForeignKeyComboBox(TestDomain.EMP_DEPARTMENT_FK);
      createTextField(TestDomain.EMP_SALARY);
      createTextField(TestDomain.EMP_COMMISSION);
      createTemporalInputPanel(TestDomain.EMP_HIREDATE)
              .calendarButton(true);

      setInitialFocusAttribute(TestDomain.EMP_NAME);

      setLayout(FlexibleGridLayout.builder().rows(3).columns(3).fixRowHeights(true).build());

      addInputPanel(TestDomain.EMP_NAME);
      addInputPanel(TestDomain.EMP_JOB);
      addInputPanel(TestDomain.EMP_DEPARTMENT_FK);

      addInputPanel(TestDomain.EMP_MGR_FK);
      addInputPanel(TestDomain.EMP_SALARY);
      addInputPanel(TestDomain.EMP_COMMISSION);

      addInputPanel(TestDomain.EMP_HIREDATE);
    }
  }
}
