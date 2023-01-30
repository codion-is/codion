package is.codion.framework.demos.manual.common.demo;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel.ItemFinder;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Arrays.asList;

/*
// tag::demoPanelImport[]
import static is.codion.swing.common.ui.component.Components.*;
// end::demoPanelImport[]
 */
// tag::demoPanel[]

public final class ApplicationPanel extends JPanel {

  public ApplicationPanel(ApplicationModel model) {
    super(borderLayout());

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel settingsPanel = new JPanel(borderLayout());

    State inputEnabledState = State.state(true);

    checkBox(inputEnabledState)
            .caption("Enabled")
            .mnemonic('N')
            .transferFocusOnEnter(true)
            .build(checkBox -> settingsPanel.add(checkBox, BorderLayout.WEST));

    button(Control.builder(model::clear)
            .enabledState(inputEnabledState)
            .build())
            .caption("Clear")
            .mnemonic('L')
            .transferFocusOnEnter(true)
            .build(button -> settingsPanel.add(button, BorderLayout.EAST));

    JPanel inputPanel = new JPanel(Layouts.flexibleGridLayout(0, 2));

    textField(model.shortStringValue())
            .columns(20)
            .lowerCase(true)
            .maximumLength(20)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .validator(new PGValidator())
            .selectionProvider(Dialogs.selectionProvider(() ->
                    Arrays.asList("a", "few", "short", "strings", "to", "choose", "from")))
            .label(label("Short String (1)")
                    .displayedMnemonic('1')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    textInputPanel(model.longStringValue())
            .columns(20)
            .maximumLength(400)
            .buttonFocusable(true)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .label(label("Long String (2)")
                    .displayedMnemonic('2')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    textArea(model.textValue())
            .rowsColumns(4, 20)
            .lineWrap(true)
            .wrapStyleWord(true)
            .transferFocusOnEnter(true)
            .dragEnabled(true)
            .transferHandler(new FilePathTransferHandler())
            .keyEvent(KeyEvents.builder(KeyEvent.VK_SPACE)
                    .modifiers(KeyEvent.CTRL_DOWN_MASK)
                    .action(Control.actionControl(actionEvent ->
                            ((JTextArea) actionEvent.getSource()).append("SPACE"))))
            .label(label("Text (3)")
                    .displayedMnemonic('3')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .scrollPane()
            .build(inputPanel::add);

    maskedTextField(model.formattedStringValue())
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
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    comboBox(model.createStringComboBoxModel(), model.stringSelectionValue())
            .editable(true)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .label(label("String Selection (5)")
                    .displayedMnemonic('5')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    localDateField(LocaleDateTimePattern.builder()
            .delimiterDash()
            .yearFourDigits()
            .build()
            .dateTimePattern(), model.localDateValue())
            .transferFocusOnEnter(true)
            .label(label("Date (6)")
                    .displayedMnemonic('6')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    localDateTimeInputPanel(LocaleDateTimePattern.builder()
            .delimiterDot()
            .yearTwoDigits()
            .hoursMinutes()
            .build()
            .dateTimePattern(), model.localDateTimeValue())
            .transferFocusOnEnter(true)
            .label(label("Date Time (7)")
                    .displayedMnemonic('7')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    integerField(model.integerValue())
            .valueRange(0, 10_000)
            .groupingUsed(true)
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .label(label("Integer (8)")
                    .displayedMnemonic('8')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    doubleField(model.doubleValue())
            .nullable(false)
            .valueRange(0, 1_000_000)
            .groupingUsed(true)
            .maximumFractionDigits(2)
            .decimalSeparator(',')
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .label(label("Double (9)")
                    .displayedMnemonic('9')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    ItemComboBoxModel<Integer> integerItemComboBoxModel = model.createIntegerItemComboBoxModel();
    Value<Integer> integerItemSelectorValue = integerItemComboBoxModel.createSelectorValue(new IntegerItemFinder());
    NumberField<Integer> integerItemSelectorField = integerField(integerItemSelectorValue)
            .columns(2)
            .horizontalAlignment(SwingConstants.CENTER)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .label(label("Integer Item (A)")
                    .displayedMnemonic('A')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build();
    JComboBox<Item<Integer>> integerItemComboBox = itemComboBox(integerItemComboBoxModel, model.integerItemValue())
            .completionMode(Completion.Mode.AUTOCOMPLETE)
            .popupMenuControl(createSelectRandomItemControl(integerItemComboBoxModel))
            .mouseWheelScrollingWithWrapAround(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build();
    panel(borderLayout())
            .add(integerItemSelectorField, BorderLayout.WEST)
            .add(integerItemComboBox, BorderLayout.CENTER)
            .build(inputPanel::add);

    slider(model.createIntegerSliderModel(), model.integerSlideValue())
            .paintTicks(true)
            .paintTrack(true)
            .minorTickSpacing(5)
            .majorTickSpacing(20)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .label(label("Integer Slide (B)")
                    .displayedMnemonic('B')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    integerSpinner(model.createIntegerSpinnerModel(), model.integerSpinValue())
            .columns(4)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .label(label("Integer Spin (C)")
                    .displayedMnemonic('C')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    comboBox(model.createIntegerComboBoxModel(), model.integerSelectionValue())
            .editable(true)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .label(label("Integer Selection (D)")
                    .displayedMnemonic('D')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    itemSpinner(model.createItemSpinnerModel(), model.itemSpinnerValue())
            .columns(20)
            .horizontalAlignment(SwingConstants.CENTER)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .label(label("Item Spin (E)")
                    .displayedMnemonic('E')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    checkBox(model.booleanValue())
            .horizontalAlignment(SwingConstants.CENTER)
            .transferFocusOnEnter(true)
            .label(label("Boolean (F)")
                    .displayedMnemonic('F')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    booleanComboBox(model.booleanSelectionValue())
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .label(label("Boolean Selection (G)")
                    .displayedMnemonic('G')
                    .build(inputPanel::add))
            .build(inputPanel::add);

    Components.list(model.createStringListModel(), model.stringListValue())
            .visibleRowCount(4)
            .selectionMode(ListSelectionModel.SINGLE_SELECTION)
            .layoutOrientation(JList.HORIZONTAL_WRAP)
            .transferFocusOnEnter(true)
            .label(label("Text List Selection (H)")
                    .displayedMnemonic('H')
                    .build(inputPanel::add))
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    add(settingsPanel, BorderLayout.NORTH);
    add(inputPanel, BorderLayout.CENTER);

    textField()
            .columns(20)
            .editable(false)
            .focusable(false)
            .border(BorderFactory.createTitledBorder("Message"))
            .enabledState(inputEnabledState)
            .linkedValueObserver(model.messageObserver())
            .build(component -> add(component, BorderLayout.SOUTH));

    Sizes.setPreferredWidth(this, 400);
  }

  private static class PGValidator implements Value.Validator<String> {

    private final List<String> swearWords = asList("fuck", "shit");

    @Override
    public void validate(String value) throws IllegalArgumentException {
      if (value != null) {
        String lowerCaseValue = value.toLowerCase();
        swearWords.forEach(swearWord -> {
          if (lowerCaseValue.contains(swearWord)) {
            throw new IllegalArgumentException("No swearing please");
          }
        });
      }
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

  private static final class IntegerItemFinder implements ItemFinder<Item<Integer>, Integer> {

    @Override
    public Integer value(Item<Integer> item) {
      return item.value();
    }

    @Override
    public Predicate<Item<Integer>> createPredicate(Integer value) {
      return item -> Objects.equals(item.value(), value);
    }
  }

  private static Control createSelectRandomItemControl(ItemComboBoxModel<Integer> integerItemComboBoxModel) {
    Random random = new Random();
    return Control.builder(() ->
                    integerItemComboBoxModel.setSelectedItem(random.nextInt(integerItemComboBoxModel.getSize()) + 1))
            .caption("Select Random Item")
            .build();
  }

  public static void main(String[] args) {
    lookAndFeelProvider("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProContrastIJTheme").enable();

    ApplicationModel applicationModel = new ApplicationModel();

    ApplicationPanel applicationPanel = new ApplicationPanel(applicationModel);

    Dialogs.componentDialog(applicationPanel)
            .title("Codion Input Components Demo")
            .icon(Logos.logoTransparent())
            .show();
  }
}
// end::demoPanel[]
