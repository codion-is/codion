/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
   * @param verticalUnitIncrement the unit increment for the vertical scrollbar
   * @return this builder instance
   */
  ScrollPaneBuilder verticalUnitIncrement(int verticalUnitIncrement);

  /**
   * @param horizontalUnitIncrement the unit increment for the horizontal scrollbar
   * @return this builder instance
   */
  ScrollPaneBuilder horizontalUnitIncrement(int horizontalUnitIncrement);

  /**
   * @param verticalBlockIncrement the block increment for the vertical scrollbar
   * @return this builder instance
   */
  ScrollPaneBuilder verticalBlockIncrement(int verticalBlockIncrement);

  /**
   * @param horizontalBlockIncrement the block increment for the horizontal scrollbar
   * @return this builder instance
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
   */
  ScrollPaneBuilder layout(LayoutManager layout);
}
