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
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import static java.util.Arrays.asList;

/**
 * The {@link EntityTablePanel} javadoc snippets, each the region of the same name.
 */
final class EntityTablePanelSnippets {

  static final class CustomTablePanel extends EntityTablePanel {

    CustomTablePanel(SwingEntityTableModel tableModel) {
      super(tableModel);
      configureToolBar(layout -> layout // @start region=configureToolBar
              .after(ControlKeys.TOGGLE_FILTER_VIEW)
              .separator()
              .control(createCustomControl())
              .remove(ControlKeys.PRINT)); // @end
      configurePopupMenu(layout -> layout // @start region=configurePopupMenu
              .after(ControlKeys.REFRESH)
              .control(createCustomControl())
              .controls(ControlKeys.COPY_CONTROLS, copy -> copy
                      .remove(ControlKeys.COPY_COLUMN))
              .remove(ControlKeys.VIEW_DEPENDENCIES)); // @end
    }

    @Override
    protected String preferencesKey() {
      // @start region=preferencesKey
      return model().getClass().getSimpleName() + "-" + model().entityType(); // @end
    }

    private Control createCustomControl() {
      return Control.command(() -> {});
    }
  }

  void popupMenuLayout() {
    EntityTablePanel.Config.POPUP_MENU_LAYOUT.set(Controls.layout(asList( // @start region=popupMenuLayout
            EntityTablePanel.ControlKeys.REFRESH,
            ControlKey.SEPARATOR,
            EntityTablePanel.ControlKeys.ADDITIONAL_POPUP_MENU_CONTROLS,
            ControlKey.SEPARATOR,
            EntityTablePanel.ControlKeys.CONDITION_CONTROLS,
            ControlKey.SEPARATOR,
            EntityTablePanel.ControlKeys.COPY_CONTROLS))); // @end
  }

  void toolBarLayout() {
    EntityTablePanel.Config.TOOLBAR_LAYOUT.set(Controls.layout(asList( // @start region=toolBarLayout
            EntityTablePanel.ControlKeys.TOGGLE_CONDITION_VIEW,
            EntityTablePanel.ControlKeys.TOGGLE_FILTER_VIEW,
            ControlKey.SEPARATOR,
            EntityTablePanel.ControlKeys.ADDITIONAL_TOOLBAR_CONTROLS))); // @end
  }
}
