/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

/**
 * A builder for JSplitPane.
 */
public interface SplitPaneBuilder extends ComponentBuilder<Void, JSplitPane, SplitPaneBuilder> {

  /**
   * @param orientation the orientation
   * @return this builder instance
   */
  SplitPaneBuilder orientation(int orientation);

  /**
   * @param oneTouchExpandable one touch expandable
   * @return this builder instance
   */
  SplitPaneBuilder oneTouchExpandable(boolean oneTouchExpandable);

  /**
   * @param leftComponent the left component
   * @return this builder instance
   */
  SplitPaneBuilder leftComponent(JComponent leftComponent);

  /**
   * @param rightComponent the right component
   * @return this builder instance
   */
  SplitPaneBuilder rightComponent(JComponent rightComponent);

  /**
   * @param topComponent the top component
   * @return this builder instance
   */
  SplitPaneBuilder topComponent(JComponent topComponent);

  /**
   * @param bottomComponent the bottom component
   * @return this builder instance
   */
  SplitPaneBuilder bottomComponent(JComponent bottomComponent);

  /**
   * @param resizeWeight the resize weight
   * @return this builder instance
   */
  SplitPaneBuilder resizeWeight(double resizeWeight);

  /**
   * @param continuousLayout the value of the continuousLayout
   * @return this builder instance
   */
  SplitPaneBuilder continuousLayout(boolean continuousLayout);

  /**
   * @param dividerSize the divider size
   * @return this builder instance
   */
  SplitPaneBuilder dividerSize(int dividerSize);
}
