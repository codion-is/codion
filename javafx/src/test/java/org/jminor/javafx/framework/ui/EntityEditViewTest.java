/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.javafx.framework.model.FXEntityEditModel;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.junit.Test;

public final class EntityEditViewTest {

  protected static final Entities ENTITIES = new TestDomain();

  protected static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, UNIT_TEST_USER, Databases.getInstance());

  static {
    new JFXPanel();
  }

  @Test
  public void constructor() {
    final FXEntityEditModel editModel = new FXEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
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
