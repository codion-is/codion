/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
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
    return new DefaultTabbedPanelLayout.DefaultBuilder();
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
