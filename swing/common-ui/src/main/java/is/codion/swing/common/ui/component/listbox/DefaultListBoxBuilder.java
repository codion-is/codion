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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.listbox;

import is.codion.common.reactive.value.ValueSet;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static javax.swing.SwingConstants.CENTER;

final class DefaultListBoxBuilder<T>
				extends AbstractComponentValueBuilder<JComboBox<T>, Set<T>, ListBoxBuilder<T>>
				implements ListBoxBuilder<T> {

	static final ItemValueStep ITEM = new DefaultItemValueStep();

	private final ComponentValue<? extends JComponent, T> itemValue;
	private final ValueSet<T> linkedValue;

	private Function<Object, String> formatter = new DefaultFormatter();

	DefaultListBoxBuilder(ComponentValue<? extends JComponent, T> itemValue, ValueSet<T> linkedValue) {
		this.itemValue = requireNonNull(itemValue);
		this.linkedValue = requireNonNull(linkedValue);
	}

	@Override
	public ListBoxBuilder<T> formatter(Function<Object, String> formatter) {
		this.formatter = requireNonNull(formatter);
		return this;
	}

	@Override
	protected JComboBox<T> createComponent() {
		FilterComboBoxModel<T> comboBoxModel = FilterComboBoxModel.builder()
						.items(linkedValue.get())
						.build();
		ListComboBox<T> comboBox = new ListComboBox<>(comboBoxModel, itemValue, linkedValue);
		comboBox.setEditor(new Editor<>(itemValue));
		comboBox.setEditable(true);
		comboBox.setRenderer(new Renderer<>(horizontalAlignment(itemValue.component()), formatter));

		return comboBox;
	}

	@Override
	protected ComponentValue<JComboBox<T>, Set<T>> createComponentValue(JComboBox<T> comboBox) {
		return new ListBoxComponentValue<>((ListComboBox<T>) comboBox);
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

		private final Function<Object, String> formatter;

		private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

		private Renderer(int horizontalAlignment, Function<Object, String> formatter) {
			this.formatter = formatter;
			listCellRenderer.setHorizontalAlignment(horizontalAlignment);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends T> list, T value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			return listCellRenderer.getListCellRendererComponent(list, formatter.apply(value), index, isSelected, cellHasFocus);
		}
	}

	private static final class ListBoxComponentValue<T>
					extends AbstractComponentValue<JComboBox<T>, Set<T>>
					implements ComponentValue<JComboBox<T>, Set<T>> {

		public ListBoxComponentValue(ListComboBox<T> comboBox) {
			super(comboBox, emptySet());
			comboBox.linkedValue().addListener(this::notifyObserver);
		}

		@Override
		protected Set<T> getComponentValue() {
			ListComboBox<T> comboBox = (ListComboBox<T>) component();
			FilterComboBoxModel<T> comboBoxModel = comboBox.getModel();

			return Stream.concat(Stream.of(comboBox.itemValue().get()), IntStream.range(0, comboBoxModel.getSize())
											.mapToObj(comboBoxModel::getElementAt))
							.filter(Objects::nonNull)
							.collect(toCollection(LinkedHashSet::new));
		}

		@Override
		protected void setComponentValue(Set<T> value) {
			ListComboBox<T> comboBox = (ListComboBox<T>) component();
			FilterComboBoxModel<T> comboBoxModel = comboBox.getModel();
			comboBoxModel.items().clear();
			value.forEach(comboBoxModel.items()::add);
		}
	}

	private static final class DefaultFormatter implements Function<Object, String> {
		@Override
		public String apply(Object value) {
			return value == null ? "" : value.toString();
		}
	}

	private static final class DefaultItemValueStep implements ItemValueStep {

		@Override
		public <T> LinkedValueBuilder<T> itemValue(ComponentValue<? extends JComponent, T> itemValue) {
			return new DefaultLinkedValueBuilder<>(requireNonNull(itemValue));
		}
	}

	private static final class DefaultLinkedValueBuilder<T> implements LinkedValueBuilder<T> {

		private final ComponentValue<? extends JComponent, T> itemValue;

		private DefaultLinkedValueBuilder(ComponentValue<? extends JComponent, T> itemValue) {
			this.itemValue = requireNonNull(itemValue);
		}

		@Override
		public ListBoxBuilder<T> linkedValue(ValueSet<T> linkedValue) {
			return new DefaultListBoxBuilder<>(itemValue, linkedValue);
		}
	}
}
