/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.javafx.framework.ui;

import dev.codion.common.db.database.Databases;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.javafx.framework.model.FXEntityEditModel;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

public final class EntityEditViewTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
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
