/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Objects.requireNonNull;

/**
 * Displays the given dependencies in a tabbed pane.
 * @see #displayDependenciesDialog(Collection, EntityConnectionProvider, JComponent)
 */
public final class EntityDependenciesPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityDependenciesPanel.class.getName());

  private final JTabbedPane tabPane = new JTabbedPane(SwingConstants.TOP);

  EntityDependenciesPanel(Map<EntityType, Collection<Entity>> dependencies, EntityConnectionProvider connectionProvider) {
    super(new BorderLayout());
    for (Map.Entry<EntityType, Collection<Entity>> entry : dependencies.entrySet()) {
      tabPane.addTab(connectionProvider.entities().definition(entry.getKey()).caption(),
              EntityTablePanel.entityTablePanel(entry.getValue(), connectionProvider));
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

  /**
   * Displays a dialog containing the entities depending on the given entities.
   * @param entities the entities for which to display dependencies
   * @param connectionProvider the connection provider
   * @param dialogParent the dialog parent
   */
  public static void displayDependenciesDialog(Collection<Entity> entities, EntityConnectionProvider connectionProvider,
                                               JComponent dialogParent) {
    displayDependenciesDialog(entities, connectionProvider, dialogParent, MESSAGES.getString("no_dependencies"));
  }

  /**
   * Shows a dialog containing the entities depending on the given entities.
   * @param entities the entities for which to display dependencies
   * @param connectionProvider the connection provider
   * @param dialogParent the dialog parent
   * @param noDependenciesMessage the message to show in case of no dependencies
   */
  public static void displayDependenciesDialog(Collection<Entity> entities, EntityConnectionProvider connectionProvider,
                                               JComponent dialogParent, String noDependenciesMessage) {
    requireNonNull(entities);
    requireNonNull(connectionProvider);
    requireNonNull(dialogParent);
    Map<EntityType, Collection<Entity>> dependencies = selectDependencies(entities, connectionProvider, dialogParent);
    if (!dependencies.isEmpty()) {
      displayDependenciesDialog(dependencies, connectionProvider, dialogParent, noDependenciesMessage);
    }
  }

  private static Map<EntityType, Collection<Entity>> selectDependencies(Collection<Entity> entities,
                                                                        EntityConnectionProvider connectionProvider,
                                                                        JComponent dialogParent) {
    WaitCursor.show(dialogParent);
    try {
      return connectionProvider.connection().selectDependencies(entities);
    }
    catch (DatabaseException e) {
      Dialogs.displayExceptionDialog(e, parentWindow(dialogParent));

      return Collections.emptyMap();
    }
    finally {
      WaitCursor.hide(dialogParent);
    }
  }

  private static void displayDependenciesDialog(Map<EntityType, Collection<Entity>> dependencies,
                                                EntityConnectionProvider connectionProvider,
                                                JComponent dialogParent, String noDependenciesMessage) {
    if (dependencies.isEmpty()) {
      JOptionPane.showMessageDialog(dialogParent, noDependenciesMessage,
              MESSAGES.getString("no_dependencies_title"), JOptionPane.INFORMATION_MESSAGE);
    }
    else {
      EntityDependenciesPanel dependenciesPanel = new EntityDependenciesPanel(dependencies, connectionProvider);
      Dialogs.componentDialog(dependenciesPanel)
              .owner(dialogParent)
              .title(MESSAGES.getString("dependencies_found"))
              .onShown(dialog -> dependenciesPanel.requestSelectedTableFocus())
              .show();
    }
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
