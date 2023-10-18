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

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.Components;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.HORIZONTAL_VERTICAL_GAP;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * EntityApplicationPanel layout based on a JTabbedPane.
 */
public class TabbedApplicationLayout implements EntityApplicationPanel.ApplicationLayout {

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (SwingConstants.TOP, SwingConstants.BOTTOM, SwingConstants.LEFT, SwingConstants.RIGHT)<br>
   * Default value: {@link SwingConstants#TOP}
   */
  public static final PropertyValue<Integer> TAB_PLACEMENT =
          Configuration.integerValue("is.codion.swing.framework.ui.TabbedApplicationLayout.tabPlacement", SwingConstants.TOP);

  private final JTabbedPane applicationTabPane = Components.tabbedPane()
          .tabPlacement(TAB_PLACEMENT.get())
          .focusable(false)
          .changeListener(new InitializeSelectedPanelListener())
          .build();

  /**
   * Sets the layout to a {@link BorderLayout} and lays out the given application panel, by adding all root entity panels to a tabbed pane.
   * Note that this method is responsible for initializing any visible entity panels using {@link EntityPanel#initialize()}.
   * @param applicationPanel the application panel to lay out
   */
  @Override
  public void layout(EntityApplicationPanel<?> applicationPanel) {
    requireNonNull(applicationPanel);
    if (!applicationPanel.entityPanels().isEmpty()) {
      //initialize first panel
      applicationPanel.entityPanels().get(0).initialize();
    }
    applicationPanel.entityPanels().forEach(this::addTab);
    applicationPanel.setLayout(new BorderLayout());
    //tab pane added to a base panel for correct Look&Feel rendering
    applicationPanel.add(borderLayoutPanel(new BorderLayout())
            .centerComponent(applicationTabPane)
            .border(createEmptyBorder(0, HORIZONTAL_VERTICAL_GAP.get(), 0, HORIZONTAL_VERTICAL_GAP.get()))
            .build(), BorderLayout.CENTER);
  }

  @Override
  public final void select(EntityPanel entityPanel) {
    requireNonNull(entityPanel);
    if (applicationTabPane.indexOfComponent(entityPanel) != -1) {
      applicationTabPane.setSelectedComponent(entityPanel);
    }
  }

  /**
   * @return the application tab pane
   */
  public final JTabbedPane applicationTabPane() {
    return applicationTabPane;
  }

  private void addTab(EntityPanel entityPanel) {
    applicationTabPane.addTab(entityPanel.caption().get(), entityPanel);
    applicationTabPane.setToolTipTextAt(applicationTabPane.getTabCount() - 1, entityPanel.getDescription());
  }

  private final class InitializeSelectedPanelListener implements ChangeListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      if (applicationTabPane.getTabCount() > 0) {
        ((EntityPanel) applicationTabPane.getSelectedComponent()).initialize();
      }
    }
  }
}
