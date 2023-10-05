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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
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
    int gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    applicationPanel.setBorder(BorderFactory.createEmptyBorder(0, gap, 0, gap));
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
            .tabPlacement(TAB_PLACEMENT.get())
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
    entityPanel.editPanel().active().addDataListener(new SelectActivatedPanelListener(entityPanel));
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
