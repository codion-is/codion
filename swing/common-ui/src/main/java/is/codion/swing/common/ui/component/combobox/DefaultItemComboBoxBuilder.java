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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.itemComboBoxModel;
import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.sortedItemComboBoxModel;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultItemComboBoxBuilder<T> extends AbstractComponentBuilder<T, JComboBox<Item<T>>, ItemComboBoxBuilder<T>>
        implements ItemComboBoxBuilder<T> {

  private final List<Item<T>> items;
  private final List<ItemListener> itemListeners = new ArrayList<>();

  private ItemComboBoxModel<T> comboBoxModel;
  private Comparator<Item<T>> comparator;
  private boolean sorted = true;
  private boolean nullable;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();
  private boolean mouseWheelScrolling = true;
  private boolean mouseWheelScrollingWithWrapAround = false;
  private int maximumRowCount = -1;
  private int popupWidth = 0;
  private ListCellRenderer<Item<T>> renderer;
  private ComboBoxEditor editor;

  DefaultItemComboBoxBuilder(List<Item<T>> items, Value<T> linkedValue) {
    super(linkedValue);
    this.items = requireNonNull(items);
    preferredHeight(preferredTextFieldHeight());
  }

  DefaultItemComboBoxBuilder(ItemComboBoxModel<T> comboBoxModel, Value<T> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = requireNonNull(comboBoxModel);
    this.items = Collections.emptyList();
    preferredHeight(preferredTextFieldHeight());
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
  public ItemComboBoxBuilder<T> comparator(Comparator<Item<T>> comparator) {
    if (comboBoxModel != null) {
      throw new IllegalStateException("ComboBoxModel has been set, which controls the sorting comparator");
    }
    this.comparator = comparator;
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
  public ItemComboBoxBuilder<T> renderer(ListCellRenderer<Item<T>> renderer) {
    this.renderer = requireNonNull(renderer);
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> editor(ComboBoxEditor editor) {
    this.editor = requireNonNull(editor);
    return this;
  }

  @Override
  public ItemComboBoxBuilder<T> itemListener(ItemListener itemListener) {
    this.itemListeners.add(requireNonNull(itemListener));
    return this;
  }

  @Override
  protected JComboBox<Item<T>> createComponent() {
    ItemComboBoxModel<T> itemComboBoxModel = initializeItemComboBoxModel();
    JComboBox<Item<T>> comboBox = new FocusableComboBox<>(itemComboBoxModel);
    Completion.enable(comboBox, completionMode);
    if (renderer != null) {
      comboBox.setRenderer(renderer);
    }
    if (editor != null) {
      comboBox.setEditor(editor);
    }
    if (mouseWheelScrolling) {
      comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(itemComboBoxModel, false));
    }
    if (mouseWheelScrollingWithWrapAround) {
      comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBoxModel, true));
    }
    if (maximumRowCount >= 0) {
      comboBox.setMaximumRowCount(maximumRowCount);
    }
    itemListeners.forEach(new AddItemListener(comboBox));
    if (Utilities.systemOrCrossPlatformLookAndFeelEnabled()) {
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
    Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    if (comboBoxModel == null) {
      List<Item<T>> modelItems = new ArrayList<>(items);
      if (nullable && !modelItems.contains(nullItem)) {
        modelItems.add(0, nullItem);
      }
      if (comparator != null) {
        comboBoxModel = sortedItemComboBoxModel(modelItems, comparator);
      }
      else if (sorted) {
        comboBoxModel = sortedItemComboBoxModel(modelItems);
      }
      else {
        comboBoxModel = itemComboBoxModel(modelItems);
      }
    }
    if (nullable && comboBoxModel.containsItem(nullItem)) {
      comboBoxModel.setSelectedItem(null);
    }

    return comboBoxModel;
  }

  private static final class AddItemListener implements Consumer<ItemListener> {

    private final JComboBox<?> comboBox;

    private AddItemListener(JComboBox<?> comboBox) {
      this.comboBox = comboBox;
    }

    @Override
    public void accept(ItemListener listener) {
      comboBox.addItemListener(listener);
    }
  }
}
