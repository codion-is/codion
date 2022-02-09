/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Employee;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.demos.chinook.ui.EmployeeEditPanel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * Just a little demo showcasing how a single {@link SwingEntityModel} behaves
 * when used by multiple {@link EntityPanel}s.
 *
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class MultiPanelDemo {

  public static void main(final String[] args) {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

    LocalEntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(DatabaseFactory.getDatabase());
    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(User.parseUser("scott:tiger"));

    EntityEditModel.POST_EDIT_EVENTS.set(true);

    SwingEntityModel employeeModel = new SwingEntityModel(Employee.TYPE, connectionProvider);
    employeeModel.getTableModel().refresh();

    JPanel basePanel = new JPanel(gridLayout(2, 2));
    for (int i = 0; i < 4; i++) {
      EntityPanel employeePanel = new EntityPanel(employeeModel, new EmployeeEditPanel(employeeModel.getEditModel()));
      employeePanel.getTablePanel().setConditionPanelVisible(true);
      employeePanel.initializePanel();
      basePanel.add(employeePanel);
    }

    Dialogs.componentDialog(basePanel)
            .title("Multi Panel Demo")
            .disposeOnEscape(false)
            .show()
            .dispose();
  }
}
