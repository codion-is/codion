/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for creating some basic panels.
 */
public final class Panels {

  private static final String CENTER_COMPONENT = "centerComponent";

  private Panels() {}

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param northComponent the component to display in the BorderLayout.NORTH position
   * @param centerComponent the component to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the NORTH an CENTER positions in a BorderLayout
   */
  public static JPanel createNorthCenterPanel(JComponent northComponent, JComponent centerComponent) {
    requireNonNull(northComponent, "northComponent");
    requireNonNull(centerComponent, CENTER_COMPONENT);

    return Components.panel(Layouts.borderLayout())
            .add(northComponent, BorderLayout.NORTH)
            .add(centerComponent, BorderLayout.CENTER)
            .build();
  }

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param westComponent the component to display in the BorderLayout.WEST position
   * @param centerComponent the component to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the WEST an CENTER positions in a BorderLayout
   */
  public static JPanel createWestCenterPanel(JComponent westComponent, JComponent centerComponent) {
    requireNonNull(westComponent, "westComponent");
    requireNonNull(centerComponent, CENTER_COMPONENT);

    return Components.panel(Layouts.borderLayout())
            .add(westComponent, BorderLayout.WEST)
            .add(centerComponent, BorderLayout.CENTER)
            .build();
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and non-focusable buttons based on buttonActions
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonActions the button actions
   * @return a panel
   * @see #createEastFocusableButtonPanel(JComponent, Action...)
   */
  public static JPanel createEastButtonPanel(JComponent centerComponent, Action... buttonActions) {
    return createEastButtonPanel(centerComponent, false, buttonActions);
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and focusable buttons based on buttonActions
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonActions the button actions
   * @return a panel
   */
  public static JPanel createEastFocusableButtonPanel(JComponent centerComponent, Action... buttonActions) {
    return createEastButtonPanel(centerComponent, true, buttonActions);
  }

  private static JPanel createEastButtonPanel(JComponent centerComponent, boolean buttonFocusable, Action... buttonActions) {
    requireNonNull(centerComponent, CENTER_COMPONENT);
    requireNonNull(buttonActions, "buttonActions");

    Dimension preferredSize = new Dimension(centerComponent.getPreferredSize().height, centerComponent.getPreferredSize().height);
    PanelBuilder buttonPanelBuilder = Components.panel(new GridLayout(1, buttonActions.length));
    for (Action buttonAction : buttonActions) {
      JButton button = new JButton(buttonAction);
      button.setPreferredSize(preferredSize);
      button.setFocusable(buttonFocusable);
      buttonPanelBuilder.add(button);
    }

    return Components.panel(new BorderLayout())
            .add(centerComponent, BorderLayout.CENTER)
            .add(buttonPanelBuilder.build(), BorderLayout.EAST)
            .build();
  }
}
