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

import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() throws DatabaseException {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
    TestEditPanel editPanel = new TestEditPanel(editModel);
    assertEquals(7, editPanel.attributes().size());
    assertThrows(IllegalStateException.class, editPanel::createControls);
    editPanel.initialize();
    editPanel.createControls();

    assertEquals(editModel, editPanel.editModel());
    assertFalse(editPanel.active().get());
    editPanel.active().set(true);
    assertTrue(editPanel.active().get());

    Entity martin = editModel.connectionProvider().connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
    editModel.set(martin);
    assertTrue(editModel.exists().get());
    editPanel.clearAndRequestFocus();
    assertFalse(editModel.exists().get());
    assertEquals(7, editPanel.attributes().size());

    editPanel.clearAfterInsert().set(true);
    editPanel.requestFocusAfterInsert().set(true);

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
      createTemporalFieldPanel(Employee.HIREDATE);
    }

    @Override
    protected void initializeUI() {
      initialFocusAttribute().set(Employee.NAME);

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
