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
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.observable.Observable;
import is.codion.common.value.ValueSet;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.ButtonPanelBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.component.button.MenuBuilder;
import is.codion.swing.common.ui.component.button.MenuItemBuilder;
import is.codion.swing.common.ui.component.button.RadioButtonBuilder;
import is.codion.swing.common.ui.component.button.RadioButtonMenuItemBuilder;
import is.codion.swing.common.ui.component.button.ToggleButtonBuilder;
import is.codion.swing.common.ui.component.button.ToolBarBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.list.ListBuilder;
import is.codion.swing.common.ui.component.listbox.ListBoxBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
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
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.List;

import static is.codion.swing.common.model.component.combobox.FilterComboBoxModel.booleanItems;
import static is.codion.swing.common.ui.layout.Layouts.*;

/**
 * A factory for {@link ComponentBuilder} instances.
 */
public final class Components {

	private Components() {}

	/**
	 * @param <B> the builder type
	 * @return a JButton builder
	 */
	public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button() {
		return ButtonBuilder.builder();
	}

	/**
	 * @param <B> the builder type
	 * @param action the button action
	 * @return a JButton builder
	 */
	public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button(Action action) {
		return ButtonBuilder.builder(action);
	}

	/**
	 * @param <B> the builder type
	 * @param control the button control
	 * @return a JButton builder
	 */
	public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button(Control control) {
		return ButtonBuilder.builder(control);
	}

	/**
	 * @param <B> the builder type
	 * @param controlBuilder the button control builder
	 * @return a JButton builder
	 */
	public static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> button(Control.Builder<?, ?> controlBuilder) {
		return ButtonBuilder.builder(controlBuilder);
	}

	/**
	 * @return a JCheckBox builder
	 */
	public static CheckBoxBuilder checkBox() {
		return CheckBoxBuilder.builder();
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
	 * @param action the item action
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a new JMenuItem builder
	 */
	public static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> menuItem(Action action) {
		return MenuItemBuilder.builder(action);
	}

	/**
	 * @param control the item control
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a new JMenuItem builder
	 */
	public static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> menuItem(Control control) {
		return MenuItemBuilder.builder(control);
	}

	/**
	 * @param controlBuilder the item control builder
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a new JMenuItem builder
	 */
	public static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> menuItem(Control.Builder<?, ?> controlBuilder) {
		return MenuItemBuilder.builder(controlBuilder);
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
	 * @return a boolean based JComboBox builder
	 */
	public static ItemComboBoxBuilder<Boolean> booleanComboBox() {
		return ItemComboBoxBuilder.builder(booleanItems());
	}

	/**
	 * @param comboBoxModel the combo box model
	 * @return a boolean based JComboBox builder
	 */
	public static ItemComboBoxBuilder<Boolean> booleanComboBox(FilterComboBoxModel<Item<Boolean>> comboBoxModel) {
		return ItemComboBoxBuilder.builder(comboBoxModel);
	}

	/**
	 * @param comboBoxModel the combo box model
	 * @param <T> the value type
	 * @return a {@link Item} based JComboBox builder
	 */
	public static <T> ItemComboBoxBuilder<T> itemComboBox(FilterComboBoxModel<Item<T>> comboBoxModel) {
		return ItemComboBoxBuilder.builder(comboBoxModel);
	}

	/**
	 * @param values the values
	 * @param <T> the value type
	 * @return a {@link Item} based JComboBox builder
	 */
	public static <T> ItemComboBoxBuilder<T> itemComboBox(List<Item<T>> values) {
		return ItemComboBoxBuilder.builder(values);
	}

	/**
	 * @param <T> the value type
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @param comboBoxModel the combo box model
	 * @return a JComboBox builder
	 */
	public static <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(ComboBoxModel<T> comboBoxModel) {
		return ComboBoxBuilder.builder(comboBoxModel);
	}

	/**
	 * Creates a {@link JComboBox} based {@link ComponentValue} instance, represented by the items
	 * in the combo box (as opposed to the selected item). The provided {@code itemValue} supplies
	 * new items to add to the combo box.
	 * <ul>
	 * <li>{@link java.awt.event.KeyEvent#VK_INSERT} adds the current value to the list
	 * <li>{@link java.awt.event.KeyEvent#VK_DELETE} deletes the selected item from the list.
	 * </ul>
	 * @param itemValue the component value providing the items to add
	 * @param linkedValue the value to link
	 * @param <T> the value type
	 * @return a new {@link ComponentValue}
	 */
	public static <T> ListBoxBuilder<T> listBox(ComponentValue<T, ? extends JComponent> itemValue,
																							ValueSet<T> linkedValue) {
		return ListBoxBuilder.listBox(itemValue, linkedValue);
	}

	/**
	 * @param <T> the value type
	 * @param valueClass the value class
	 * @return a {@link TemporalFieldPanel} builder
	 */
	public static <T extends Temporal> TemporalFieldPanel.Builder<T> temporalFieldPanel(Class<T> valueClass) {
		return TemporalFieldPanel.builder(valueClass);
	}

	/**
	 * @return a {@link LocalTime} based {@link TemporalFieldPanel} builder
	 */
	public static TemporalFieldPanel.Builder<LocalTime> localTimeFieldPanel() {
		return temporalFieldPanel(LocalTime.class);
	}

	/**
	 * @return a {@link LocalDate} based {@link TemporalFieldPanel} builder
	 */
	public static TemporalFieldPanel.Builder<LocalDate> localDateFieldPanel() {
		return temporalFieldPanel(LocalDate.class);
	}

	/**
	 * @return a {@link LocalDateTime} based {@link TemporalFieldPanel} builder
	 */
	public static TemporalFieldPanel.Builder<LocalDateTime> localDateTimeFieldPanel() {
		return temporalFieldPanel(LocalDateTime.class);
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
	public static <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringField() {
		return TextFieldBuilder.builder(String.class);
	}

	/**
	 * @param <B> the builder type
	 * @return a JTextField builder
	 */
	public static <B extends TextFieldBuilder<Character, JTextField, B>> TextFieldBuilder<Character, JTextField, B> characterField() {
		return TextFieldBuilder.builder(Character.class);
	}

	/**
	 * @param <T> the value type
	 * @param <C> the text field type
	 * @param <B> the builder type
	 * @param valueClass the value class
	 * @return a JTextField builder
	 */
	public static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Class<T> valueClass) {
		return TextFieldBuilder.builder(valueClass);
	}

	/**
	 * @return a {@link LocalTime} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<LocalTime> localTimeField() {
		return TemporalField.builder(LocalTime.class);
	}

	/**
	 * @return a {@link LocalDate} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<LocalDate> localDateField() {
		return TemporalField.builder(LocalDate.class);
	}

	/**
	 * @return a {@link LocalDateTime} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<LocalDateTime> localDateTimeField() {
		return TemporalField.builder(LocalDateTime.class);
	}

	/**
	 * @return a {@link OffsetDateTime} based {@link TemporalField} builder
	 */
	public static TemporalField.Builder<OffsetDateTime> offsetDateTimeField() {
		return TemporalField.builder(OffsetDateTime.class);
	}

	/**
	 * @param <T> the temporal type
	 * @param temporalClass the temporal class
	 * @return a {@link TemporalField} builder
	 */
	public static <T extends Temporal> TemporalField.Builder<T> temporalField(Class<T> temporalClass) {
		return TemporalField.builder(temporalClass);
	}

	/**
	 * @return a Short based {@link NumberField} builder
	 */
	public static NumberField.Builder<Short> shortField() {
		return NumberField.builder(Short.class);
	}

	/**
	 * @return an Integer based {@link NumberField} builder
	 */
	public static NumberField.Builder<Integer> integerField() {
		return NumberField.builder(Integer.class);
	}

	/**
	 * @return a Long based {@link NumberField} builder
	 */
	public static NumberField.Builder<Long> longField() {
		return NumberField.builder(Long.class);
	}

	/**
	 * @return a Double based {@link NumberField} builder
	 */
	public static NumberField.Builder<Double> doubleField() {
		return NumberField.builder(Double.class);
	}

	/**
	 * @return a BigDecimal based {@link NumberField} builder
	 */
	public static NumberField.Builder<BigDecimal> bigDecimalField() {
		return NumberField.builder(BigDecimal.class);
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
		return NumberSpinnerBuilder.builder(new SpinnerNumberModel(), Double.class);
	}

	/**
	 * @param spinnerNumberModel the spinner model
	 * @return a Double based JSpinner builder
	 */
	public static NumberSpinnerBuilder<Double> doubleSpinner(SpinnerNumberModel spinnerNumberModel) {
		return NumberSpinnerBuilder.builder(spinnerNumberModel, Double.class);
	}

	/**
	 * @return an Integer based JSpinner builder
	 */
	public static NumberSpinnerBuilder<Integer> integerSpinner() {
		return NumberSpinnerBuilder.builder(new SpinnerNumberModel(), Integer.class);
	}

	/**
	 * @param spinnerNumberModel the spinner model
	 * @return an Integer based JSpinner builder
	 */
	public static NumberSpinnerBuilder<Integer> integerSpinner(SpinnerNumberModel spinnerNumberModel) {
		return NumberSpinnerBuilder.builder(spinnerNumberModel, Integer.class);
	}

	/**
	 * @param <T> the value type
	 * @param spinnerModel the spinner model
	 * @return a JSpinner builder
	 */
	public static <T> ListSpinnerBuilder<T> listSpinner(SpinnerListModel spinnerModel) {
		return ListSpinnerBuilder.builder(spinnerModel);
	}

	/**
	 * @param <T> the value type
	 * @param spinnerModel the spinner model
	 * @return a JSpinner builder
	 */
	public static <T> ItemSpinnerBuilder<T> itemSpinner(SpinnerListModel spinnerModel) {
		return ItemSpinnerBuilder.builder(spinnerModel);
	}

	/**
	 * @param boundedRangeModel the slider model
	 * @return a JSlider builder
	 */
	public static SliderBuilder slider(BoundedRangeModel boundedRangeModel) {
		return SliderBuilder.builder(boundedRangeModel);
	}

	/**
	 * @param listModel the list model to base the list on
	 * @param <T> the list value type
	 * @return a new list builder factory
	 */
	public static <T> ListBuilder.Factory<T> list(FilterListModel<T> listModel) {
		return ListBuilder.factory(listModel);
	}

	/**
	 * @param <T> the type to display in the label (using value.toString() or "" for null).
	 * @return a JLabel builder
	 */
	public static <T> LabelBuilder<T> label() {
		return LabelBuilder.builder((String) null);
	}

	/**
	 * @param <T> the type to display in the label (using value.toString() or "" for null).
	 * @param observable the observable to link to the label text
	 * @return a JLabel builder
	 */
	public static <T> LabelBuilder<T> label(Observable<T> observable) {
		return LabelBuilder.builder(observable);
	}

	/**
	 * @param <T> the type to display in the label (using value.toString() or "" for null).
	 * @param icon the label icon
	 * @return a JLabel builder
	 */
	public static <T> LabelBuilder<T> label(Icon icon) {
		return LabelBuilder.builder(icon);
	}

	/**
	 * @param text the label text
	 * @return a JLabel builder
	 */
	public static LabelBuilder<String> label(String text) {
		return LabelBuilder.builder(text);
	}

	/**
	 * @return a JPanel builder
	 */
	public static PanelBuilder panel() {
		return PanelBuilder.builder();
	}

	/**
	 * @param layout the panel layout manager
	 * @return a JPanel builder
	 */
	public static PanelBuilder panel(LayoutManager layout) {
		return PanelBuilder.builder(layout);
	}

	/**
	 * @param panel the panel to configure
	 * @return a JPanel builder
	 */
	public static PanelBuilder panel(JPanel panel) {
		return PanelBuilder.builder(panel);
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
	 * @param view the view component
	 * @return a JScrollPane builder
	 */
	public static ScrollPaneBuilder scrollPane(JComponent view) {
		return ScrollPaneBuilder.builder(view);
	}

	/**
	 * @return an indeterminate JProgressBar builder
	 */
	public static ProgressBarBuilder progressBar() {
		return ProgressBarBuilder.builder();
	}

	/**
	 * @param boundedRangeModel the model
	 * @return a JProgressBar builder
	 */
	public static ProgressBarBuilder progressBar(BoundedRangeModel boundedRangeModel) {
		return ProgressBarBuilder.builder(boundedRangeModel);
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
	public static ToolBarBuilder toolBar() {
		return ToolBarBuilder.builder();
	}

	/**
	 * @param controls the Controls
	 * @return a {@link javax.swing.JToolBar} builder
	 */
	public static ToolBarBuilder toolBar(Controls controls) {
		return ToolBarBuilder.builder(controls);
	}

	/**
	 * @param controlsBuilder the {@link ControlsBuilder}
	 * @return a {@link javax.swing.JToolBar} builder
	 */
	public static ToolBarBuilder toolBar(ControlsBuilder controlsBuilder) {
		return ToolBarBuilder.builder(controlsBuilder);
	}

	/**
	 * @return a button panel builder
	 */
	public static ButtonPanelBuilder buttonPanel() {
		return ButtonPanelBuilder.builder();
	}

	/**
	 * @param actions the actions
	 * @return a button panel builder
	 */
	public static ButtonPanelBuilder buttonPanel(Action... actions) {
		return ButtonPanelBuilder.builder(actions);
	}

	/**
	 * @param controls the Controls
	 * @return a button panel builder
	 */
	public static ButtonPanelBuilder buttonPanel(Controls controls) {
		return ButtonPanelBuilder.builder(controls);
	}

	/**
	 * @param controlsBuilder the {@link ControlsBuilder}
	 * @return a button panel builder
	 */
	public static ButtonPanelBuilder buttonPanel(ControlsBuilder controlsBuilder) {
		return ButtonPanelBuilder.builder(controlsBuilder);
	}

	/**
	 * Creates a new {@link BorderLayoutPanelBuilder} instance using a new
	 * {@link BorderLayout} instance with the default horizontal and vertical gap.
	 * @return a border layout panel builder
	 * @see Layouts#GAP
	 */
	public static BorderLayoutPanelBuilder borderLayoutPanel() {
		return BorderLayoutPanelBuilder.builder();
	}

	/**
	 * @param layout the layout to use
	 * @return a new border layout panel builder
	 */
	public static BorderLayoutPanelBuilder borderLayoutPanel(BorderLayout layout) {
		return BorderLayoutPanelBuilder.builder(layout);
	}

	/**
	 * Creates a new {@link PanelBuilder} instance using a new {@link java.awt.GridLayout}
	 * with the default horizontal and vertical gap.
	 * @param rows the number of rows
	 * @param columns the number of columns
	 * @return a grid layout panel builder
	 * @see Layouts#GAP
	 */
	public static PanelBuilder gridLayoutPanel(int rows, int columns) {
		return panel(gridLayout(rows, columns));
	}

	/**
	 * Creates a new {@link PanelBuilder} instance using a new
	 * {@link is.codion.swing.common.ui.layout.FlexibleGridLayout} with the default horizontal and vertical gap.
	 * @param rows the number of rows
	 * @param columns the number of columns
	 * @return a flexible grid layout panel builder
	 * @see Layouts#GAP
	 */
	public static PanelBuilder flexibleGridLayoutPanel(int rows, int columns) {
		return panel(flexibleGridLayout(rows, columns));
	}

	/**
	 * Creates a new {@link PanelBuilder} instance using a new {@link java.awt.FlowLayout} with the default
	 * horizontal and vertical gap.
	 * @param alignment the flow layout alignment
	 * @return a flow layout panel builder
	 * @see Layouts#GAP
	 */
	public static PanelBuilder flowLayoutPanel(int alignment) {
		return panel(flowLayout(alignment));
	}

	/**
	 * @return a new menu builder
	 */
	public static MenuBuilder menu() {
		return MenuBuilder.builder();
	}

	/**
	 * @param controls the controls to base the menu on
	 * @return a new menu builder
	 */
	public static MenuBuilder menu(Controls controls) {
		return MenuBuilder.builder(controls);
	}

	/**
	 * @param controlsBuilder the {@link ControlsBuilder} to base the menu on
	 * @return a new menu builder
	 */
	public static MenuBuilder menu(ControlsBuilder controlsBuilder) {
		return MenuBuilder.builder(controlsBuilder);
	}
}
