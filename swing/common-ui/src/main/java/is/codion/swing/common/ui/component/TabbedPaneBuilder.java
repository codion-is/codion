/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import java.awt.Component;

/**
 * A builder for a JTabbedPane.
 * <pre>
 * Components.tabbedPane()
 *         .addTab("First Tab", new JLabel("First"))
 *         .addTab("Second Tab", new JLabel("Second"))
 *         .build();
 *
 * Components.tabbedPane()
 *         .tabPlacement(SwingConstants.TOP)
 *         .tabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
 *         .tabBuilder("First Tab", new JLabel("First"))
 *         .mnemonic(KeyEvent.VK_1)
 *         .toolTipText("This is the first tab")
 *         .icon(firstTabIcon)
 *         .add()
 *         .tabBuilder("Second Tab", new JLabel("Second"))
 *         .mnemonic(KeyEvent.VK_2)
 *         .toolTipText("This is the second tab")
 *         .icon(secondTabIcon)
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
   * @param tabLayoutPolicy the tab layout policy
   * @return this builder instance
   * @see JTabbedPane#setTabLayoutPolicy(int)
   */
  TabbedPaneBuilder tabLayoutPolicy(int tabLayoutPolicy);

  /**
   * @param changeListener the change listener
   * @return this builder instance
   */
  TabbedPaneBuilder changeListener(ChangeListener changeListener);

  /**
   * Adds a tab to this tabbed pane builder
   * @param title the tab title
   * @param component the component to display in the tab
   * @return this builder instance
   */
  TabbedPaneBuilder addTab(String title, JComponent component);

  /**
   * Returns a new {@link TabBuilder} for adding a tab
   * @param component the component to display in the tab
   * @return a new {@link TabBuilder} instance
   */
  TabBuilder tabBuilder(JComponent component);

  /**
   * Returns a new {@link TabBuilder} for adding a tab
   * @param title the tab title
   * @param component the component to display in the tab
   * @return a new {@link TabBuilder} instance
   */
  TabBuilder tabBuilder(String title, JComponent component);

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
     * @param tabComponent the tab component
     * @return this builder instance
     * @see JTabbedPane#setTabComponentAt(int, Component)
     */
    TabBuilder tabComponent(JComponent tabComponent);

    /**
     * Adds this tab and returns the {@link TabbedPaneBuilder}
     * @return the {@link TabbedPaneBuilder} instance
     */
    TabbedPaneBuilder add();
  }
}
