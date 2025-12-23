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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.utilities.item.Item;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultItemComboBoxBuilder<T> extends AbstractComponentValueBuilder<JComboBox<Item<T>>, T, ItemComboBoxBuilder<T>>
				implements ItemComboBoxBuilder<T> {

	static final BuilderFactory FACTORY = new DefaultBuilderFactory();

	private final List<Item<T>> items;
	private final List<ItemListener> itemListeners = new ArrayList<>();

	private @Nullable FilterComboBoxModel<Item<T>> comboBoxModel;
	private @Nullable Comparator<Item<T>> comparator;
	private boolean sorted = false;
	private boolean nullable;
	private Completion.Mode completionMode = Completion.COMPLETION_MODE.getOrThrow();
	private boolean normalize = true;
	private boolean mouseWheelScrolling = ComboBoxBuilder.MOUSE_WHEEL_SCROLLING.getOrThrow();
	private boolean mouseWheelScrollingWithWrapAround = false;
	private int maximumRowCount = -1;
	private int popupWidth = 0;
	private @Nullable ListCellRenderer<Item<T>> renderer;
	private @Nullable ComboBoxEditor editor;

	DefaultItemComboBoxBuilder(List<Item<T>> items) {
		this.items = new ArrayList<>(requireNonNull(items));
		preferredHeight(preferredTextFieldHeight());
	}

	DefaultItemComboBoxBuilder(FilterComboBoxModel<Item<T>> comboBoxModel) {
		this.comboBoxModel = requireNonNull(comboBoxModel);
		this.items = Collections.emptyList();
		value(comboBoxModel.getSelectedItem() == null ? null : comboBoxModel.getSelectedItem().get());
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
	public ItemComboBoxBuilder<T> comparator(@Nullable Comparator<Item<T>> comparator) {
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
	public ItemComboBoxBuilder<T> normalize(boolean normalize) {
		this.normalize = normalize;
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
	public ItemComboBoxBuilder<T> renderer(@Nullable ListCellRenderer<Item<T>> renderer) {
		this.renderer = renderer;
		return this;
	}

	@Override
	public ItemComboBoxBuilder<T> editor(@Nullable ComboBoxEditor editor) {
		this.editor = editor;
		return this;
	}

	@Override
	public ItemComboBoxBuilder<T> itemListener(ItemListener itemListener) {
		this.itemListeners.add(requireNonNull(itemListener));
		return this;
	}

	@Override
	protected JComboBox<Item<T>> createComponent() {
		FilterComboBoxModel<Item<T>> itemComboBoxModel = comboBoxModel == null ? createItemComboBoxModel() : comboBoxModel;
		JComboBox<Item<T>> comboBox = new FocusableComboBox<>(itemComboBoxModel);
		if (editor == null) {
			Completion.builder()
							.mode(completionMode)
							.normalize(normalize)
							.enable(comboBox);
		}
		if (renderer != null) {
			comboBox.setRenderer(renderer);
		}
		if (editor != null) {
			comboBox.setEditor(editor);
		}
		if (mouseWheelScrolling) {
			comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBox, false));
		}
		if (mouseWheelScrollingWithWrapAround) {
			comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBox, true));
		}
		if (maximumRowCount >= 0) {
			comboBox.setMaximumRowCount(maximumRowCount);
		}
		itemListeners.forEach(new AddItemListener(comboBox));
		if (DefaultComboBoxBuilder.systemOrCrossPlatformLookAndFeelEnabled()) {
			new SteppedComboBoxUI(comboBox, popupWidth);
		}
		comboBox.addPropertyChangeListener("editor", new CopyEditorActionsListener());

		return comboBox;
	}

	@Override
	protected ComponentValue<JComboBox<Item<T>>, T> createValue(JComboBox<Item<T>> component) {
		return new SelectedItemValue<>(component);
	}

	private FilterComboBoxModel<Item<T>> createItemComboBoxModel() {
		Item<T> nullItem = Item.item(null, FilterComboBoxModel.NULL_CAPTION.getOrThrow());
		if (nullable && !items.contains(nullItem)) {
			items.add(0, nullItem);
		}
		if (comparator != null) {
			comboBoxModel = FilterComboBoxModel.builder()
							.items(items)
							.sorted(comparator)
							.build();
		}
		else if (sorted) {
			comboBoxModel = FilterComboBoxModel.builder()
							.items(items)
							.sorted(true)
							.build();
		}
		else {
			comboBoxModel = FilterComboBoxModel.builder()
							.items(items)
							.build();
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

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public <T> ItemComboBoxBuilder<T> model(FilterComboBoxModel<Item<T>> comboBoxModel) {
			return new DefaultItemComboBoxBuilder<>(comboBoxModel);
		}

		@Override
		public <T> ItemComboBoxBuilder<T> items(List<Item<T>> items) {
			return new DefaultItemComboBoxBuilder<>(items);
		}
	}
}
