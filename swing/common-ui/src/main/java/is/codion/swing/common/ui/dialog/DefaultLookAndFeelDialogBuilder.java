/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.UserPreferences;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.laf.LookAndFeelSelectionPanel;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelDialogBuilder implements LookAndFeelSelectionDialogBuilder {

  private JComponent dialogOwner;
  private boolean changeDuringSelection = LookAndFeelSelectionPanel.CHANGE_DURING_SELECTION.get();
  private String userPreferencePropertyName;

  @Override
  public LookAndFeelSelectionDialogBuilder dialogOwner(JComponent dialogOwner) {
    this.dialogOwner = requireNonNull(dialogOwner);
    return this;
  }

  @Override
  public LookAndFeelSelectionDialogBuilder changeDuringSelection(boolean changeDuringSelection) {
    this.changeDuringSelection = changeDuringSelection;
    return this;
  }

  @Override
  public LookAndFeelSelectionDialogBuilder userPreferencePropertyName(String userPreferencePropertyName) {
    this.userPreferencePropertyName = userPreferencePropertyName;
    return this;
  }

  @Override
  public Control createControl() {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(LookAndFeelProvider.class.getName());
    String caption = resourceBundle.getString("select_look_and_feel");

    return Control.builder(() -> selectLookAndFeel()
                    .ifPresent(provider -> {
                      if (userPreferencePropertyName != null) {
                        UserPreferences.putUserPreference(userPreferencePropertyName, provider.name());
                      }
                    }))
            .caption(caption)
            .build();
  }

  @Override
  public Optional<LookAndFeelProvider> selectLookAndFeel() {
    LookAndFeelSelectionPanel lookAndFeelSelectionPanel = new LookAndFeelSelectionPanel(changeDuringSelection);
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(lookAndFeelSelectionPanel)
            .owner(dialogOwner)
            .title(ResourceBundle.getBundle(LookAndFeelProvider.class.getName()).getString("select_look_and_feel"))
            .onOk(() -> okPressed.set(true))
            .show();
    if (okPressed.get()) {
      lookAndFeelSelectionPanel.enableSelected();

      return Optional.of(lookAndFeelSelectionPanel.selectedLookAndFeel());
    }
    lookAndFeelSelectionPanel.revert();

    return Optional.empty();
  }
}
