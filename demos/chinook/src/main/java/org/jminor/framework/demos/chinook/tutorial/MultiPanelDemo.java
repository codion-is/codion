/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.tutorial;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.user.Users;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.demos.chinook.ui.EmployeeEditPanel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.dialog.DisposeOnEscape;
import org.jminor.swing.common.ui.dialog.Modal;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.ui.EntityPanel;

import javax.swing.JPanel;
import java.awt.GridLayout;

/**
 * Just a little demo showcasing how a single {@link SwingEntityModel} behaves
 * when used by multiple {@link EntityPanel}s.
 *
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class MultiPanelDemo {

  public static void main(final String[] args) {
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    LocalEntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(Databases.getInstance());
    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(Users.parseUser("scott:tiger"));

    EntityEditModel.POST_EDIT_EVENTS.set(true);

    SwingEntityModel employeeModel = new SwingEntityModel(Chinook.T_EMPLOYEE, connectionProvider);
    employeeModel.refresh();

    JPanel basePanel = new JPanel(new GridLayout(2, 2, 5, 5));
    for (int i = 0; i < 4; i++) {
      EntityPanel employeePanel = new EntityPanel(employeeModel, new EmployeeEditPanel(employeeModel.getEditModel()));
      employeePanel.getTablePanel().setConditionPanelVisible(true);
      employeePanel.initializePanel();
      basePanel.add(employeePanel);
    }

    Dialogs.displayInDialog(null, basePanel, "Multi Panel Demo", Modal.NO, DisposeOnEscape.NO);
  }
}
