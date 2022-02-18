/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A builder for JLabel.
 * @param <T> the type to display in the label (using value.toString() or "" for null).
 */
public interface LabelBuilder<T> extends ComponentBuilder<T, JLabel, LabelBuilder<T>> {

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.CENTER)<br>
   * Default value: SwingConstants.LEADING
   */
  PropertyValue<Integer> LABEL_TEXT_ALIGNMENT = Configuration.integerValue(
          "is.codion.swing.common.ui.LabelBuilder.labelTextAlignment", SwingConstants.LEADING);

  /**
   * @param horizontalAlignment the horizontal text alignment
   * @return this builder instance
   */
  LabelBuilder<T> horizontalAlignment(int horizontalAlignment);

  /**
   * @param displayedMnemonic the label mnemonic
   * @return this builder instance
   */
  LabelBuilder<T> displayedMnemonic(int displayedMnemonic);

  /**
   * @param component the component to associate with this label
   * @return this builder instance
   */
  LabelBuilder<T> labelFor(JComponent component);

  /**
   * @param icon the label icon
   * @return this builder instance
   */
  LabelBuilder<T> icon(Icon icon);

  /**
   * @param iconTextGap the icon text gap
   * @return this builder instance
   */
  LabelBuilder<T> iconTextGap(int iconTextGap);
}
