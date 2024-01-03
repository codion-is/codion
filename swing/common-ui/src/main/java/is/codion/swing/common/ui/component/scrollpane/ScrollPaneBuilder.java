/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.scrollpane;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.awt.LayoutManager;

/**
 * A builder for JScrollPane
 */
public interface ScrollPaneBuilder extends ComponentBuilder<Void, JScrollPane, ScrollPaneBuilder> {

  /**
   * @param verticalScrollBarPolicy the vertical scroll bar policy
   * @return this builder instance
   * @see JScrollPane#setVerticalScrollBarPolicy(int)
   */
  ScrollPaneBuilder verticalScrollBarPolicy(int verticalScrollBarPolicy);

  /**
   * @param horizontalScrollBarPolicy the horizontal scroll bar policy
   * @return this builder instance
   * @see JScrollPane#setHorizontalScrollBarPolicy(int)
   */
  ScrollPaneBuilder horizontalScrollBarPolicy(int horizontalScrollBarPolicy);

  /**
   * @param verticalUnitIncrement the unit increment for the vertical scrollbar
   * @return this builder instance
   * @see javax.swing.JScrollBar#setUnitIncrement(int)
   */
  ScrollPaneBuilder verticalUnitIncrement(int verticalUnitIncrement);

  /**
   * @param horizontalUnitIncrement the unit increment for the horizontal scrollbar
   * @return this builder instance
   * @see javax.swing.JScrollBar#setUnitIncrement(int)
   */
  ScrollPaneBuilder horizontalUnitIncrement(int horizontalUnitIncrement);

  /**
   * @param verticalBlockIncrement the block increment for the vertical scrollbar
   * @return this builder instance
   * @see javax.swing.JScrollBar#setBlockIncrement(int)
   */
  ScrollPaneBuilder verticalBlockIncrement(int verticalBlockIncrement);

  /**
   * @param horizontalBlockIncrement the block increment for the horizontal scrollbar
   * @return this builder instance
   * @see javax.swing.JScrollBar#setBlockIncrement(int)
   */
  ScrollPaneBuilder horizontalBlockIncrement(int horizontalBlockIncrement);

  /**
   * @param wheelScrollingEnabled wheel scrolling enabled
   * @return this builder instance
   */
  ScrollPaneBuilder wheelScrollingEnable(boolean wheelScrollingEnabled);

  /**
   * @param layout the layout manager
   * @return this builder instance
   * @see JScrollPane#setLayout(LayoutManager)
   */
  ScrollPaneBuilder layout(LayoutManager layout);

  /**
   * @param view the view component
   * @return a new {@link ScrollPaneBuilder} instance
   */
  static ScrollPaneBuilder builder(JComponent view) {
    return new DefaultScrollPaneBuilder(view);
  }
}
