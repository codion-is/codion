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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.framework.db.EntityConnection;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JFrame;

/**
 * The {@link EntityPanel} javadoc snippets, each the region of the same name.
 */
final class EntityPanelSnippets {

  void usage(EntityConnection connection) {
    SwingEntityModel entityModel = new SwingEntityModel(Employee.TYPE, connection); // @start region=usage
    EntityPanel entityPanel = new EntityPanel(entityModel);
    entityPanel.initialize();
    JFrame frame = new JFrame();
    frame.add(entityPanel);
    frame.pack();
    frame.setVisible(true); // @end
  }

  static final class CustomPanel extends EntityPanel {

    CustomPanel(SwingEntityModel model) {
      super(model);
      configureControls(layout -> layout // @start region=configureControls
              .separator()
              .control(createCustomControl())); // @end
    }

    @Override
    public String preferencesKey() {
      // @start region=preferencesKey
      return model().preferencesKey(); // @end
    }

    private Control createCustomControl() {
      return Control.command(() -> {});
    }
  }
}
