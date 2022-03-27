/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.item.Item;
import is.codion.common.model.UserPreferences;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelDialogBuilder implements LookAndFeelSelectionDialogBuilder {

  private JComponent dialogOwner;
  private boolean changeDuringSelection = CHANGE_LOOK_AND_FEEL_DURING_SELECTION.get();
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
                        UserPreferences.putUserPreference(userPreferencePropertyName, provider.getName());
                      }
                    }))
            .caption(caption)
            .build();
  }

  @Override
  public Optional<LookAndFeelProvider> selectLookAndFeel() {
    List<Item<LookAndFeelProvider>> items = new ArrayList<>();
    Value<Item<LookAndFeelProvider>> currentLookAndFeel = Value.value();
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    LookAndFeelProvider.getLookAndFeelProviders().values().stream()
            .sorted(Comparator.comparing(LookAndFeelProvider::getName))
            .map(provider -> Item.item(provider, provider.getName()))
            .forEach(item -> {
              items.add(item);
              if (currentLookAndFeelClassName.equals(item.getValue().getClassName())) {
                currentLookAndFeel.set(item);
              }
            });
    ItemComboBoxModel<LookAndFeelProvider> comboBoxModel = ItemComboBoxModel.createModel(items);
    currentLookAndFeel.toOptional().ifPresent(comboBoxModel::setSelectedItem);
    if (changeDuringSelection) {
      comboBoxModel.addSelectionListener(lookAndFeelProvider ->
              LookAndFeelProvider.enableLookAndFeel(lookAndFeelProvider.getValue()));
    }

    JComboBox<Item<LookAndFeelProvider>> comboBox = Components.comboBox(comboBoxModel)
            .completionMode(Completion.Mode.NONE)
            .mouseWheelScrolling(true)
            .build();

    ResourceBundle resourceBundle = ResourceBundle.getBundle(LookAndFeelProvider.class.getName());
    String dialogTitle = resourceBundle.getString("select_look_and_feel");

    int option = JOptionPane.showOptionDialog(dialogOwner, comboBox, dialogTitle,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
    LookAndFeelProvider selectedLookAndFeel = comboBoxModel.getSelectedValue().getValue();
    if (option == JOptionPane.OK_OPTION) {
      if (!changeDuringSelection) {
        LookAndFeelProvider.enableLookAndFeel(selectedLookAndFeel);
      }

      return Optional.of(selectedLookAndFeel);
    }
    if (changeDuringSelection && currentLookAndFeel.get().getValue() != selectedLookAndFeel) {
      LookAndFeelProvider.enableLookAndFeel(currentLookAndFeel.get().getValue());
    }

    return Optional.empty();
  }
}
