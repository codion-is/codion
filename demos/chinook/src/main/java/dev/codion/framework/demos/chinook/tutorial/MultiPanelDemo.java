/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.tutorial;

import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.Databases;
import dev.codion.common.user.Users;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.demos.chinook.domain.Chinook;
import dev.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import dev.codion.framework.demos.chinook.ui.EmployeeEditPanel;
import dev.codion.framework.model.EntityEditModel;
import dev.codion.swing.common.ui.dialog.Dialogs;
import dev.codion.swing.common.ui.dialog.DisposeOnEscape;
import dev.codion.swing.common.ui.dialog.Modal;
import dev.codion.swing.framework.model.SwingEntityModel;
import dev.codion.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;

import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

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
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    LocalEntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(Databases.getInstance());
    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(Users.parseUser("scott:tiger"));

    EntityEditModel.POST_EDIT_EVENTS.set(true);

    SwingEntityModel employeeModel = new SwingEntityModel(Chinook.T_EMPLOYEE, connectionProvider);
    employeeModel.refresh();

    JPanel basePanel = new JPanel(gridLayout(2, 2));
    for (int i = 0; i < 4; i++) {
      EntityPanel employeePanel = new EntityPanel(employeeModel, new EmployeeEditPanel(employeeModel.getEditModel()));
      employeePanel.getTablePanel().setConditionPanelVisible(true);
      employeePanel.initializePanel();
      basePanel.add(employeePanel);
    }

    Dialogs.displayInDialog(null, basePanel, "Multi Panel Demo", Modal.NO, DisposeOnEscape.NO);
  }
}
