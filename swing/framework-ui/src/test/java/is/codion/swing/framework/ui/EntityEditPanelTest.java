/*
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static javax.swing.SwingConstants.HORIZONTAL;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() throws DatabaseException {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
    TestEditPanel editPanel = new TestEditPanel(editModel);
    assertEquals(7, editPanel.componentAttributes().size());
    editPanel.createHorizontalControlPanel();
    editPanel.createVerticalControlPanel();
    editPanel.createControlToolBar(HORIZONTAL);
    editPanel.initializePanel();

    assertEquals(editModel, editPanel.editModel());
    assertFalse(editPanel.isActive());
    editPanel.setActive(true);
    assertTrue(editPanel.isActive());

    Entity martin = editModel.connectionProvider().connection().selectSingle(Employee.NAME, "MARTIN");
    editModel.setEntity(martin);
    assertFalse(editModel.isEntityNew());
    editPanel.clearAndRequestFocus();
    assertTrue(editModel.isEntityNew());
    assertEquals(7, editPanel.componentAttributes().size());

    editPanel.setClearAfterInsert(true);
    assertTrue(editPanel.isClearAfterInsert());
    editPanel.setRequestFocusAfterInsert(true);
    assertTrue(editPanel.isRequestFocusAfterInsert());

    assertNotNull(editPanel.control(EntityEditPanel.ControlCode.INSERT));
    assertNotNull(editPanel.control(EntityEditPanel.ControlCode.UPDATE));
    assertNotNull(editPanel.control(EntityEditPanel.ControlCode.DELETE));
    assertNotNull(editPanel.control(EntityEditPanel.ControlCode.REFRESH));
    assertNotNull(editPanel.control(EntityEditPanel.ControlCode.CLEAR));
  }

  private static final class TestEditPanel extends EntityEditPanel {

    public TestEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
      createTextField(Employee.NAME);
      createItemComboBox(Employee.JOB);
      createForeignKeyComboBox(Employee.MGR_FK);
      createForeignKeyComboBox(Employee.DEPARTMENT_FK);
      createTextField(Employee.SALARY);
      createTextField(Employee.COMMISSION);
      createTemporalInputPanel(Employee.HIREDATE);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Employee.NAME);

      setLayout(Layouts.flexibleGridLayout(3, 3));

      addInputPanel(Employee.NAME);
      addInputPanel(Employee.JOB);
      addInputPanel(Employee.DEPARTMENT_FK);

      addInputPanel(Employee.MGR_FK);
      addInputPanel(Employee.SALARY);
      addInputPanel(Employee.COMMISSION);

      addInputPanel(Employee.HIREDATE);
    }
  }
}
