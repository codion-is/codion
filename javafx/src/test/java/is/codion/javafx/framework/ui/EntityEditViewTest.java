/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.javafx.framework.model.FXEntityEditModel;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

public final class EntityEditViewTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void constructor() {
    FXEntityEditModel editModel = new FXEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    EntityEditView editView = new EmpEditView(editModel).initializePanel();
    editView.getButtonPanel();
  }

  static final class EmpEditView extends EntityEditView {

    public EmpEditView(final FXEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected Node initializeEditPanel() {
      setInitialFocusAttribute(TestDomain.EMP_ID);

      createTextField(TestDomain.EMP_ID);
      createTextField(TestDomain.EMP_NAME);
      createTextField(TestDomain.EMP_SALARY);
      createForeignKeySearchField(TestDomain.EMP_DEPARTMENT_FK);
      createDatePicker(TestDomain.EMP_HIREDATE);
      createForeignKeyComboBox(TestDomain.EMP_MGR_FK);

      createPropertyPanel(TestDomain.EMP_ID);

      return new GridPane();
    }
  }
}
