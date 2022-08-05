/*
 * Copyright (c) 2018 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;

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
    SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    TestEditPanel editPanel = new TestEditPanel(editModel);
    assertEquals(7, editPanel.getComponentAttributes().size());
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

    Entity martin = editModel.connectionProvider().connection().selectSingle(TestDomain.EMP_NAME, "MARTIN");
    editModel.setEntity(martin);
    assertFalse(editModel.isEntityNew());
    editPanel.clearAndRequestFocus();
    assertTrue(editModel.isEntityNew());
    assertEquals(7, editPanel.getComponentAttributes().size());

    editPanel.setClearAfterInsert(true);
    assertTrue(editPanel.isClearAfterInsert());
    editPanel.setRequestFocusAfterInsert(true);
    assertTrue(editPanel.isRequestFocusAfterInsert());

    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.INSERT));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.UPDATE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.DELETE));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.REFRESH));
    assertNotNull(editPanel.getControl(EntityEditPanel.ControlCode.CLEAR));
  }

  private static final class TestEditPanel extends EntityEditPanel {

    public TestEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
      createTextField(TestDomain.EMP_NAME);
      createItemComboBox(TestDomain.EMP_JOB);
      createForeignKeyComboBox(TestDomain.EMP_MGR_FK);
      createForeignKeyComboBox(TestDomain.EMP_DEPARTMENT_FK);
      createTextField(TestDomain.EMP_SALARY);
      createTextField(TestDomain.EMP_COMMISSION);
      createTemporalInputPanel(TestDomain.EMP_HIREDATE);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(TestDomain.EMP_NAME);

      setLayout(Layouts.flexibleGridLayout(3, 3));

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
