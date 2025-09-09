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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.common.demo;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.plugin.flatlaf.intellij.themes.monokaipro.MonokaiPro;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ItemFinder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_SPACE;
import static javax.swing.BorderFactory.createTitledBorder;

/*
// tag::demoPanelImport[]
import static is.codion.swing.common.ui.component.Components.*;
// end::demoPanelImport[]
 */
// tag::demoPanel[]

public final class ApplicationPanel extends JPanel {

	public ApplicationPanel(ApplicationModel model) {
		super(borderLayout());

		setBorder(emptyBorder());

		JPanel settingsPanel = new JPanel(borderLayout());

		State inputEnabledState = State.state(true);

		checkBox()
						.link(inputEnabledState)
						.text("Enabled")
						.mnemonic('N')
						.transferFocusOnEnter(true)
						.build(checkBox -> settingsPanel.add(checkBox, BorderLayout.WEST));

		button()
						.control(Control.builder()
										.command(model::clear)
										.enabled(inputEnabledState)
										.caption("Clear")
										.mnemonic('L'))
						.transferFocusOnEnter(true)
						.build(button -> settingsPanel.add(button, BorderLayout.EAST));

		JPanel inputPanel = flexibleGridLayoutPanel(0, 2).build();

		stringField()
						.link(model.shortStringValue())
						.columns(20)
						.lowerCase(true)
						.maximumLength(20)
						.selectAllOnFocusGained(true)
						.transferFocusOnEnter(true)
						.validIndicator(new PGValidator())
						// CTRL-SPACE displays a dialog for selecting a value
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_SPACE)
										.modifiers(CTRL_DOWN_MASK)
										.action(Control.action(ApplicationPanel::selectString)))
						.label(label("Short String (1)")
										.displayedMnemonic('1')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		textFieldPanel()
						.link(model.longStringValue())
						.columns(20)
						.maximumLength(400)
						.buttonFocusable(true)
						.selectAllOnFocusGained(true)
						.transferFocusOnEnter(true)
						.label(label("Long String (2)")
										.displayedMnemonic('2')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		textArea()
						.link(model.textValue())
						.rowsColumns(4, 20)
						.lineWrap(true)
						.wrapStyleWord(true)
						.transferFocusOnEnter(true)
						.dragEnabled(true)
						.transferHandler(new FilePathTransferHandler())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_SPACE)
										.modifiers(CTRL_DOWN_MASK)
										.action(Control.action(actionEvent ->
														((JTextArea) actionEvent.getSource()).append("SPACE"))))
						.label(label("Text (3)")
										.displayedMnemonic('3')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.scrollPane()
						.build(inputPanel::add);

		maskedTextField()
						.link(model.formattedStringValue())
						.mask("(##) ##-##")
						.placeholderCharacter('_')
						.placeholder("(00) 00-00")
						.emptyStringToNullValue(true)
						.invalidStringToNullValue(true)
						.valueContainsLiteralCharacters(true)
						.commitsOnValidEdit(true)
						.focusLostBehaviour(JFormattedTextField.COMMIT)
						.transferFocusOnEnter(true)
						.label(label("Formatted String (4)")
										.displayedMnemonic('4')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		comboBox()
						.model(model.createStringComboBoxModel())
						.link(model.stringSelectionValue())
						.editable(true)
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.label(label("String Selection (5)")
										.displayedMnemonic('5')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		localDateField()
						.link(model.localDateValue())
						.dateTimePattern(LocaleDateTimePattern.builder()
										.delimiterDash()
										.yearFourDigits()
										.build()
										.dateTimePattern())
						.transferFocusOnEnter(true)
						.validIndicator(new LocalDateValidator())
						.label(label("Date (6)")
										.displayedMnemonic('6')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		localDateTimeFieldPanel()
						.link(model.localDateTimeValue())
						.dateTimePattern(LocaleDateTimePattern.builder()
										.delimiterDot()
										.yearTwoDigits()
										.hoursMinutes()
										.build()
										.dateTimePattern())
						.transferFocusOnEnter(true)
						.label(label("Date Time (7)")
										.displayedMnemonic('7')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		integerField()
						.link(model.integerValue())
						.range(0, 10_000)
						.groupingUsed(true)
						.groupingSeparator('.')
						.transferFocusOnEnter(true)
						.label(label("Integer (8)")
										.displayedMnemonic('8')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		doubleField()
						.link(model.doubleValue())
						.nullable(false)
						.range(0, 1_000_000)
						.groupingUsed(true)
						.maximumFractionDigits(2)
						.decimalSeparator(',')
						.groupingSeparator('.')
						.convertGroupingToDecimalSeparator(true)
						.transferFocusOnEnter(true)
						.label(label("Double (9)")
										.displayedMnemonic('9')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		FilterComboBoxModel<Item<Integer>> integerItemComboBoxModel = model.createIntegerItemComboBoxModel();
		Value<Integer> integerItemSelector = integerItemComboBoxModel.createSelector(new IntegerItemFinder());
		NumberField<Integer> integerItemSelectorField = integerField()
						.link(integerItemSelector)
						.columns(2)
						.horizontalAlignment(SwingConstants.CENTER)
						.selectAllOnFocusGained(true)
						.transferFocusOnEnter(true)
						.label(label("Integer Item (A)")
										.displayedMnemonic('A')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build();
		JComboBox<Item<Integer>> integerItemComboBox = itemComboBox()
						.model(integerItemComboBoxModel)
						.link(model.integerItemValue())
						.completionMode(Completion.Mode.AUTOCOMPLETE)
						.popupMenuControl(comboBox -> createSelectRandomItemControl(integerItemComboBoxModel))
						.mouseWheelScrollingWithWrapAround(true)
						.transferFocusOnEnter(true)
						.enabled(inputEnabledState)
						.build();
		borderLayoutPanel()
						.west(integerItemSelectorField)
						.center(integerItemComboBox)
						.build(inputPanel::add);

		slider()
						.model(model.createIntegerSliderModel())
						.link(model.integerSlideValue())
						.paintTicks(true)
						.paintTrack(true)
						.minorTickSpacing(5)
						.majorTickSpacing(20)
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.label(label("Integer Slide (B)")
										.displayedMnemonic('B')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		integerSpinner()
						.model(model.createIntegerSpinnerModel())
						.link(model.integerSpinValue())
						.columns(4)
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.label(label("Integer Spin (C)")
										.displayedMnemonic('C')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		comboBox()
						.model(model.createIntegerComboBoxModel())
						.link(model.integerSelectionValue())
						.editable(true)
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.label(label("Integer Selection (D)")
										.displayedMnemonic('D')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		Components.<String>itemSpinner()
						.model(model.createItemSpinnerModel())
						.link(model.itemSpinnerValue())
						.columns(20)
						.horizontalAlignment(SwingConstants.CENTER)
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.label(label("Item Spin (E)")
										.displayedMnemonic('E')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		nullableCheckBox()
						.link(model.booleanValue())
						.horizontalAlignment(SwingConstants.CENTER)
						.transferFocusOnEnter(true)
						.label(label("Boolean (F)")
										.displayedMnemonic('F')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		booleanComboBox()
						.link(model.booleanSelectionValue())
						.mouseWheelScrolling(true)
						.transferFocusOnEnter(true)
						.enabled(inputEnabledState)
						.label(label("Boolean Selection (G)")
										.displayedMnemonic('G')
										.build(inputPanel::add))
						.build(inputPanel::add);

		Components.list()
						.model(model.createStringListModel())
						.selectedItems()
						.link(model.stringListValues())
						.visibleRowCount(4)
						.layoutOrientation(JList.HORIZONTAL_WRAP)
						.transferFocusOnEnter(true)
						.label(label("Text List Selection (H)")
										.displayedMnemonic('H')
										.build(inputPanel::add))
						.enabled(inputEnabledState)
						.build(inputPanel::add);

		add(settingsPanel, BorderLayout.NORTH);
		add(inputPanel, BorderLayout.CENTER);

		flexibleGridLayoutPanel(2, 1)
						.add(stringField()
										.columns(20)
										.editable(false)
										.focusable(false)
										.border(createTitledBorder("Message"))
										.enabled(inputEnabledState)
										.link(model.message())
										.build(component -> add(component, BorderLayout.SOUTH)))
						.add(LookAndFeelComboBox.builder().build())
						.build(southPanel -> add(southPanel, BorderLayout.SOUTH));
	}

	private static void selectString(ActionEvent event) {
		JTextField stringField = (JTextField) event.getSource();

		Dialogs.select()
						.list(List.of("a", "few", "short", "strings", "to", "choose", "from"))
						.owner(stringField)
						.defaultSelection("strings")
						.select()
						.single()
						.ifPresent(stringField::setText);
	}

	private static class PGValidator implements Predicate<String> {

		private static final List<String> SWEAR_WORDS = List.of("fuck", "shit");

		@Override
		public boolean test(String value) {
			if (value != null) {
				String lowerCaseValue = value.toLowerCase();
				for (String swearWord : SWEAR_WORDS) {
					if (lowerCaseValue.contains(swearWord)) {
						return false;
					}
				}
			}

			return true;
		}
	}

	private static class FilePathTransferHandler extends TransferHandler {

		@Override
		public boolean canImport(TransferSupport support) {
			return Arrays.stream(support.getDataFlavors())
							.anyMatch(DataFlavor::isFlavorJavaFileListType);
		}

		@Override
		public boolean importData(TransferSupport support) {
			try {
				((JTextArea) support.getComponent()).setText(support.getTransferable()
								.getTransferData(DataFlavor.javaFileListFlavor).toString());

				return true;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final class LocalDateValidator implements Predicate<LocalDate> {

		@Override
		public boolean test(LocalDate localDate) {
			return localDate != null && !localDate.isBefore(LocalDate.now());
		}
	}

	private static final class IntegerItemFinder implements ItemFinder<Item<Integer>, Integer> {

		@Override
		public Integer value(Item<Integer> item) {
			return item.getOrThrow();
		}

		@Override
		public Predicate<Item<Integer>> predicate(Integer value) {
			return item -> Objects.equals(item.get(), value);
		}
	}

	private static Control createSelectRandomItemControl(FilterComboBoxModel<Item<Integer>> integerItemComboBoxModel) {
		Random random = new Random();
		return Control.builder()
						.command(() -> integerItemComboBoxModel.setSelectedItem(
										random.nextInt(integerItemComboBoxModel.getSize()) + 1))
						.caption("Select Random Item")
						.build();
	}

	public static void main(String[] args) {
		findLookAndFeel(MonokaiPro.class)
						.ifPresent(LookAndFeelEnabler::enable);

		ValidIndicatorFactory.FACTORY_CLASS.set("is.codion.plugin.flatlaf.indicator.FlatLafValidIndicatorFactory");

		ApplicationModel applicationModel = new ApplicationModel();

		ApplicationPanel applicationPanel = new ApplicationPanel(applicationModel);

		Dialogs.builder()
						.component(applicationPanel)
						.title("Codion Input Components Demo")
						.icon(Logos.logoTransparent())
						.show();
	}
}
// end::demoPanel[]
