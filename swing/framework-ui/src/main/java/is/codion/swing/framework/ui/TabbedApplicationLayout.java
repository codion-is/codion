/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import static java.util.Objects.requireNonNull;

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
   * Lays out the given application panel, by adding all root entity panels to a tabbed pane.
   * @param applicationPanel the application panel
   */
  @Override
  public void layoutPanel(EntityApplicationPanel<?> applicationPanel) {
    requireNonNull(applicationPanel);
    if (!applicationPanel.entityPanels().isEmpty()) {
      //initialize first panel
      applicationPanel.entityPanels().get(0).initialize();
    }
    applicationPanel.entityPanels().forEach(this::addTab);
    applicationPanel.setLayout(new BorderLayout());
    //tab pane added to a base panel for correct Look&Feel rendering
    applicationPanel.add(borderLayoutPanel()
            .centerComponent(applicationTabPane)
            .build(), BorderLayout.CENTER);
  }

  @Override
  public final void selectEntityPanel(EntityPanel entityPanel) {
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
