/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.UserPreferences;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.laf.LookAndFeelComboBox.lookAndFeelComboBox;
import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelSelectionDialogBuilder implements LookAndFeelSelectionDialogBuilder {

  private static final int PADDING = 10;

  private JComponent owner;
  private boolean enableOnSelection = LookAndFeelComboBox.ENABLE_ON_SELECTION.get();
  private String userPreferencePropertyName;

  @Override
  public LookAndFeelSelectionDialogBuilder owner(JComponent owner) {
    this.owner = requireNonNull(owner);
    return this;
  }

  @Override
  public LookAndFeelSelectionDialogBuilder enableOnSelection(boolean enableOnSelection) {
    this.enableOnSelection = enableOnSelection;
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
                        UserPreferences.setUserPreference(userPreferencePropertyName, provider.lookAndFeelInfo().getClassName());
                      }
                    }))
            .name(caption)
            .build();
  }

  @Override
  public Optional<LookAndFeelProvider> selectLookAndFeel() {
    LookAndFeelComboBox lookAndFeelComboBox = lookAndFeelComboBox(enableOnSelection);
    JPanel basePanel = new JPanel(new BorderLayout());
    basePanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));
    basePanel.add(lookAndFeelComboBox, BorderLayout.CENTER);
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(basePanel)
            .owner(owner)
            .title(ResourceBundle.getBundle(LookAndFeelProvider.class.getName()).getString("select_look_and_feel"))
            .onOk(() -> okPressed.set(true))
            .show();
    if (okPressed.get()) {
      lookAndFeelComboBox.enableSelected();

      return Optional.of(lookAndFeelComboBox.selectedLookAndFeel());
    }
    lookAndFeelComboBox.revert();

    return Optional.empty();
  }
}
