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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.listbox;

import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

final class ListComboBox<T> extends JComboBox<T> {

	private final ComponentValue<? extends JComponent, T> componentValue;

	private ListComboBox(DefaultBuilder<T> builder) {
		super(FilterComboBoxModel.builder()
						.items(Collections.<T>emptyList())
						.build());
		this.componentValue = builder.componentValue;
		setEditor(new Editor<>(componentValue));
		setEditable(true);
		setRenderer(new Renderer<>(horizontalAlignment(componentValue.component()), builder.formatter));
		KeyEvents.builder()
						.keyCode(VK_INSERT)
						.action(command(this::addItem))
						.enable(componentValue.component());
		KeyEvents.builder()
						.keyCode(VK_DELETE)
						.action(command(this::removeItem))
						.enable(componentValue.component());
		KeyEvents.builder()
						.keyCode(VK_DELETE)
						.modifiers(MENU_SHORTCUT_MASK)
						.action(command(this::clear))
						.enable(componentValue.component());
	}

	@Override
	public FilterComboBoxModel<T> getModel() {
		return (FilterComboBoxModel<T>) super.getModel();
	}

	ComponentValue<? extends JComponent, T> componentValue() {
		return componentValue;
	}

	private void addItem() {
		FilterComboBoxModel<T> comboBoxModel = getModel();
		if (!componentValue.isNull() && !comboBoxModel.items().contains(componentValue.getOrThrow())) {
			comboBoxModel.items().add(componentValue.getOrThrow());
			componentValue.clear();
			if (isPopupVisible()) {
				hidePopup();
				showPopup();
			}
		}
	}

	private void removeItem() {
		FilterComboBoxModel<T> comboBoxModel = getModel();
		int index = getSelectedIndex();
		if (index != -1) {
			T selecteditem = comboBoxModel.getSelectedItem();
			comboBoxModel.items().remove(selecteditem);
			setSelectedIndex(Math.min(index, comboBoxModel.getSize() - 1));
		}
	}

	private void clear() {
		getModel().items().clear();
	}

	private static int horizontalAlignment(JComponent component) {
		if (component instanceof JTextField) {
			return ((JTextField) component).getHorizontalAlignment();
		}

		return CENTER;
	}

	private static final class Editor<T> implements ComboBoxEditor {

		private final ComponentValue<? extends JComponent, T> itemValue;

		private Editor(ComponentValue<? extends JComponent, T> itemValue) {
			this.itemValue = itemValue;
		}

		@Override
		public void setItem(Object anObject) {
			itemValue.set((T) anObject);
		}

		@Override
		public Component getEditorComponent() {
			return itemValue.component();
		}

		@Override
		public @Nullable Object getItem() {
			return itemValue.get();
		}

		@Override
		public void selectAll() {}

		@Override
		public void addActionListener(ActionListener l) {}

		@Override
		public void removeActionListener(ActionListener l) {}
	}

	private static final class Renderer<T> implements ListCellRenderer<T> {

		private final Function<T, String> formatter;

		private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

		private Renderer(int horizontalAlignment, Function<T, String> formatter) {
			this.formatter = formatter;
			listCellRenderer.setHorizontalAlignment(horizontalAlignment);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends T> list, T value,
		                                              int index, boolean isSelected, boolean cellHasFocus) {
			return listCellRenderer.getListCellRendererComponent(list, formatter.apply(value), index, isSelected, cellHasFocus);
		}
	}

	static final class DefaultBuilder<T>
 					extends AbstractComponentValueBuilder<JComboBox<T>, Set<T>, ListComboBoxBuilder<T>>
					implements ListComboBoxBuilder<T> {

		static final ComponentStep ITEM = new DefaultComponentStep();

		private final ComponentValue<? extends JComponent, T> componentValue;

		private Function<T, String> formatter = new DefaultFormatter<>();

		private DefaultBuilder(ComponentValue<? extends JComponent, T> componentValue) {
			this.componentValue = componentValue;
		}

		@Override
		public ListComboBoxBuilder<T> formatter(Function<T, String> formatter) {
			this.formatter = requireNonNull(formatter);
			return this;
		}

		@Override
		protected JComboBox<T> createComponent() {
			return new ListComboBox<>(this);
		}

		@Override
		protected ComponentValue<JComboBox<T>, Set<T>> createValue(JComboBox<T> comboBox) {
			return new ListBoxComponentValue<>((ListComboBox<T>) comboBox);
		}

		private static final class ListBoxComponentValue<T>
						extends AbstractComponentValue<JComboBox<T>, Set<T>>
						implements ComponentValue<JComboBox<T>, Set<T>> {

			public ListBoxComponentValue(ListComboBox<T> comboBox) {
				super(comboBox, emptySet());
				comboBox.componentValue().addListener(this::notifyObserver);
				comboBox.getModel().addListDataListener(new ComboBoxModelListener());
			}

			@Override
			protected Set<T> getComponentValue() {
				ListComboBox<T> comboBox = (ListComboBox<T>) component();
				List<T> items = new ArrayList<>(comboBox.getModel().getSize() + 1);
				comboBox.componentValue().optional().ifPresent(items::add);
				items.addAll(comboBox.getModel().items().included().get());
				items.sort(comparing(Objects::toString));

				return new LinkedHashSet<>(items);
			}

			@Override
			protected void setComponentValue(Set<T> value) {
				((ListComboBox<T>) component()).getModel().items().set(value);
			}

			private final class ComboBoxModelListener implements ListDataListener {

				@Override
				public void intervalAdded(ListDataEvent e) {
					notifyObserver();
				}

				@Override
				public void intervalRemoved(ListDataEvent e) {
					notifyObserver();
				}

				@Override
				public void contentsChanged(ListDataEvent e) {
					notifyObserver();
				}
			}
		}

		private static final class DefaultFormatter<T> implements Function<T, String> {

			@Override
			public String apply(Object value) {
				return value == null ? "" : value.toString();
			}
		}

		private static final class DefaultComponentStep implements ComponentStep {

			@Override
			public <T> ListComboBoxBuilder<T> component(ComponentValue<? extends JComponent, T> component) {
				return new DefaultBuilder<>(requireNonNull(component));
			}
		}
	}
}
