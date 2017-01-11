/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.javafx.framework.model.FXEntityEditModel;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.junit.Test;

public final class EntityEditViewTest {

  static {
    new JFXPanel();
    TestDomain.init();
  }

  @Test
  public void constructor() {
    final FXEntityEditModel editModel = new FXEntityEditModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityEditView editView = new EmpEditView(editModel).initializePanel();
    editView.getButtonPanel();
  }

  static final class EmpEditView extends EntityEditView {

    public EmpEditView(final FXEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected Node initializeEditPanel() {
      setInitialFocusProperty(TestDomain.EMP_ID);

      createTextField(TestDomain.EMP_ID);
      createTextField(TestDomain.EMP_NAME);
      createTextField(TestDomain.EMP_SALARY);
      createForeignKeyLookupField(TestDomain.EMP_DEPARTMENT_FK);
      createDatePicker(TestDomain.EMP_HIREDATE);
      createForeignKeyComboBox(TestDomain.EMP_MGR_FK);

      createPropertyPanel(TestDomain.EMP_ID);

      return new GridPane();
    }
  }
}
