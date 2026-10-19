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

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;

import static java.util.Collections.emptyList;

/**
 * The {@link EntityApplicationPanel} javadoc snippets, each the region of the same name.
 */
final class EntityApplicationPanelSnippets {

  static final class CustomApplicationPanel extends EntityApplicationPanel<SwingEntityApplicationModel> {

    CustomApplicationPanel(SwingEntityApplicationModel applicationModel) {
      super(applicationModel, emptyList(), emptyList());
      configureMainMenu(layout -> layout // @start region=configureMainMenu
              .controls(ControlKeys.TOOLS_MENU, tools -> tools
                      .control(createImportControl()))
              .controls(ControlKeys.VIEW_MENU, view -> view
                      .after(ControlKeys.SELECT_SCALING)
                      .control(createSelectLanguageControl()))
              .after(ControlKeys.TOOLS_MENU)
              .control(createReportsMenu())); // @end
    }

    private Control createImportControl() {
      return Control.builder()
              .command(() -> {})
              .caption("Import...")
              .build();
    }

    private Control createSelectLanguageControl() {
      return Control.builder()
              .command(() -> {})
              .caption("Language...")
              .build();
    }

    private Controls createReportsMenu() {
      return Controls.builder()
              .caption("Reports")
              .control(Control.builder()
                      .command(() -> {})
                      .caption("Sales"))
              .build();
    }
  }
}
