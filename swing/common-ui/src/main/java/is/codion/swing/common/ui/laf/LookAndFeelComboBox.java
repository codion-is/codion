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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
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
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static is.codion.common.item.Item.item;
import static is.codion.swing.common.ui.Utilities.systemLookAndFeelClassName;
import static is.codion.swing.common.ui.component.combobox.ComboBoxBuilder.enableMouseWheelSelection;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeels;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static javax.swing.UIManager.getCrossPlatformLookAndFeelClassName;
import static javax.swing.UIManager.getLookAndFeel;

/**
 * A combo box for selecting a LookAndFeel.
 * Instantiate via factory methods {@link #lookAndFeelComboBox()} or {@link #lookAndFeelComboBox(boolean)}.
 * @see #lookAndFeelComboBox()
 * @see #lookAndFeelComboBox(boolean)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelInfo)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelEnabler)
 */
public final class LookAndFeelComboBox extends JComboBox<Item<LookAndFeelEnabler>> {

	/**
	 * The look and feel to enable when no look and feel is selected.
	 */
	public enum DefaultLookAndFeel {
		/**
		 * The system look and feel
		 */
		SYSTEM,
		/**
		 * The cross-platform look and feel
		 */
		CROSS_PLATFORM,
	}

	/**
	 * Species which look and feel to use when none is selected in the combo box.
	 * <ul>
	 * <li>Value type: {@link DefaultLookAndFeel}
	 * <li>Default value: {@link DefaultLookAndFeel#SYSTEM}
	 * </ul>
	 */
	public static final PropertyValue<DefaultLookAndFeel> DEFAULT_LOOK_AND_FEEL =
					Configuration.enumValue(LookAndFeelComboBox.class.getName() + ".defaultLookAndFeel",
									DefaultLookAndFeel.class, DefaultLookAndFeel.SYSTEM);

	/**
	 * Specifies whether to enable the Look and Feel dynamically when selected
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> ENABLE_ON_SELECTION =
					Configuration.booleanValue(LookAndFeelComboBox.class.getName() + ".enableOnSelection", true);

	private static final LookAndFeelEnabler CROSS_PLATFORM_LOOK_AND_FEEL =
					LookAndFeelEnabler.lookAndFeelEnabler(new LookAndFeelInfo("Cross Platform", getCrossPlatformLookAndFeelClassName()));

	private static final LookAndFeelEnabler SYSTEM_LOOK_AND_FEEL =
					LookAndFeelEnabler.lookAndFeelEnabler(new LookAndFeelInfo("System", systemLookAndFeelClassName()));

	private final LookAndFeelEnabler originalLookAndFeel;

	private LookAndFeelComboBox(FilterComboBoxModel<Item<LookAndFeelEnabler>> comboBoxModel, boolean enableOnSelection) {
		super(requireNonNull(comboBoxModel));
		originalLookAndFeel = comboBoxModel.selection().item().optional()
						.map(Item::value)
						.orElse(LookAndFeelEnabler.lookAndFeelEnabler(new LookAndFeelInfo("Original", getLookAndFeel().getName())));
		setRenderer(new LookAndFeelRenderer());
		setEditor(new LookAndFeelEditor());
		enableMouseWheelSelection(this);
		if (enableOnSelection) {
			comboBoxModel.selection().item().addConsumer(lookAndFeelProvider ->
							SwingUtilities.invokeLater(() -> lookAndFeelProvider.value().enable()));
		}
	}

	@Override
	public FilterComboBoxModel<Item<LookAndFeelEnabler>> getModel() {
		return (FilterComboBoxModel<Item<LookAndFeelEnabler>>) super.getModel();
	}

	/**
	 * @return the currently selected look and feel
	 */
	public LookAndFeelEnabler selectedLookAndFeel() {
		return getModel().selection().item().getOrThrow().value();
	}

	/**
	 * Enables the currently selected look and feel, if it is already selected, this method does nothing
	 */
	public void enableSelected() {
		String currentLookAndFeelClassName = getLookAndFeel().getClass().getName();
		if (!selectedLookAndFeel().lookAndFeelInfo().getClassName().equals(currentLookAndFeelClassName)) {
			selectedLookAndFeel().enable();
		}
	}

	/**
	 * Reverts the look and feel to the look and feel active when this look and feel combobox was created,
	 * if it is already enabled, this method does nothing
	 */
	public void revert() {
		String currentLookAndFeelClassName = getLookAndFeel().getClass().getName();
		if (originalLookAndFeel != null && !currentLookAndFeelClassName.equals(originalLookAndFeel.lookAndFeelInfo().getClassName())) {
			originalLookAndFeel.enable();
		}
	}

	/**
	 * Instantiates a new {@link LookAndFeelComboBox} displaying the available look and feels
	 * @return a new {@link LookAndFeelComboBox} instance
	 */
	public static LookAndFeelComboBox lookAndFeelComboBox() {
		return new LookAndFeelComboBox(createLookAndFeelComboBoxModel(), ENABLE_ON_SELECTION.getOrThrow());
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

		private Item<LookAndFeelEnabler> item;

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
			this.item = (Item<LookAndFeelEnabler>) item;
			if (this.item != null) {
				panel.setLookAndFeel(this.item.value(), false);
			}
		}
	}

	private static final class LookAndFeelRenderer implements ListCellRenderer<Item<LookAndFeelEnabler>> {

		private final LookAndFeelPanel panel = new LookAndFeelPanel();

		@Override
		public Component getListCellRendererComponent(JList<? extends Item<LookAndFeelEnabler>> list, Item<LookAndFeelEnabler> value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			if (value != null) {
				panel.setLookAndFeel(value.value(), isSelected);
			}

			return panel;
		}
	}

	private static FilterComboBoxModel<Item<LookAndFeelEnabler>> createLookAndFeelComboBoxModel() {
		List<Item<LookAndFeelEnabler>> items = new ArrayList<>(initializeAvailableLookAndFeels());
		DefaultLookAndFeel defaultLookAndFeel = DEFAULT_LOOK_AND_FEEL.get();
		items.add(0, item(defaultLookAndFeel == DefaultLookAndFeel.SYSTEM ? SYSTEM_LOOK_AND_FEEL : CROSS_PLATFORM_LOOK_AND_FEEL));
		FilterComboBoxModel<Item<LookAndFeelEnabler>> comboBoxModel = FilterComboBoxModel.builder(items).build();
		currentLookAndFeel(comboBoxModel).ifPresent(comboBoxModel::setSelectedItem);

		return comboBoxModel;
	}

	private static List<Item<LookAndFeelEnabler>> initializeAvailableLookAndFeels() {
		return lookAndFeels().stream()
						.sorted(comparing(lookAndFeelProvider -> lookAndFeelProvider.lookAndFeelInfo().getName()))
						.map(provider -> item(provider, provider.lookAndFeelInfo().getName()))
						.collect(Collectors.toList());
	}

	private static Optional<Item<LookAndFeelEnabler>> currentLookAndFeel(FilterComboBoxModel<Item<LookAndFeelEnabler>> comboBoxModel) {
		String currentLookAndFeelClassName = getLookAndFeel().getClass().getName();

		return comboBoxModel.items().get().stream()
						.filter(item -> item.value().lookAndFeelInfo().getClassName().equals(currentLookAndFeelClassName))
						.findFirst();
	}
}
