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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.Configuration;
import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

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

import static is.codion.swing.common.model.component.combobox.FilterComboBoxModel.filterComboBoxModel;
import static is.codion.swing.common.ui.component.combobox.ComboBoxBuilder.enableMouseWheelSelection;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.enableLookAndFeel;
import static java.util.Objects.requireNonNull;

/**
 * A combo box for selecting a LookAndFeel.
 * Instantiate via factory methods {@link #lookAndFeelComboBox()} or {@link #lookAndFeelComboBox(boolean)}.
 * @see #lookAndFeelComboBox()
 * @see #lookAndFeelComboBox(boolean)
 * @see LookAndFeelProvider#addLookAndFeel(javax.swing.UIManager.LookAndFeelInfo)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelProvider)
 */
public final class LookAndFeelComboBox extends JComboBox<Item<LookAndFeelProvider>> {

	/**
	 * Specifies whether to enable the Look and Feel dynamically when selected
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> ENABLE_ON_SELECTION =
					Configuration.booleanValue(LookAndFeelComboBox.class.getName() + ".enableOnSelection", true);

	private final LookAndFeelProvider originalLookAndFeel;

	private LookAndFeelComboBox(FilterComboBoxModel<Item<LookAndFeelProvider>> comboBoxModel, boolean enableOnSelection) {
		super(requireNonNull(comboBoxModel));
		Item<LookAndFeelProvider> selectedValue = comboBoxModel.selection().value();
		originalLookAndFeel = selectedValue == null ? null : selectedValue.value();
		setRenderer(new LookAndFeelRenderer());
		setEditor(new LookAndFeelEditor());
		enableMouseWheelSelection(this);
		if (enableOnSelection) {
			comboBoxModel.selection().item().addConsumer(lookAndFeelProvider ->
							SwingUtilities.invokeLater(() -> enableLookAndFeel(lookAndFeelProvider.value())));
		}
	}

	@Override
	public FilterComboBoxModel<Item<LookAndFeelProvider>> getModel() {
		return (FilterComboBoxModel<Item<LookAndFeelProvider>>) super.getModel();
	}

	/**
	 * @return the currently selected look and feel
	 */
	public LookAndFeelProvider selectedLookAndFeel() {
		return getModel().selection().value().value();
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
		return new LookAndFeelComboBox(createLookAndFeelComboBoxModel(), ENABLE_ON_SELECTION.get());
	}

	/**
	 * Instantiates a new {@link LookAndFeelComboBox} displaying the available look and feels
	 * @param enableOnSelection if true the look and feel is enabled dynamically when selected
	 * @return a new {@link LookAndFeelComboBox} instance
	 */
	public static LookAndFeelComboBox lookAndFeelComboBox(boolean enableOnSelection) {
		return new LookAndFeelComboBox(createLookAndFeelComboBoxModel(), enableOnSelection);
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
				panel.setLookAndFeel(this.item.value(), false);
			}
		}
	}

	private static final class LookAndFeelRenderer implements ListCellRenderer<Item<LookAndFeelProvider>> {

		private final LookAndFeelPanel panel = new LookAndFeelPanel();

		@Override
		public Component getListCellRendererComponent(JList<? extends Item<LookAndFeelProvider>> list, Item<LookAndFeelProvider> value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			if (value != null) {
				panel.setLookAndFeel(value.value(), isSelected);
			}

			return panel;
		}
	}

	private static FilterComboBoxModel<Item<LookAndFeelProvider>> createLookAndFeelComboBoxModel() {
		FilterComboBoxModel<Item<LookAndFeelProvider>> comboBoxModel = filterComboBoxModel(initializeAvailableLookAndFeels());
		currentLookAndFeel(comboBoxModel).ifPresent(comboBoxModel::setSelectedItem);

		return comboBoxModel;
	}

	private static List<Item<LookAndFeelProvider>> initializeAvailableLookAndFeels() {
		return LookAndFeelProvider.lookAndFeelProviders().values().stream()
						.sorted(Comparator.comparing(lookAndFeelProvider -> lookAndFeelProvider.lookAndFeelInfo().getName()))
						.map(provider -> Item.item(provider, provider.lookAndFeelInfo().getName()))
						.collect(Collectors.toList());
	}

	private static Optional<Item<LookAndFeelProvider>> currentLookAndFeel(FilterComboBoxModel<Item<LookAndFeelProvider>> comboBoxModel) {
		String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();

		return comboBoxModel.items().get().stream()
						.filter(item -> item.value().lookAndFeelInfo().getClassName().equals(currentLookAndFeelClassName))
						.findFirst();
	}
}
