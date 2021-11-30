/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultItemComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<Item<T>>, ItemComboBoxBuilder<T>>
        implements ItemComboBoxBuilder<T> {

  private final List<Item<T>> items;

  private ItemComboBoxModel<T> comboBoxModel;
  private int popupWidth;
  private Comparator<Item<T>> sortComparator;
  private boolean sorted = true;
  private boolean nullable;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();

  DefaultItemComboBoxBuilder(final List<Item<T>> items, final Value<T> linkedValue) {
    super(linkedValue);
    this.items = requireNonNull(items);
    preferredHeight(getPreferredTextFieldHeight());
  }

  DefaultItemComboBoxBuilder(final ItemComboBoxModel<T> comboBoxModel, final Value<T> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = requireNonNull(comboBoxModel);
    this.items = Collections.emptyList();
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public ItemComboBoxBuilder<T> nullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> popupWidth(final int popupWidth) {
    this.popupWidth = popupWidth;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> sorted(final boolean sorted) {
    if (comboBoxModel != null) {
      throw new IllegalStateException("ComboBoxModel has been set, which controls the sorting");
    }
    this.sorted = sorted;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> sortComparator(final Comparator<Item<T>> sortComparator) {
    if (comboBoxModel != null) {
      throw new IllegalStateException("ComboBoxModel has been set, which controls the sorting comparator");
    }
    this.sortComparator = sortComparator;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> completionMode(final Completion.Mode completionMode) {
    this.completionMode = requireNonNull(completionMode);
    return this;
  }

  @Override
  protected SteppedComboBox<Item<T>> buildComponent() {
    final ItemComboBoxModel<T> itemComboBoxModel = initializeItemComboBoxModel();
    final SteppedComboBox<Item<T>> comboBox = new SteppedComboBox<>(itemComboBoxModel);
    Completion.enable(comboBox, completionMode);
    if (popupWidth > 0) {
      comboBox.setPopupWidth(popupWidth);
    }

    return comboBox;
  }

  @Override
  protected ComponentValue<T, SteppedComboBox<Item<T>>> buildComponentValue(final SteppedComboBox<Item<T>> component) {
    return ComponentValues.itemComboBox(component);
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<Item<T>> component) {
    component.setTransferFocusOnEnter(true);
    TransferFocusOnEnter.enable((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected void setInitialValue(final SteppedComboBox<Item<T>> component, final T initialValue) {
    component.setSelectedItem(initialValue);
  }

  private ItemComboBoxModel<T> initializeItemComboBoxModel() {
    final Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    if (comboBoxModel == null) {
      final List<Item<T>> modelItems = new ArrayList<>(items);
      if (nullable && !modelItems.contains(nullItem)) {
        modelItems.add(0, nullItem);
      }
      if (sortComparator != null) {
        comboBoxModel = ItemComboBoxModel.createSortedModel(modelItems, sortComparator);
      }
      else if (sorted) {
        comboBoxModel = ItemComboBoxModel.createSortedModel(modelItems);
      }
      else {
        comboBoxModel = ItemComboBoxModel.createModel(modelItems);
      }
    }
    if (nullable && comboBoxModel.containsItem(nullItem)) {
      comboBoxModel.setSelectedItem(nullItem);
    }

    return comboBoxModel;
  }
}
