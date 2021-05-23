/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

/**
 * A base interface for JDialog builders
 */
public interface DialogBuilder<B extends DialogBuilder<B>> {

  /**
   * @param owner the dialog owner
   * @return this DialogBuilder instance
   */
  B owner(Window owner);

  /**
   * Sets the dialog owner as the parent window of the given component.
   * @param owner the dialog parent component
   * @return this builder instance
   */
  B owner(JComponent owner);

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
