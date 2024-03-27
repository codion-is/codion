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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
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

  private JTabbedPane tabbedPane;

  /**
   * Sets the layout to a {@link BorderLayout} and lays out the given application panel, by adding all root entity panels to a tabbed pane.
   * Note that this method is responsible for initializing any visible entity panels using {@link EntityPanel#initialize()}.
   * @param applicationPanel the application panel to layout
   */
  @Override
  public void layout(EntityApplicationPanel<?> applicationPanel) {
    requireNonNull(applicationPanel);
    if (tabbedPane != null) {
      throw new IllegalStateException("EntityApplicationPanel has already been laid out: " + applicationPanel);
    }
    tabbedPane = Components.tabbedPane()
            .tabPlacement(TAB_PLACEMENT.get())
            .focusable(false)
            .changeListener(new InitializeSelectedPanelListener())
            .build();
    applicationPanel.entityPanels().forEach(this::addTab);//InitializeSelectedPanelListener initializes first panel
    applicationPanel.setBorder(createEmptyBorder(0, Layouts.GAP.get(), 0, Layouts.GAP.get()));
    applicationPanel.setLayout(new BorderLayout());
    //tab pane added to a base panel for correct Look&Feel rendering
    applicationPanel.add(borderLayoutPanel(new BorderLayout())
            .centerComponent(tabbedPane)
            .build(), BorderLayout.CENTER);
  }

  @Override
  public final void select(EntityPanel entityPanel) {
    requireNonNull(entityPanel);
    if (tabbedPane == null) {
      throw new IllegalStateException("EntityApplicationPanel has not been laid out");
    }
    if (tabbedPane.indexOfComponent(entityPanel) != -1) {
      tabbedPane.setSelectedComponent(entityPanel);
    }
  }

  /**
   * @return the application tabbed pane
   */
  public final JTabbedPane tabbedPane() {
    return tabbedPane;
  }

  private void addTab(EntityPanel entityPanel) {
    tabbedPane.addTab(entityPanel.caption().get(), entityPanel);
    tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, entityPanel.description().get());
  }

  private final class InitializeSelectedPanelListener implements ChangeListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      if (tabbedPane.getTabCount() > 0) {
        ((EntityPanel) tabbedPane.getSelectedComponent()).activate();
      }
    }
  }
}
