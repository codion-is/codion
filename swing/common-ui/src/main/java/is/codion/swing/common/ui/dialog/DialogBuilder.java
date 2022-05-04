/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.value.ValueObserver;

import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Window;

/**
 * A base interface for JDialog builders
 * @param <B> the Builder type
 */
public interface DialogBuilder<B extends DialogBuilder<B>> {

  /**
   * @param owner the dialog owner
   * @return this DialogBuilder instance
   */
  B owner(Window owner);

  /**
   * Sets the dialog owner as the parent window of the given component.
   * Also sets the {@link #locationRelativeTo(Component)} using the given component.
   * @param owner the dialog parent component
   * @return this builder instance
   */
  B owner(Component owner);

  /**
   * @param component the component for the relative location
   * @return this builder instance
   */
  B locationRelativeTo(Component component);

  /**
   * @param title the dialog title
   * @return this builder instance
   */
  B title(String title);

  /**
   * @param titleProvider a value observer for a dynamic dialog title
   * @return this builder instance
   */
  B titleProvider(ValueObserver<String> titleProvider);

  /**
   * @param icon the dialog icon
   * @return this builder instance
   */
  B icon(ImageIcon icon);
}
