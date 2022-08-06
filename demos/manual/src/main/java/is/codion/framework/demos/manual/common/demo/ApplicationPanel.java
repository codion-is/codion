package is.codion.framework.demos.manual.common.demo;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel.Finder;
import is.codion.common.state.State;
import is.codion.common.value.Value;
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
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
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
            .mnemonic('E')
            .transferFocusOnEnter(true)
            .build(checkBox -> settingsPanel.add(checkBox, BorderLayout.WEST));

    button(Control.builder(model::clear)
            .enabledState(inputEnabledState)
            .build())
            .caption("Clear")
            .mnemonic('C')
            .transferFocusOnEnter(true)
            .build(button -> settingsPanel.add(button, BorderLayout.EAST));

    JPanel inputPanel = new JPanel(Layouts.flexibleGridLayout(0, 2));

    label("Short String")
            .build(inputPanel::add);
    textField(model.shortStringValue())
            .columns(20)
            .upperCase(true)
            .maximumLength(20)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .validator(new PGValidator())
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Long String")
            .build(inputPanel::add);
    textInputPanel(model.longStringValue())
            .columns(20)
            .maximumLength(400)
            .buttonFocusable(true)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Text")
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
            .enabledState(inputEnabledState)
            .scrollPane()
            .build(inputPanel::add);

    label("Formatted String")
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
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("String Selection")
            .build(inputPanel::add);
    comboBox(createStringComboBoxModel(), model.stringSelectionValue())
            .editable(true)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Date")
            .build(inputPanel::add);
    localDateField(LocaleDateTimePattern.builder()
            .delimiterDash()
            .yearFourDigits()
            .build()
            .dateTimePattern(), model.localDateValue())
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Date Time")
            .build(inputPanel::add);
    localDateTimeInputPanel(LocaleDateTimePattern.builder()
            .delimiterDot()
            .yearTwoDigits()
            .hoursMinutes()
            .build()
            .dateTimePattern(), model.localDateTimeValue())
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Integer")
            .build(inputPanel::add);
    integerField(model.integerValue())
            .valueRange(0, 10_000)
            .groupingUsed(true)
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Double")
            .build(inputPanel::add);
    doubleField(model.doubleValue())
            .valueRange(0, 1_000_000)
            .groupingUsed(true)
            .maximumFractionDigits(2)
            .decimalSeparator(',')
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Integer Item")
            .build(inputPanel::add);
    ItemComboBoxModel<Integer> integerItemComboBoxModel = createIntegerItemComboBoxModel();
    Value<Integer> integerItemSelectorValue = integerItemComboBoxModel.createSelectorValue(new IntegerItemFinder());
    NumberField<Integer> integerItemSelectorField = integerField(integerItemSelectorValue)
            .columns(2)
            .horizontalAlignment(SwingConstants.CENTER)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
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

    label("Integer Slide")
            .build(inputPanel::add);
    slider(createSliderModel(), model.integerSlideValue())
            .paintTicks(true)
            .paintTrack(true)
            .minorTickSpacing(5)
            .majorTickSpacing(20)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Integer Spin")
            .build(inputPanel::add);
    integerSpinner(createSpinnerModel(), model.integerSpinValue())
            .columns(4)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Integer Selection")
            .build(inputPanel::add);
    comboBox(createIntegerComboBoxModel(), model.integerSelectionValue())
            .editable(true)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Item Spin")
            .build(inputPanel::add);
    itemSpinner(createItemSpinnerModel(), model.itemSpinnerValue())
            .columns(20)
            .horizontalAlignment(SwingConstants.CENTER)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Boolean")
            .build(inputPanel::add);
    checkBox(model.booleanValue())
            .horizontalAlignment(SwingConstants.CENTER)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Boolean Selection")
            .build(inputPanel::add);
    booleanComboBox(model.booleanSelectionValue())
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .enabledState(inputEnabledState)
            .build(inputPanel::add);

    label("Text List Selection")
            .build(inputPanel::add);
    Components.list(createStringListModel(), model.stringListValue())
            .visibleRowCount(4)
            .selectionMode(ListSelectionModel.SINGLE_SELECTION)
            .layoutOrientation(JList.HORIZONTAL_WRAP)
            .transferFocusOnEnter(true)
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

    Sizes.setPreferredWidth(this, 380);
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

  private static SpinnerNumberModel createSpinnerModel() {
    return new SpinnerNumberModel(0, 0, 100, 10);
  }

  private static SpinnerListModel createItemSpinnerModel() {
    return new SpinnerListModel(Arrays.asList(
            Item.item("Hello"),
            Item.item("Everybody"),
            Item.item("How"),
            Item.item("Are"),
            Item.item("You")
    ));
  }

  private static DefaultBoundedRangeModel createSliderModel() {
    return new DefaultBoundedRangeModel(0, 0, 0, 100);
  }

  private static ComboBoxModel<String> createStringComboBoxModel() {
    return new DefaultComboBoxModel<>(new String[] {"Hello", "Everybody", "How", "Are", "You"});
  }

  private static ListModel<String> createStringListModel() {
    DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("Here");
    listModel.addElement("Are");
    listModel.addElement("A");
    listModel.addElement("Few");
    listModel.addElement("Elements");
    listModel.addElement("To");
    listModel.addElement("Select");
    listModel.addElement("From");

    return listModel;
  }

  private static ItemComboBoxModel<Integer> createIntegerItemComboBoxModel() {
    return ItemComboBoxModel.createModel(asList(
            Item.item(1, "One"), Item.item(2, "Two"), Item.item(3, "Three"),
            Item.item(4, "Four"), Item.item(5, "Five"), Item.item(6, "Six"),
            Item.item(7, "Seven"), Item.item(8, "Eight"), Item.item(9, "Nine")
    ));
  }

  private static final class IntegerItemFinder implements Finder<Item<Integer>, Integer> {

    @Override
    public Integer getValue(Item<Integer> item) {
      return item.value();
    }

    @Override
    public Predicate<Item<Integer>> createPredicate(Integer value) {
      return item -> Objects.equals(item.value(), value);
    }
  }

  private static ComboBoxModel<Integer> createIntegerComboBoxModel() {
    return new DefaultComboBoxModel<>(new Integer[] {101, 202, 303, 404});
  }

  private static Control createSelectRandomItemControl(ItemComboBoxModel<Integer> integerItemComboBoxModel) {
    Random random = new Random();
    return Control.builder(() ->
                    integerItemComboBoxModel.setSelectedItem(random.nextInt(integerItemComboBoxModel.getSize()) + 1))
            .caption("Select Random Item")
            .build();
  }

  public static void main(String[] args) {
    ApplicationModel applicationModel = new ApplicationModel();

    ApplicationPanel applicationPanel = new ApplicationPanel(applicationModel);

    Dialogs.componentDialog(applicationPanel)
            .title("Codion Input Components Demo")
            .icon(Logos.logoTransparent())
            .show();
  }
}
// end::demoPanel[]
