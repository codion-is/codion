/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.item.Item;
import is.codion.common.model.UserPreferences;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.itemComboBoxModel;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

final class DefaultFontSizeSelectionDialogBuilder implements FontSizeSelectionDialogBuilder {

  private final String userPreferencePropertyName;

  private JComponent owner;

  DefaultFontSizeSelectionDialogBuilder(String userPreferencePropertyName) {
    this.userPreferencePropertyName = requireNonNull(userPreferencePropertyName);
  }

  @Override
  public FontSizeSelectionDialogBuilder owner(JComponent owner) {
    this.owner = requireNonNull(owner);
    return this;
  }

  @Override
  public Control createControl() {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(DefaultFontSizeSelectionDialogBuilder.class.getName());
    String caption = resourceBundle.getString("select_font_size");

    return Control.builder(() -> selectFontSize()
                    .ifPresent(fontSize -> {
                      UserPreferences.setUserPreference(userPreferencePropertyName, Integer.toString(fontSize));
                      JOptionPane.showMessageDialog(owner, resourceBundle.getString("font_size_selected_message"));
                    }))
            .name(caption)
            .build();
  }

  @Override
  public Optional<Integer> selectFontSize() {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(DefaultFontSizeSelectionDialogBuilder.class.getName());
    int currentFontSize = Integer.parseInt(UserPreferences.getUserPreference(userPreferencePropertyName, "100"));
    FontSizeSelectionPanel fontSizeSelectionPanel = new FontSizeSelectionPanel(currentFontSize);
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(fontSizeSelectionPanel)
            .owner(owner)
            .title(resourceBundle.getString("select_font_size"))
            .onOk(() -> okPressed.set(true))
            .show();
    if (okPressed.get()) {
      return Optional.of(fontSizeSelectionPanel.selectedFontSize());
    }

    return Optional.empty();
  }

  private static final class FontSizeSelectionPanel extends JPanel {

    private final ItemComboBoxModel<Integer> fontSizeComboBoxModel;

    private FontSizeSelectionPanel(int currentFontSize) {
      super(Layouts.borderLayout());
      List<Item<Integer>> values = initializeValues();
      this.fontSizeComboBoxModel = itemComboBoxModel(values);
      add(Components.itemComboBox(fontSizeComboBoxModel)
              .initialValue(currentFontSize)
              .renderer(new FontSizeCellRenderer(values, currentFontSize))
              .build(), BorderLayout.CENTER);
      setBorder(createEmptyBorder(10, 10, 0, 10));
    }

    private int selectedFontSize() {
      return fontSizeComboBoxModel.selectedValue().value();
    }

    private static List<Item<Integer>> initializeValues() {
      List<Item<Integer>> values = new ArrayList<>();
      for (int i = 50; i <= 200; i += 5) {
        values.add(Item.item(i, i + "%"));
      }

      return values;
    }

    private static final class FontSizeCellRenderer implements ListCellRenderer<Item<Integer>> {

      private final DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
      private final List<Item<Integer>> values;
      private final Integer currentFontSize;

      private FontSizeCellRenderer(List<Item<Integer>> values, Integer currentFontSize) {
        this.values = values;
        this.currentFontSize = currentFontSize;
      }

      @Override
      public Component getListCellRendererComponent(JList<? extends Item<Integer>> list, Item<Integer> value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
        Component component = defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (index >= 0) {
          Font font = component.getFont();
          int newSize = Math.round(font.getSize() * (values.get(index).value() / (float) currentFontSize.doubleValue()));
          component.setFont(new Font(font.getName(), font.getStyle(), newSize));
        }

        return component;
      }
    }
  }
}
