/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * A builder for a JTabbedPane.
 */
public interface TabbedPaneBuilder extends ComponentBuilder<Void, JTabbedPane, TabbedPaneBuilder> {

  /**
   * @param tabPlacement the tab placement
   * @return this builder instance
   */
  TabbedPaneBuilder tabPlacement(int tabPlacement);

  /**
   * @param title the tab title
   * @param component the tab component
   * @return this builder instance
   */
  TabbedPaneBuilder tab(String title, JComponent component);

  /**
   * @param index the tab index
   * @param mnemonic the tab mnemonic
   * @return this builder instance
   */
  TabbedPaneBuilder mnemonicAt(int index, int mnemonic);

  /**
   * @param changeListener the change listener
   * @return this builder instance
   */
  TabbedPaneBuilder changeListener(ChangeListener changeListener);
}
