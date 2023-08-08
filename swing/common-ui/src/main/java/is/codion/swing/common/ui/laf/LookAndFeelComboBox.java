/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.Configuration;
import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.component.combobox.ComboBoxBuilder.enableMouseWheelSelection;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.enableLookAndFeel;
import static java.util.Objects.requireNonNull;

/**
 * A combo box for selecting a LookAndFeel.
 * Instantiate via factory methods {@link #lookAndFeelComboBox()} or {@link #lookAndFeelComboBox(boolean)}.
 * @see #lookAndFeelComboBox()
 * @see #lookAndFeelComboBox(boolean)
 * @see LookAndFeelProvider#addLookAndFeelProvider(javax.swing.UIManager.LookAndFeelInfo)
 * @see LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)
 */
public final class LookAndFeelComboBox extends JComboBox<Item<LookAndFeelProvider>> {

  /**
   * Specifies whether to change the Look and Feel dynamically when selecting<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CHANGE_ON_SELECTION =
          Configuration.booleanValue("is.codion.swing.common.ui.dialog.LookAndFeelComboBox.changeOnSelection", false);

  private final LookAndFeelProvider originalLookAndFeel;

  private LookAndFeelComboBox(FilteredComboBoxModel<Item<LookAndFeelProvider>> comboBoxModel, boolean changeOnSelection) {
    super(requireNonNull(comboBoxModel));
    Item<LookAndFeelProvider> selectedValue = comboBoxModel.selectedValue();
    originalLookAndFeel = selectedValue == null ? null : selectedValue.get();
    setRenderer(new LookAndFeelRenderer());
    setEditor(new LookAndFeelEditor());
    enableMouseWheelSelection(this);
    if (changeOnSelection) {
      comboBoxModel.addSelectionListener(lookAndFeelProvider ->
              SwingUtilities.invokeLater(() -> enableLookAndFeel(lookAndFeelProvider.get())));
    }
  }

  @Override
  public FilteredComboBoxModel<Item<LookAndFeelProvider>> getModel() {
    return (FilteredComboBoxModel<Item<LookAndFeelProvider>>) super.getModel();
  }

  /**
   * @return the currently selected look and feel
   */
  public LookAndFeelProvider selectedLookAndFeel() {
    return getModel().selectedValue().get();
  }

  /**
   * Enables the currently selected look and feel, if it is already selected, this method does nothing
   */
  public void enableSelected() {
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    if (!selectedLookAndFeel().lookAndFeelInfo().getClassName().equals(currentLookAndFeelClassName)) {
      enableLookAndFeel(selectedLookAndFeel());
    }
  }

  /**
   * Reverts the look and feel to the look and feel active when this look and feel combobox was created,
   * if it is already enabled, this method does nothing
   */
  public void revert() {
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    if (originalLookAndFeel != null && !currentLookAndFeelClassName.equals(originalLookAndFeel.lookAndFeelInfo().getClassName())) {
      enableLookAndFeel(originalLookAndFeel);
    }
  }

  /**
   * Instantiates a new {@link LookAndFeelComboBox} displaying the available look and feels
   * @return a new {@link LookAndFeelComboBox} instance
   */
  public static LookAndFeelComboBox lookAndFeelComboBox() {
    return new LookAndFeelComboBox(createLookAndFeelComboBoxModel(), CHANGE_ON_SELECTION.get());
  }

  /**
   * Instantiates a new {@link LookAndFeelComboBox} displaying the available look and feels
   * @param changeOnSelection if true the look and feel is changed dynamically when selected
   * @return a new {@link LookAndFeelComboBox} instance
   */
  public static LookAndFeelComboBox lookAndFeelComboBox(boolean changeOnSelection) {
    return new LookAndFeelComboBox(createLookAndFeelComboBoxModel(), changeOnSelection);
  }

  private static final class LookAndFeelEditor extends BasicComboBoxEditor {

    private final LookAndFeelPanel panel = new LookAndFeelPanel();

    private Item<LookAndFeelProvider> item;

    @Override
    public Component getEditorComponent() {
      return panel;
    }

    @Override
    public Object getItem() {
      return item;
    }

    @Override
    public void setItem(Object item) {
      this.item = (Item<LookAndFeelProvider>) item;
      if (this.item != null) {
        panel.setLookAndFeel(this.item.get(), false);
      }
    }
  }

  private static final class LookAndFeelRenderer implements ListCellRenderer<Item<LookAndFeelProvider>> {

    private final LookAndFeelPanel panel = new LookAndFeelPanel();

    @Override
    public Component getListCellRendererComponent(JList<? extends Item<LookAndFeelProvider>> list, Item<LookAndFeelProvider> value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      if (value != null) {
        panel.setLookAndFeel(value.get(), isSelected);
      }

      return panel;
    }
  }

  private static FilteredComboBoxModel<Item<LookAndFeelProvider>> createLookAndFeelComboBoxModel() {
    FilteredComboBoxModel<Item<LookAndFeelProvider>> comboBoxModel = new FilteredComboBoxModel<>();
    comboBoxModel.setItems(initializeAvailableLookAndFeels());
    currentLookAndFeel(comboBoxModel).ifPresent(comboBoxModel::setSelectedItem);

    return comboBoxModel;
  }

  private static List<Item<LookAndFeelProvider>> initializeAvailableLookAndFeels() {
    return LookAndFeelProvider.lookAndFeelProviders().values().stream()
            .sorted(Comparator.comparing(lookAndFeelProvider -> lookAndFeelProvider.lookAndFeelInfo().getName()))
            .map(provider -> Item.item(provider, provider.lookAndFeelInfo().getName()))
            .collect(Collectors.toList());
  }

  private static Optional<Item<LookAndFeelProvider>> currentLookAndFeel(FilteredComboBoxModel<Item<LookAndFeelProvider>> comboBoxModel) {
    String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();

    return comboBoxModel.items().stream()
            .filter(item -> item.get().lookAndFeelInfo().getClassName().equals(currentLookAndFeelClassName))
            .findFirst();
  }
}
