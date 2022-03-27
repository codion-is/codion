/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.JComponent;
import java.util.Optional;

/**
 * Builds a dialog for selecting a look and feel.
 */
public interface LookAndFeelSelectionDialogBuilder {

  /**
   * Specifies whether to change the Look and Feel dynamically when choosing<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> CHANGE_LOOK_AND_FEEL_DURING_SELECTION =
          Configuration.booleanValue("codion.swing.lookAndFeel.changeDuringSelection", false);

  /**
   * @param dialogOwner the dialog owner
   * @return this builder
   */
  LookAndFeelSelectionDialogBuilder dialogOwner(JComponent dialogOwner);

  /**
   * @param changeDuringSelection true if the Look and Feel should change dynamically when choosing
   * @return this builder
   */
  LookAndFeelSelectionDialogBuilder changeDuringSelection(boolean changeDuringSelection);

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
