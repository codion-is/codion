/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
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
   * Also sets the {@link #locationRelativeTo(JComponent)} using the given component.
   * @param owner the dialog parent component
   * @return this builder instance
   */
  B owner(JComponent owner);

  /**
   * @param component the component for the relative location
   * @return this builder instance
   */
  B locationRelativeTo(JComponent component);

  /**
   * @param title the dialog title
   * @return this builder instance
   */
  B title(String title);

  /**
   * @param icon the dialog icon
   * @return this builder instance
   */
  B icon(ImageIcon icon);
}
