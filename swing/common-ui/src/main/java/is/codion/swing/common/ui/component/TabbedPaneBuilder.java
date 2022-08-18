/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * A builder for a JTabbedPane.
 * <pre>
 * JTabbedPane tabbedPane = Components.tabbedPane()
 *         .tabPlacement(SwingConstants.TOP)
 *         .tab("First Tab", new JLabel("First"))
 *         .mnemonic(KeyEvent.VK_1)
 *         .toolTipText("This is the first tab")
 *         .add()
 *         .tab("Second Tab", new JLabel("Second"))
 *         .mnemonic(KeyEvent.VK_2)
 *         .toolTipText("This is the second tab")
 *         .add()
 *         .build();
 * </pre>
 */
public interface TabbedPaneBuilder extends ComponentBuilder<Void, JTabbedPane, TabbedPaneBuilder> {

  /**
   * @param tabPlacement the tab placement
   * @return this builder instance
   */
  TabbedPaneBuilder tabPlacement(int tabPlacement);

  /**
   * Returns a new {@link TabBuilder} for adding a tab
   * @param component the component to display in the tab
   * @return a new {@link TabBuilder} instance
   */
  TabBuilder tab(JComponent component);

  /**
   * Returns a new {@link TabBuilder} for adding a tab
   * @param title the tab title
   * @param component the component to display in the tab
   * @return a new {@link TabBuilder} instance
   */
  TabBuilder tab(String title, JComponent component);

  /**
   * @param changeListener the change listener
   * @return this builder instance
   */
  TabbedPaneBuilder changeListener(ChangeListener changeListener);

  /**
   * Builds a Tab for a {@link TabbedPaneBuilder}.
   */
  interface TabBuilder {

    /**
     * @param mnemonic the tab mnemonic
     * @return this builder instance
     */
    TabBuilder mnemonic(int mnemonic);

    /**
     * @param toolTipText the tab tool tip text
     * @return this builder instance
     */
    TabBuilder toolTipText(String toolTipText);

    /**
     * @param icon the tab icon
     * @return this builder instance
     */
    TabBuilder icon(Icon icon);

    /**
     * Adds this tab and returns the {@link TabbedPaneBuilder}
     * @return the {@link TabbedPaneBuilder} instance
     */
    TabbedPaneBuilder add();
  }
}
