/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.JComponent;
import java.util.Optional;

/**
 * Builds a dialog for selecting a look and feel.
 */
public interface LookAndFeelSelectionDialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this builder
   */
  LookAndFeelSelectionDialogBuilder owner(JComponent owner);

  /**
   * @param enableOnSelection true if the Look and Feel should be enabled dynamically when selecting
   * @return this builder
   */
  LookAndFeelSelectionDialogBuilder enableOnSelection(boolean enableOnSelection);

  /**
   * @param userPreferencePropertyName the name of the property to use when saving the selected look and feel as a user preference
   * @return this builder
   */
  LookAndFeelSelectionDialogBuilder userPreferencePropertyName(String userPreferencePropertyName);

  /**
   * Displays a dialog allowing the user the select between all available Look and Feels.
   * @return the selected look and feel provider, an empty Optional if cancelled
   */
  Optional<LookAndFeelProvider> selectLookAndFeel();

  /**
   * Creates a {@link Control} for selecting the Look and Feel.
   * @return a Control for displaying a dialog for selecting a look and feel
   */
  Control createControl();
}
