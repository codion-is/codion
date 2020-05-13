/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.db.database.Databases;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.swing.common.ui.layout.FlexibleGridLayout;
import dev.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import dev.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import dev.codion.swing.common.ui.time.TemporalInputPanel.CalendarButton;
import dev.codion.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

import static javax.swing.SwingConstants.HORIZONTAL;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditPanelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

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

    final Entity martin = editModel.getConnectionProvider().getConnection().selectSingle(TestDomain.T_EMP,
            TestDomain.EMP_NAME, "MARTIN");
    editModel.setEntity(martin);
    assertFalse(editModel.isEntityNew());
    editPanel.clearAndRequestFocus();
    assertTrue(editModel.isEntityNew());
    assertEquals(7, editPanel.getComponentPropertyIds().size());

    editPanel.setClearAfterInsert(true);
    assertTrue(editPanel.isClearAfterInsert());
    editPanel.setRequestFocusAfterInsert(true);
    assertTrue(editPanel.isRequestFocusAfterInsert());

    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.SAVE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.UPDATE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.DELETE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.REFRESH));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.CLEAR));
    assertNotNull(editPanel.getInsertControl());
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
      createTemporalInputPanel(TestDomain.EMP_HIREDATE, CalendarButton.YES);

      setInitialFocusProperty(TestDomain.EMP_NAME);

      setLayout(new FlexibleGridLayout(3, 3, 5, 5, FixRowHeights.YES, FixColumnWidths.NO));

      addPropertyPanel(TestDomain.EMP_NAME);
      addPropertyPanel(TestDomain.EMP_JOB);
      addPropertyPanel(TestDomain.EMP_DEPARTMENT_FK);

      addPropertyPanel(TestDomain.EMP_MGR_FK);
      addPropertyPanel(TestDomain.EMP_SALARY);
      addPropertyPanel(TestDomain.EMP_COMMISSION);

      addPropertyPanel(TestDomain.EMP_HIREDATE);
    }
  }
}
