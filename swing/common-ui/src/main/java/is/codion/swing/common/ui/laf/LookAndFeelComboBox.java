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

import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.item.Item.item;
import static is.codion.swing.common.ui.Utilities.enableComponents;
import static is.codion.swing.common.ui.component.combobox.ComboBoxBuilder.enableMouseWheelSelection;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeels;
import static java.util.stream.Collectors.toList;
import static javax.swing.UIManager.getLookAndFeel;

/**
 * A combo box for selecting a LookAndFeel.
 * Instantiate via builder {@link #builder()}.
 * @see #builder()
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelInfo)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelEnabler)
 */
public final class LookAndFeelComboBox extends JComboBox<Item<LookAndFeelEnabler>> {

	/**
	 * <p>Specifies whether to include installed look and feels in the selection combo box by default, if auxiliary ones are provided.
	 * <p>Note that this has no effect if only the look and feels are installed.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see UIManager#getInstalledLookAndFeels()
	 * @see is.codion.swing.common.ui.laf.LookAndFeelProvider
	 */
	public static final PropertyValue<Boolean> INCLUDE_INSTALLED_LOOK_AND_FEELS =
					booleanValue(LookAndFeelComboBox.class.getName() + ".includeInstalledLookAndFeels", false);

	/**
	 * Specifies whether to enable the Look and Feel dynamically when selected
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> ENABLE_ON_SELECTION =
					booleanValue(LookAndFeelComboBox.class.getName() + ".enableOnSelection", true);

	private final LookAndFeelEnabler originalLookAndFeel = createOriginalLookAndFeel();

	private final State includeInstalled;

	private LookAndFeelComboBox(DefaultBuilder builder) {
		super(createLookAndFeelComboBoxModel());
		Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults = new ConcurrentHashMap<>();
		setRenderer(new LookAndFeelRenderer(lookAndFeelDefaults));
		setEditor(new LookAndFeelEditor(lookAndFeelDefaults));
		enableMouseWheelSelection(this);
		getModel().selection().item().set(item(originalLookAndFeel));
		if (builder.enabled != null) {
			enableComponents(builder.enabled, this);
		}
		includeInstalled = State.builder(builder.includeInstalled)
						.listener(getModel().items()::filter)
						.build();
		getModel().items().visible().predicate()
						.set(item -> includeInstalled.get() || !item.value().installed());
		if (builder.onSelection != null) {
			getModel().selection().item().addConsumer(item ->
							builder.onSelection.accept(item.value()));
		}
		if (builder.enableOnSelection) {
			getModel().selection().item().addConsumer(lookAndFeelProvider ->
							SwingUtilities.invokeLater(() -> lookAndFeelProvider.value().enable()));
		}
	}

	@Override
	public FilterComboBoxModel<Item<LookAndFeelEnabler>> getModel() {
		return (FilterComboBoxModel<Item<LookAndFeelEnabler>>) super.getModel();
	}

	/**
	 * @return a {@link State} controlling whether installed look and feels are included
	 */
	public State includeInstalled() {
		return includeInstalled;
	}

	/**
	 * @return the currently selected look and feel
	 */
	public LookAndFeelEnabler selectedLookAndFeel() {
		return getModel().selection().item().getOrThrow().value();
	}

	/**
	 * Enables the currently selected look and feel, if it is already enabled, this method does nothing
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
	 * Instantiates a new {@link LookAndFeelComboBox.Builder} displaying the available look and feels
	 * @return a new {@link LookAndFeelComboBox.Builder} instance
	 */
	public static LookAndFeelComboBox.Builder builder() {
		return new LookAndFeelComboBox.DefaultBuilder();
	}

	/**
 * Builds a {@link LookAndFeelComboBox}
 */
	public interface Builder {

		/**
		 * @param enableOnSelection true if look and feel should be enabled when selected
		 * @return this builder
		 */
		Builder enableOnSelection(boolean enableOnSelection);

		/**
		 * @param includeInstalled true if installed look and feels should be included
		 * @return this builder
		 */
		Builder includeInstalled(boolean includeInstalled);

		/**
		 * @param enabled the enabled observer
		 * @return this builder
		 */
		Builder enabled(ObservableState enabled);

		/**
		 * @param onSelection called when the selection changes
		 * @return this builcer
		 */
		Builder onSelection(Consumer<LookAndFeelEnabler> onSelection);

		/**
		 * @return a new {@link LookAndFeelComboBox}
		 */
		LookAndFeelComboBox build();
	}

	private static final class DefaultBuilder implements Builder {

		private ObservableState enabled;
		private boolean includeInstalled = INCLUDE_INSTALLED_LOOK_AND_FEELS.getOrThrow();
		private boolean enableOnSelection = ENABLE_ON_SELECTION.getOrThrow();
		private Consumer<LookAndFeelEnabler> onSelection;

		@Override
		public Builder enableOnSelection(boolean enableOnSelection) {
			this.enableOnSelection = enableOnSelection;
			return this;
		}

		@Override
		public Builder includeInstalled(boolean includeInstalled) {
			this.includeInstalled = includeInstalled;
			return this;
		}

		@Override
		public Builder enabled(ObservableState enabled) {
			this.enabled = enabled;
			return this;
		}

		@Override
		public Builder onSelection(Consumer<LookAndFeelEnabler> onSelection) {
			this.onSelection = onSelection;
			return this;
		}

		@Override
		public LookAndFeelComboBox build() {
			return new LookAndFeelComboBox(this);
		}
	}

	private static final class LookAndFeelEditor extends BasicComboBoxEditor {

		private final LookAndFeelPanel panel;

		private Item<LookAndFeelEnabler> item;

		private LookAndFeelEditor(Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults) {
			panel = new LookAndFeelPanel(lookAndFeelDefaults);
		}

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

		private final LookAndFeelPanel panel;

		private LookAndFeelRenderer(Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults) {
			panel = new LookAndFeelPanel(lookAndFeelDefaults);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Item<LookAndFeelEnabler>> list, Item<LookAndFeelEnabler> value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			if (value != null) {
				panel.setLookAndFeel(value.value(), isSelected);
			}

			return panel;
		}
	}

	private static LookAndFeelEnabler createOriginalLookAndFeel() {
		LookAndFeel lookAndFeel = getLookAndFeel();

		return new DefaultLookAndFeelEnabler(new LookAndFeelInfo(lookAndFeel.getName(), lookAndFeel.getClass().getName()));
	}

	private static FilterComboBoxModel<Item<LookAndFeelEnabler>> createLookAndFeelComboBoxModel() {
		return FilterComboBoxModel.builder(lookAndFeels().stream()
										.map(provider -> item(provider, provider.lookAndFeelInfo().getName()))
										.collect(toList()))
						.sorted(true)
						.build();
	}
}
