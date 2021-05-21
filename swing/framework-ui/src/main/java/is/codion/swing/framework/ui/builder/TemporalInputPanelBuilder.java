/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.UpdateOn;

import java.awt.Dimension;
import java.time.temporal.Temporal;
import java.util.function.Consumer;

/**
 * Builds a TemporalInputPanel.
 * @param <T> the temporal type
 */
public interface TemporalInputPanelBuilder<T extends Temporal> extends ComponentBuilder<T, TemporalInputPanel<T>> {

  @Override
  TemporalInputPanelBuilder<T> preferredHeight(int preferredHeight);

  @Override
  TemporalInputPanelBuilder<T> preferredWidth(int preferredWidth);

  @Override
  TemporalInputPanelBuilder<T> preferredSize(Dimension preferredSize);

  @Override
  TemporalInputPanelBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  TemporalInputPanelBuilder<T> enabledState(StateObserver enabledState);

  @Override
  TemporalInputPanelBuilder<T> onBuild(Consumer<TemporalInputPanel<T>> onBuild);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> updateOn(UpdateOn updateOn);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> columns(int columns);

  /**
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> calendarButton(boolean calendarButton);
}
