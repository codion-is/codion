/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JScrollPane;
import java.awt.LayoutManager;

/**
 * A builder for JScrollPane
 */
public interface ScrollPaneBuilder extends ComponentBuilder<Void, JScrollPane, ScrollPaneBuilder> {

  /**
   * @param verticalScrollBarPolicy the vertical scroll bar policy
   * @return this builder instance
   */
  ScrollPaneBuilder verticalScrollBarPolicy(int verticalScrollBarPolicy);

  /**
   * @param horizontalScrollBarPolicy the horizontal scroll bar policy
   * @return this builder instance
   */
  ScrollPaneBuilder horizontalScrollBarPolicy(int horizontalScrollBarPolicy);

  /**
   * @param wheelScrollingEnabled wheel scrolling enabled
   * @return this builder instance
   */
  ScrollPaneBuilder wheelScrollingEnable(boolean wheelScrollingEnabled);

  /**
   * @param layout the layout manager
   * @return this builder instance
   */
  ScrollPaneBuilder layout(LayoutManager layout);
}
