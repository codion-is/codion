/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.DefaultComboBoxBuilder.CopyEditorActionsListener;
import is.codion.swing.common.ui.component.DefaultComboBoxBuilder.SteppedComboBoxUI;
import is.codion.swing.common.ui.component.combobox.ComboBoxMouseWheelListener;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static is.codion.swing.common.ui.component.text.TextComponents.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultItemComboBoxBuilder<T> extends AbstractComponentBuilder<T, JComboBox<Item<T>>, ItemComboBoxBuilder<T>>
        implements ItemComboBoxBuilder<T> {

  private final List<Item<T>> items;

  private ItemComboBoxModel<T> comboBoxModel;
  private Comparator<Item<T>> sortComparator;
  private boolean sorted = true;
  private boolean nullable;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();
  private boolean mouseWheelScrolling = true;
  private boolean mouseWheelScrollingWithWrapAround = false;
  private int maximumRowCount = -1;
  private int popupWidth = 0;

  DefaultItemComboBoxBuilder(List<Item<T>> items, Value<T> linkedValue) {
    super(linkedValue);
    this.items = requireNonNull(items);
    preferredHeight(getPreferredTextFieldHeight());
  }

  DefaultItemComboBoxBuilder(ItemComboBoxModel<T> comboBoxModel, Value<T> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = requireNonNull(comboBoxModel);
    this.items = Collections.emptyList();
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public ItemComboBoxBuilder<T> nullable(boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> sorted(boolean sorted) {
    if (comboBoxModel != null) {
      throw new IllegalStateException("ComboBoxModel has been set, which controls the sorting");
    }
    this.sorted = sorted;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> sortComparator(Comparator<Item<T>> sortComparator) {
    if (comboBoxModel != null) {
      throw new IllegalStateException("ComboBoxModel has been set, which controls the sorting comparator");
    }
    this.sortComparator = sortComparator;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> completionMode(Completion.Mode completionMode) {
    this.completionMode = requireNonNull(completionMode);
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> mouseWheelScrolling(boolean mouseWheelScrolling) {
    this.mouseWheelScrolling = mouseWheelScrolling;
    if (mouseWheelScrolling) {
      this.mouseWheelScrollingWithWrapAround = false;
    }
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround) {
    this.mouseWheelScrollingWithWrapAround = mouseWheelScrollingWithWrapAround;
    if (mouseWheelScrollingWithWrapAround) {
      this.mouseWheelScrolling = false;
    }
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> maximumRowCount(int maximumRowCount) {
    this.maximumRowCount = maximumRowCount;
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> popupWidth(int popupWidth) {
    this.popupWidth = popupWidth;
    return this;
  }

  @Override
  protected JComboBox<Item<T>> createComponent() {
    ItemComboBoxModel<T> itemComboBoxModel = initializeItemComboBoxModel();
    JComboBox<Item<T>> comboBox = new JComboBox<Item<T>>(itemComboBoxModel) {
      /**
       * Overridden as a workaround for editable combo boxes as initial focus components on
       * detail panels stealing the focus from the parent panel on initialization
       */
      @Override
      public void requestFocus() {
        if (isEditable()) {
          getEditor().getEditorComponent().requestFocus();
        }
        else {
          super.requestFocus();
        }
      }
    };
    Completion.enable(comboBox, completionMode);
    if (mouseWheelScrolling) {
      comboBox.addMouseWheelListener(ComboBoxMouseWheelListener.create(itemComboBoxModel));
    }
    if (mouseWheelScrollingWithWrapAround) {
      comboBox.addMouseWheelListener(ComboBoxMouseWheelListener.createWithWrapAround(comboBoxModel));
    }
    if (maximumRowCount >= 0) {
      comboBox.setMaximumRowCount(maximumRowCount);
    }
    if (LookAndFeelProvider.isSystemLookAndFeelEnabled()) {
      new SteppedComboBoxUI(comboBox, popupWidth);
    }
    comboBox.addPropertyChangeListener("editor", new CopyEditorActionsListener());

    return comboBox;
  }

  @Override
  protected ComponentValue<T, JComboBox<Item<T>>> createComponentValue(JComboBox<Item<T>> component) {
    return new SelectedItemValue<>(component);
  }

  @Override
  protected void setInitialValue(JComboBox<Item<T>> component, T initialValue) {
    component.setSelectedItem(initialValue);
  }

  private ItemComboBoxModel<T> initializeItemComboBoxModel() {
    Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    if (comboBoxModel == null) {
      List<Item<T>> modelItems = new ArrayList<>(items);
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
