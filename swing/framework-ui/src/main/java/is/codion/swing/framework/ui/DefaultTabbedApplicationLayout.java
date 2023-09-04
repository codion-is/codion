/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;

final class DefaultTabbedApplicationLayout implements TabbedApplicationLayout {

  private List<EntityPanel> entityPanels;
  private JTabbedPane applicationTabPane;

  @Override
  public void layoutPanel(EntityApplicationPanel<?> applicationPanel) {
    this.entityPanels = applicationPanel.entityPanels();
    applicationTabPane = createApplicationTabPane();
    //initialize first panel
    selectedPanel().ifPresent(EntityPanel::initialize);
    applicationPanel.setLayout(new BorderLayout());
    //tab pane added to a base panel for correct Look&Feel rendering
    applicationPanel.add(borderLayoutPanel()
            .centerComponent(applicationTabPane)
            .build(), BorderLayout.CENTER);
  }

  @Override
  public void selectEntityPanel(EntityPanel entityPanel) {
    if (applicationTabPane != null && applicationTabPane.indexOfComponent(entityPanel) != -1) {
      applicationTabPane.setSelectedComponent(entityPanel);
    }
  }

  @Override
  public JTabbedPane applicationTabPane() {
    return applicationTabPane;
  }

  private Optional<EntityPanel> selectedPanel() {
    if (applicationTabPane != null && applicationTabPane.getTabCount() > 0) {
      return Optional.of((EntityPanel) applicationTabPane.getSelectedComponent());
    }

    return entityPanels.isEmpty() ? Optional.empty() : Optional.of(entityPanels.get(0));
  }

  private JTabbedPane createApplicationTabPane() {
    TabbedPaneBuilder builder = Components.tabbedPane()
            .tabPlacement(EntityApplicationPanel.TAB_PLACEMENT.get())
            .focusable(false)
            .changeListener(new InitializeSelectedPanelListener());
    entityPanels.stream()
            .peek(entityPanel -> builder.tabBuilder(entityPanel.caption().get(), entityPanel)
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
        selectEntityPanel(entityPanel);
      }
    }
  }
}
