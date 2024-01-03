/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.test;

import is.codion.common.user.User;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;

import static java.util.Objects.requireNonNull;

/**
 * A class for testing {@link EntityApplicationPanel} classes
 */
public class EntityApplicationPanelTestUnit<M extends SwingEntityApplicationModel> {

  private final Class<M> modelClass;
  private final Class<? extends EntityApplicationPanel<M>> panelClass;
  private final User user;

  /**
   * Instantiates a new entity application panel test unit
   * @param modelClass the application model class
   * @param panelClass the application panel class
   * @param user the user
   */
  protected EntityApplicationPanelTestUnit(Class<M> modelClass, Class<? extends EntityApplicationPanel<M>> panelClass, User user) {
    this.modelClass = requireNonNull(modelClass, "modelClass");
    this.panelClass = requireNonNull(panelClass, "panelClass");
    this.user = requireNonNull(user, "user");
  }

  /**
   * Instantiates the panel and initializes it
   */
  protected final void testInitialize() {
    EntityApplicationPanel.builder(modelClass, panelClass)
            .automaticLoginUser(user)
            .saveDefaultUsername(false)
            .setUncaughtExceptionHandler(false)
            .displayStartupDialog(false)
            .displayFrame(false)
            .onApplicationStarted(applicationPanel -> applicationPanel.applicationModel().connectionProvider().close())
            .start(false);
  }
}
