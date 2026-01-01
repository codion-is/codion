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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.ButtonPanelBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.component.button.ControlPanelBuilder;
import is.codion.swing.common.ui.component.button.MenuBuilder;
import is.codion.swing.common.ui.component.button.MenuItemBuilder;
import is.codion.swing.common.ui.component.button.NullableCheckBoxBuilder;
import is.codion.swing.common.ui.component.button.RadioButtonBuilder;
import is.codion.swing.common.ui.component.button.RadioButtonMenuItemBuilder;
import is.codion.swing.common.ui.component.button.ToggleButtonBuilder;
import is.codion.swing.common.ui.component.button.ToolBarBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.listbox.ListBoxBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.FlexibleGridLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.FlowLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.GridLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.InputPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder.PanelBuilderFactory;
import is.codion.swing.common.ui.component.progressbar.ProgressBarBuilder;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.component.slider.SliderBuilder;
import is.codion.swing.common.ui.component.spinner.ItemSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.ListSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.splitpane.SplitPaneBuilder;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.component.text.FileInputPanel;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.PasswordFieldBuilder;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.common.ui.component.text.TextPaneBuilder;
import is.codion.swing.common.ui.component.tree.TreeBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static is.codion.swing.common.model.component.combobox.FilterComboBoxModel.booleanItems;

/**
 * A factory for {@link ComponentBuilder} instances.
 */
public final class Components {

	private Components() {}

	/**
	 * @param <B> the builder type
	 * @return a JButton builder
	 */
	public static <B extends ButtonBuilder<JButton, Void, B>> ButtonBuilder<JButton, Void, B> button() {
		return ButtonBuilder.builder();
	}

	/**
	 * @return a JCheckBox builder
	 */
	public static CheckBoxBuilder checkBox() {
		return CheckBoxBuilder.builder();
	}

	/**
	 * @return a NullableCheckBox builder
	 */
	public static NullableCheckBoxBuilder nullableCheckBox() {
		return NullableCheckBoxBuilder.builder();
	}

	/**
	 * @return a JRadioButton builder
	 */
	public static RadioButtonBuilder radioButton() {
		return RadioButtonBuilder.builder();
	}

	/**
	 * @param <B> the builder type
	 * @return a JToggleButton builder
	 */
	public static <B extends ToggleButtonBuilder<JToggleButton, B>> ToggleButtonBuilder<JToggleButton, B> toggleButton() {
		return ToggleButtonBuilder.builder();
	}

	/**
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a new JMenuItem builder
	 */
	public static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> menuItem() {
		return MenuItemBuilder.builder();
	}

	/**
	 * @param <B> the builder type
	 * @return a new JCheckBoxMenuItem builder
	 */
	public static <B extends CheckBoxMenuItemBuilder<B>> CheckBoxMenuItemBuilder<B> checkBoxMenuItem() {
		return CheckBoxMenuItemBuilder.builder();
	}

	/**
	 * @param <B> the builder type
	 * @return a new JRadioButtonMenuItem builder
	 */
	public static <B extends RadioButtonMenuItemBuilder<B>> RadioButtonMenuItemBuilder<B> radioButtonMenuItem() {
		return RadioButtonMenuItemBuilder.builder();
	}

	/**
	 * @return a boolean based {@link ItemComboBoxBuilder}
	 */
	public static ItemComboBoxBuilder<Boolean> booleanComboBox() {
		return ItemComboBoxBuilder.builder().items(booleanItems());
	}

	/**
	 * @return a {@link ItemComboBoxBuilder.BuilderFactory}
	 */
	public static ItemComboBoxBuilder.BuilderFactory itemComboBox() {
		return ItemComboBoxBuilder.builder();
	}

	/**
	 * @return a {@link ComboBoxBuilder.ModelStep}
	 */
	public static ComboBoxBuilder.ModelStep comboBox() {
		return ComboBoxBuilder.builder();
	}

	/**
	 * Creates a {@link JComboBox} based {@link ComponentValue} instance, represented by the items
	 * in the combo box (as opposed to the selected item). The provided {@code itemValue} supplies
	 * new items to add to the combo box.
	 * <ul>
	 * <li>{@link java.awt.event.KeyEvent#VK_INSERT} adds the current value to the list
	 * <li>{@link java.awt.event.KeyEvent#VK_DELETE} deletes the selected item from the list.
	 * </ul>
	 * @return a new {@link ListBoxBuilder.ItemValueStep}
	 */
	public static ListBoxBuilder.ItemValueStep listBox() {
		return ListBoxBuilder.builder();
	}

	/**
	 * @return a {@link TemporalFieldPanel.Builder.TemporalClassStep} builder
	 */
	public static TemporalFieldPanel.Builder.TemporalClassStep temporalFieldPanel() {
		return TemporalFieldPanel.builder();
	}

	/**
	 * @return a {@link LocalTime} based {@link TemporalFieldPanel} builder
	 */
	public static TemporalFieldPanel.Builder<LocalTime> localTimeFieldPanel() {
		return temporalFieldPanel().temporalClass(LocalTime.class);
	}

	/**
	 * @return a {@link LocalDate} based {@link TemporalFieldPanel} builder
	 */
	public static TemporalFieldPanel.Builder<LocalDate> localDateFieldPanel() {
		return temporalFieldPanel().temporalClass(LocalDate.class);
	}

	/**
	 * @return a {@link LocalDateTime} based {@link TemporalFieldPanel} builder
	 */
	public static TemporalFieldPanel.Builder<LocalDateTime> localDateTimeFieldPanel() {
		return temporalFieldPanel().temporalClass(LocalDateTime.class);
	}

	/**
	 * @return a {@link TextFieldPanel} builder
	 */
	public static TextFieldPanel.Builder textFieldPanel() {
		return TextFieldPanel.builder();
	}

	/**
	 * @return a JTextArea builder
	 */
	public static TextAreaBuilder textArea() {
		return TextAreaBuilder.builder();
	}

	/**
	 * @return a JTextPane builder
	 */
	public static TextPaneBuilder textPane() {
		return TextPaneBuilder.builder();
	}

	/**
	 * @param <B> the builder type
	 * @return a JTextField builder
	 */
	public static <B extends TextFieldBuilder<JTextField, String, B>> TextFieldBuilder<JTextField, String, B> stringField() {
		return TextFieldBuilder.builder().valueClass(String.class);
	}

	/**
	 * @param <B> the builder type
	 * @return a JTextField builder
	 */
	public static <B extends TextFieldBuilder<JTextField, Character, B>> TextFieldBuilder<JTextField, Character, B> characterField() {
		return TextFieldBuilder.builder().valueClass(Character.class);
	}

	/**
	 * @return a JTextField builder
	 */
	public static TextFieldBuilder.ValueClassStep textField() {
		return TextFieldBuilder.builder();
	}

	/**
	 * @return a {@link LocalTime} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<LocalTime> localTimeField() {
		return TemporalField.builder().temporalClass(LocalTime.class);
	}

	/**
	 * @return a {@link LocalDate} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<LocalDate> localDateField() {
		return TemporalField.builder().temporalClass(LocalDate.class);
	}

	/**
	 * @return a {@link LocalDateTime} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<LocalDateTime> localDateTimeField() {
		return TemporalField.builder().temporalClass(LocalDateTime.class);
	}

	/**
	 * @return a {@link OffsetDateTime} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<OffsetDateTime> offsetDateTimeField() {
		return TemporalField.builder().temporalClass(OffsetDateTime.class);
	}

	/**
	 * @return a {@link TemporalField} builder
	 */
	public static TemporalField.Builder.TemporalClassStep temporalField() {
		return TemporalField.builder();
	}

	/**
	 * @return a Short based {@link NumberField} builder
	 */
	public static NumberField.Builder<Short> shortField() {
		return NumberField.builder().numberClass(Short.class);
	}

	/**
	 * @return an Integer based {@link NumberField} builder
	 */
	public static NumberField.Builder<Integer> integerField() {
		return NumberField.builder().numberClass(Integer.class);
	}

	/**
	 * @return a Long based {@link NumberField} builder
	 */
	public static NumberField.Builder<Long> longField() {
		return NumberField.builder().numberClass(Long.class);
	}

	/**
	 * @return a Double based {@link NumberField} builder
	 */
	public static NumberField.Builder<Double> doubleField() {
		return NumberField.builder().numberClass(Double.class);
	}

	/**
	 * @return a BigDecimal based {@link NumberField} builder
	 */
	public static NumberField.Builder<BigDecimal> bigDecimalField() {
		return NumberField.builder().numberClass(BigDecimal.class);
	}

	/**
	 * @return a JFormattedTextField builder
	 */
	public static MaskedTextFieldBuilder maskedTextField() {
		return MaskedTextFieldBuilder.builder();
	}

	/**
	 * @return a JPasswordField builder
	 */
	public static PasswordFieldBuilder passwordField() {
		return PasswordFieldBuilder.builder();
	}

	/**
	 * @return a Double based JSpinner builder
	 */
	public static NumberSpinnerBuilder<Double> doubleSpinner() {
		return NumberSpinnerBuilder.builder()
						.numberClass(Double.class);
	}

	/**
	 * @return an Integer based JSpinner builder
	 */
	public static NumberSpinnerBuilder<Integer> integerSpinner() {
		return NumberSpinnerBuilder.builder()
						.numberClass(Integer.class);
	}

	/**
	 * @param <T> the value type
	 * @return a JSpinner builder
	 */
	public static <T> ListSpinnerBuilder<T> listSpinner() {
		return ListSpinnerBuilder.builder();
	}

	/**
	 * @param <T> the value type
	 * @return a JSpinner builder
	 */
	public static <T> ItemSpinnerBuilder<T> itemSpinner() {
		return ItemSpinnerBuilder.builder();
	}

	/**
	 * @return a JSlider builder
	 */
	public static SliderBuilder.ModelStep slider() {
		return SliderBuilder.builder();
	}

	/**
	 * @return a JTree builder
	 */
	public static TreeBuilder.ModelStep tree() {
		return TreeBuilder.builder();
	}

	/**
	 * @param <T> the type to display in the label (using value.toString() or "" for null).
	 * @return a JLabel builder
	 */
	public static <T> LabelBuilder<T> label() {
		return LabelBuilder.builder();
	}

	/**
	 * @param <T> the type to display in the label (using value.toString() or "" for null).
	 * @param text the label text
	 * @return a JLabel builder
	 */
	public static <T> LabelBuilder<T> label(String text) {
		return LabelBuilder.builder(text);
	}

	/**
	 * @return a JPanel builder factory
	 */
	public static PanelBuilderFactory panel() {
		return PanelBuilder.builder();
	}

	/**
	 * @return a {@link InputPanelBuilder}
	 */
	public static InputPanelBuilder inputPanel() {
		return InputPanelBuilder.builder();
	}

	/**
	 * @return a JTabbedPane builder
	 */
	public static TabbedPaneBuilder tabbedPane() {
		return TabbedPaneBuilder.builder();
	}

	/**
	 * @return a JSplitPane builder
	 */
	public static SplitPaneBuilder splitPane() {
		return SplitPaneBuilder.builder();
	}

	/**
	 * @return a JScrollPane builder
	 */
	public static ScrollPaneBuilder scrollPane() {
		return ScrollPaneBuilder.builder();
	}

	/**
	 * @return an indeterminate JProgressBar builder
	 */
	public static ProgressBarBuilder progressBar() {
		return ProgressBarBuilder.builder();
	}

	/**
	 * Provides builder for a {@link Path} based file input panel.
	 * @return a {@link FileInputPanel} builder
	 */
	public static FileInputPanel.Builder<Path> pathInputPanel() {
		return FileInputPanel.builder().path();
	}

	/**
	 * Provides builder for a byte array based file input panel.
	 * @return a {@link FileInputPanel} builder
	 */
	public static FileInputPanel.Builder<byte[]> byteArrayInputPanel() {
		return FileInputPanel.builder().byteArray();
	}

	/**
	 * @return a {@link javax.swing.JToolBar} builder
	 */
	public static ControlPanelBuilder.ControlsStep<JToolBar, ToolBarBuilder> toolBar() {
		return ToolBarBuilder.builder();
	}

	/**
	 * @return a button panel builder
	 */
	public static ControlPanelBuilder.ControlsStep<JPanel, ButtonPanelBuilder> buttonPanel() {
		return ButtonPanelBuilder.builder();
	}

	/**
	 * Creates a new {@link BorderLayoutPanelBuilder} instance using a new
	 * {@link BorderLayout} instance with the default horizontal and vertical gap.
	 * @return a border layout panel builder
	 * @see Layouts#GAP
	 */
	public static BorderLayoutPanelBuilder borderLayoutPanel() {
		return panel().borderLayout();
	}

	/**
	 * Creates a new {@link PanelBuilder} instance using a new {@link java.awt.GridLayout}
	 * with the default horizontal and vertical gap.
	 * @param rows the number of rows
	 * @param columns the number of columns
	 * @return a grid layout panel builder
	 * @see Layouts#GAP
	 */
	public static GridLayoutPanelBuilder gridLayoutPanel(int rows, int columns) {
		return panel().gridLayout(rows, columns);
	}

	/**
	 * Creates a new {@link PanelBuilder} instance using a new
	 * {@link is.codion.swing.common.ui.layout.FlexibleGridLayout} with the default horizontal and vertical gap.
	 * @param rows the number of rows
	 * @param columns the number of columns
	 * @return a flexible grid layout panel builder
	 * @see Layouts#GAP
	 */
	public static FlexibleGridLayoutPanelBuilder flexibleGridLayoutPanel(int rows, int columns) {
		return panel().flexibleGridLayout(rows, columns);
	}

	/**
	 * Creates a new {@link PanelBuilder} instance using a new {@link java.awt.FlowLayout} with the default
	 * horizontal and vertical gap.
	 * @param alignment the flow layout alignment
	 * @return a flow layout panel builder
	 * @see Layouts#GAP
	 */
	public static FlowLayoutPanelBuilder flowLayoutPanel(int alignment) {
		return panel().flowLayout(alignment);
	}

	/**
	 * @return a new menu builder
	 */
	public static MenuBuilder.ControlsStep menu() {
		return MenuBuilder.builder();
	}
}
