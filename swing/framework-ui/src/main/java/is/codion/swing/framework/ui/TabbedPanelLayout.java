/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.framework.ui.EntityPanel.PanelLayout;
import is.codion.swing.framework.ui.EntityPanel.PanelState;

/**
 * A {@link PanelLayout} implementation based on a JTabbedPane.
 */
public interface TabbedPanelLayout extends PanelLayout {

  /**
   * Specifies whether actions to hide detail panels or show them in a dialog are available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> INCLUDE_DETAIL_PANEL_CONTROLS =
          Configuration.booleanValue("is.codion.swing.framework.ui.TabEntityPanelLayout.includeDetailPanelControls", true);

  /**
   * @param detailPanelState the detail panel state
   * @return a new {@link TabbedPanelLayout} with the given detail panel state
   */
  static TabbedPanelLayout detailPanelState(PanelState detailPanelState) {
    return builder().detailPanelState(detailPanelState).build();
  }

  /**
   * @param splitPaneResizeWeight the split pane resize weight
   * @return a new {@link TabbedPanelLayout} with the given split pane resize weight
   */
  static TabbedPanelLayout splitPaneResizeWeight(double splitPaneResizeWeight) {
    return builder().splitPaneResizeWeight(splitPaneResizeWeight).build();
  }

  /**
   * @return a new {@link TabbedPanelLayout.Builder} instance
   */
  static Builder builder() {
    return new DefaultTabPanelLayout.DefaultBuilder();
  }

  /**
   * Builds a {@link TabbedPanelLayout}.
   */
  interface Builder {

    /**
     * @param detailPanelState the initial detail panel state
     * @return this builder instance
     */
    Builder detailPanelState(PanelState detailPanelState);

    /**
     * @param splitPaneResizeWeight the detail panel split pane size weight
     * @return this builder instance
     */
    Builder splitPaneResizeWeight(double splitPaneResizeWeight);

    /**
     * @param includeDetailTabPane true if the detail panel tab pane should be included
     * @return this builder instance
     */
    Builder includeDetailTabPane(boolean includeDetailTabPane);

    /**
     * @param includeDetailPanelControls true if detail panel controls should be available
     * @return this builder instance
     */
    Builder includeDetailPanelControls(boolean includeDetailPanelControls);

    /**
     * @return a new {@link TabbedPanelLayout} instance based on this builder
     */
    TabbedPanelLayout build();
  }
}
