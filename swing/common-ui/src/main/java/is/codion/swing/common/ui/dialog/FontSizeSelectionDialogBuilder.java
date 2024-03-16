/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Control;

import javax.swing.JComponent;
import java.util.OptionalInt;

/**
 * Builds a dialog for selecting the font size.
 */
public interface FontSizeSelectionDialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this builder
   */
  FontSizeSelectionDialogBuilder owner(JComponent owner);

  /**
   * Displays a dialog allowing the user the select a font size multiplier.
   * @return the selected font size multiplier, an empty Optional if cancelled
   */
  OptionalInt selectFontSize();

  /**
   * Creates a {@link Control} for selecting the font size.
   * @return a Control for displaying a dialog for selecting a font size
   */
  Control createControl();
}
