/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JFormattedTextField;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a formatted text field.
 */
public interface FormattedTextFieldBuilder extends ComponentBuilder<String, JFormattedTextField> {

  @Override
  FormattedTextFieldBuilder preferredHeight(int preferredHeight);

  @Override
  FormattedTextFieldBuilder preferredWidth(int preferredWidth);

  @Override
  FormattedTextFieldBuilder preferredSize(Dimension preferredSize);

  @Override
  FormattedTextFieldBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  FormattedTextFieldBuilder enabledState(StateObserver enabledState);

  @Override
  FormattedTextFieldBuilder onBuild(Consumer<JFormattedTextField> onBuild);

  /**
   * @return this builder instance
   */
  FormattedTextFieldBuilder formatMaskString(String formatMaskString);

  /**
   * @return this builder instance
   */
  FormattedTextFieldBuilder valueContainsLiterals(boolean valueContainsLiterals);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  FormattedTextFieldBuilder updateOn(UpdateOn updateOn);

  /**
   * @return this builder instance
   */
  FormattedTextFieldBuilder columns(int columns);
}
