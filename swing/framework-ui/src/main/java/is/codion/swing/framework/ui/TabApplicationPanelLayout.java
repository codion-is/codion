/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.panel.HierarchyPanel;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.framework.ui.EntityApplicationPanel.ApplicationPanelLayout;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;

final class TabApplicationPanelLayout implements ApplicationPanelLayout {

  private List<EntityPanel> entityPanels;
  private JTabbedPane applicationTabPane;

  @Override
  public void layoutPanel(EntityApplicationPanel<?> applicationPanel) {
    this.entityPanels = applicationPanel.entityPanels();
    applicationTabPane = createApplicationTabPane();
    //initialize first panel
    selectedChildPanel()
            .map(EntityPanel.class::cast)
            .ifPresent(EntityPanel::initialize);
    applicationPanel.setLayout(new BorderLayout());
    //tab pane added to a base panel for correct Look&Feel rendering
    applicationPanel.add(borderLayoutPanel()
            .centerComponent(applicationTabPane)
            .build(), BorderLayout.CENTER);
  }

  @Override
  public Optional<HierarchyPanel> selectedChildPanel() {
    if (applicationTabPane != null && applicationTabPane.getTabCount() > 0) {//initializeUI() may have been overridden
      return Optional.of((HierarchyPanel) applicationTabPane.getSelectedComponent());
    }

    return entityPanels.isEmpty() ? Optional.empty() : Optional.of(entityPanels.get(0));
  }

  @Override
  public void selectChildPanel(HierarchyPanel childPanel) {
    if (applicationTabPane != null && applicationTabPane.indexOfComponent((JComponent) childPanel) != -1) {//initializeUI() may have been overridden
      applicationTabPane.setSelectedComponent((JComponent) childPanel);
    }
  }

  private JTabbedPane createApplicationTabPane() {
    TabbedPaneBuilder builder = Components.tabbedPane()
            .tabPlacement(EntityApplicationPanel.TAB_PLACEMENT.get())
            .focusable(false)
            .changeListener(new InitializeSelectedPanelListener());
    entityPanels.stream()
            .peek(entityPanel -> builder.tabBuilder(entityPanel.getCaption(), entityPanel)
                    .toolTipText(entityPanel.getDescription())
                    .add())
            .filter(entityPanel -> entityPanel.editPanel() != null)
            .forEach(this::addSelectActivatedPanelListener);

    return builder.build();
  }

  private void addSelectActivatedPanelListener(EntityPanel entityPanel) {
    entityPanel.editPanel().addActiveListener(new SelectActivatedPanelListener(entityPanel));
  }

  private final class InitializeSelectedPanelListener implements ChangeListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      if (applicationTabPane.getTabCount() > 0) {
        ((EntityPanel) applicationTabPane.getSelectedComponent()).initialize();
      }
    }
  }

  private final class SelectActivatedPanelListener implements Consumer<Boolean> {

    private final EntityPanel entityPanel;

    private SelectActivatedPanelListener(EntityPanel entityPanel) {
      this.entityPanel = entityPanel;
    }

    @Override
    public void accept(Boolean panelActivated) {
      if (panelActivated) {
        selectChildPanel(entityPanel);
      }
    }
  }
}
