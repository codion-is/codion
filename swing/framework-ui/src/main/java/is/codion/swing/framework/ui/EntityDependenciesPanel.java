/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Map;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;

/**
 * Displays the given dependencies in a tabbed pane.
 */
final class EntityDependenciesPanel extends JPanel {

  private final JTabbedPane tabPane = new JTabbedPane(SwingConstants.TOP);

  EntityDependenciesPanel(Map<EntityType, Collection<Entity>> dependencies, EntityConnectionProvider connectionProvider) {
    super(new BorderLayout());
    for (Map.Entry<EntityType, Collection<Entity>> entry : dependencies.entrySet()) {
      tabPane.addTab(connectionProvider.entities().definition(entry.getKey()).caption(),
              EntityTablePanel.createEntityTablePanel(entry.getValue(), connectionProvider));
    }
    add(tabPane, BorderLayout.CENTER);
    KeyEvents.builder(VK_RIGHT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(Control.control(new NavigateRightCommand()))
            .enable(tabPane);
    KeyEvents.builder(VK_LEFT)
            .modifiers(ALT_DOWN_MASK | CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(Control.control(new NavigateLeftCommand()))
            .enable(tabPane);
  }

  void requestSelectedTableFocus() {
    ((EntityTablePanel) tabPane.getSelectedComponent()).table().requestFocusInWindow();
  }

  private final class NavigateRightCommand implements Control.Command {

    @Override
    public void perform() throws Exception {
      int selectedIndex = tabPane.getSelectedIndex();
      tabPane.setSelectedIndex(selectedIndex == tabPane.getTabCount() - 1 ? 0 : selectedIndex + 1);
      requestSelectedTableFocus();
    }
  }

  private final class NavigateLeftCommand implements Control.Command {

    @Override
    public void perform() throws Exception {
      int selectedIndex = tabPane.getSelectedIndex();
      tabPane.setSelectedIndex(selectedIndex == 0 ? tabPane.getTabCount() - 1 : selectedIndex - 1);
      requestSelectedTableFocus();
    }
  }
}
