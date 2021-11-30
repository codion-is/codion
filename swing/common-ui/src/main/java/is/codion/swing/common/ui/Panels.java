/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.i18n.Messages;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for creating some basic panels.
 */
public final class Panels {

  private Panels() {}

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param northComponent the component to display in the BorderLayout.NORTH position
   * @param centerComponent the component to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the NORTH an CENTER positions in a BorderLayout
   */
  public static JPanel createNorthCenterPanel(final JComponent northComponent, final JComponent centerComponent) {
    requireNonNull(northComponent, "northComponent");
    requireNonNull(centerComponent, "centerComponent");
    final JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(northComponent, BorderLayout.NORTH);
    panel.add(centerComponent, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param westComponent the component to display in the BorderLayout.WEST position
   * @param centerComponent the component to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the WEST an CENTER positions in a BorderLayout
   */
  public static JPanel createWestCenterPanel(final JComponent westComponent, final JComponent centerComponent) {
    requireNonNull(westComponent, "westComponent");
    requireNonNull(centerComponent, "centerComponent");
    final JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(westComponent, BorderLayout.WEST);
    panel.add(centerComponent, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a non-focusable button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @return a panel
   * @see #createEastFocusableButtonPanel(JComponent, Action)
   */
  public static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction) {
    return createEastButtonPanel(centerComponent, buttonAction, false);
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a focusable button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @return a panel
   */
  public static JPanel createEastFocusableButtonPanel(final JComponent centerComponent, final Action buttonAction) {
    return createEastButtonPanel(centerComponent, buttonAction, true);
  }

  /**
   * Creates a panel containing two buttons, based on the given actions
   * @param okAction the ok action
   * @param cancelAction the cancel action
   * @return a new panel instance
   */
  public static JPanel createOkCancelButtonPanel(final Action okAction, final Action cancelAction) {
    requireNonNull(okAction, "okAction");
    requireNonNull(cancelAction, "cancelAction");
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
    final JPanel buttonPanel = new JPanel(Layouts.gridLayout(1, 2));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    return buttonPanel;
  }

  private static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction,
                                              final boolean buttonFocusable) {
    requireNonNull(centerComponent, "centerComponent");
    requireNonNull(buttonAction, "buttonAction");
    final JPanel panel = new JPanel(new BorderLayout());
    final JButton button = new JButton(buttonAction);
    button.setPreferredSize(new Dimension(centerComponent.getPreferredSize().height, centerComponent.getPreferredSize().height));
    button.setFocusable(buttonFocusable);
    panel.add(centerComponent, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);

    return panel;
  }
}
