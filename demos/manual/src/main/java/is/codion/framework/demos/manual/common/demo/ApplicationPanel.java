package is.codion.framework.demos.manual.common.demo;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static is.codion.swing.common.ui.component.ComponentBuilders.*;
import static java.util.Arrays.asList;

/*
// tag::demoPanelImport[]
import static is.codion.swing.common.ui.component.ComponentBuilders.*;
// end::demoPanelImport[]
 */
// tag::demoPanel[]

public final class ApplicationPanel extends JPanel {

  public ApplicationPanel(ApplicationModel model) {
    super(Layouts.borderLayout());

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel inputPanel = new JPanel(Layouts.flexibleGridLayout(0, 2));

    label("Short String")
            .build(inputPanel::add);
    textField()
            .columns(20)
            .upperCase(true)
            .maximumLength(20)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .validator(new PGValidator())
            .linkedValue(model.getShortStringValue())
            .build(inputPanel::add);

    label("Long String")
            .build(inputPanel::add);
    textInputPanel()
            .columns(20)
            .maximumLength(400)
            .buttonFocusable(true)
            .selectAllOnFocusGained(true)
            .transferFocusOnEnter(true)
            .linkedValue(model.getLongStringValue())
            .build(inputPanel::add);

    label("Text")
            .build(inputPanel::add);
    textArea()
            .rowsColumns(4, 20)
            .lineWrap(true)
            .wrapStyleWord(true)
            .transferFocusOnEnter(true)
            .linkedValue(model.getTextValue())
            .buildScrollPane(inputPanel::add);

    label("Formatted String")
            .build(inputPanel::add);
    formattedTextField()
            .formatMask("(##) ##-##")
            .valueContainsLiterals(true)
            .focusLostBehaviour(JFormattedTextField.COMMIT)
            .transferFocusOnEnter(true)
            .linkedValue(model.getFormattedStringValue())
            .build(inputPanel::add);

    label("String Selection")
            .build(inputPanel::add);
    comboBox(String.class, createStringComboBoxModel())
            .editable(true)
            .transferFocusOnEnter(true)
            .linkedValue(model.getStringSelectionValue())
            .build(inputPanel::add);

    label("Date")
            .build(inputPanel::add);
    localDateField(LocaleDateTimePattern.builder()
            .delimiterDash()
            .yearFourDigits()
            .build()
            .getDateTimePattern())
            .transferFocusOnEnter(true)
            .linkedValue(model.getLocalDateValue())
            .build(inputPanel::add);

    label("Date Time")
            .build(inputPanel::add);
    localDateTimeInputPanel(LocaleDateTimePattern.builder()
            .delimiterDot()
            .yearTwoDigits()
            .hoursMinutes()
            .build()
            .getDateTimePattern())
            .transferFocusOnEnter(true)
            .linkedValue(model.getLocalDateTimeValue())
            .build(inputPanel::add);

    label("Integer")
            .build(inputPanel::add);
    integerField()
            .range(0, 10_000)
            .groupingUsed(true)
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .linkedValue(model.getIntegerValue())
            .build(inputPanel::add);

    label("Double")
            .build(inputPanel::add);
    doubleField()
            .range(0, 1_000_000)
            .groupingUsed(true)
            .maximumFractionDigits(2)
            .decimalSeparator(',')
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .linkedValue(model.getDoubleValue())
            .build(inputPanel::add);

    label("Integer Item")
            .build(inputPanel::add);
    ItemComboBoxModel<Integer> integerItemComboBoxModel = createIntegerItemComboBoxModel();
    itemComboBox(integerItemComboBoxModel)
            .completionMode(Completion.Mode.AUTOCOMPLETE)
            .popupMenuControl(createSelectRandomItemControl(integerItemComboBoxModel))
            .transferFocusOnEnter(true)
            .linkedValue(model.getIntegerItemValue())
            .build(inputPanel::add);

    label("Integer Slide")
            .build(inputPanel::add);
    slider(createSliderModel())
            .paintTicks(true)
            .paintTrack(true)
            .transferFocusOnEnter(true)
            .linkedValue(model.getIntegerSlideValue())
            .build(inputPanel::add);

    label("Integer Spin")
            .build(inputPanel::add);
    integerSpinner(createSpinnerModel())
            .columns(4)
            .transferFocusOnEnter(true)
            .mouseWheelScrolling(true)
            .linkedValue(model.getIntegerSpinValue())
            .build(inputPanel::add);

    label("Item Spin")
            .build(inputPanel::add);
    ComponentBuilders.<String>itemSpinner(createItemSpinnerModel())
            .columns(20)
            .horizontalAlignment(SwingConstants.CENTER)
            .transferFocusOnEnter(true)
            .mouseWheelScrolling(true)
            .linkedValue(model.getItemSpinnerValue())
            .build(inputPanel::add);

    label("Boolean")
            .build(inputPanel::add);
    checkBox()
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValue(model.getBooleanValue())
            .transferFocusOnEnter(true)
            .linkedValue(model.getBooleanValue())
            .build(inputPanel::add);

    label("Boolean Selection")
            .build(inputPanel::add);
    booleanComboBox()
            .transferFocusOnEnter(true)
            .linkedValue(model.getBooleanSelectionValue())
            .build(inputPanel::add);

    label("Text List Selection")
            .build(inputPanel::add);
    ComponentBuilders.list(createStringListModel())
            .visibleRowCount(4)
            .layoutOrientation(JList.HORIZONTAL_WRAP)
            .transferFocusOnEnter(true)
            .linkedValue(model.getStringListValue())
            .buildScrollPane(inputPanel::add);

    add(inputPanel, BorderLayout.CENTER);

    textField()
            .columns(20)
            .editable(false)
            .focusable(false)
            .border(BorderFactory.createTitledBorder("Message"))
            .linkedValueObserver(model.getMessageObserver())
            .build(component -> add(component, BorderLayout.SOUTH));

    Components.setPreferredWidth(this, 380);
  }

  private static class PGValidator implements Value.Validator<String> {

    private final List<String> swearWords = asList("fuck", "shit");

    @Override
    public void validate(final String value) throws IllegalArgumentException {
      if (value != null) {
        final String lowerCaseValue = value.toLowerCase();
        swearWords.forEach(swearWord -> {
          if (lowerCaseValue.contains(swearWord)) {
            throw new IllegalArgumentException("No swearing please");
          }
        });
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
    final DefaultListModel<String> listModel = new DefaultListModel<>();
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

  private static Control createSelectRandomItemControl(final ItemComboBoxModel<Integer> integerItemComboBoxModel) {
    Random random = new Random();
    return Control.builder(() ->
                    integerItemComboBoxModel.setSelectedItem(random.nextInt(integerItemComboBoxModel.getSize()) + 1))
            .caption("Select Random Item")
            .build();
  }

  public static void main(String[] args) {
    ApplicationModel applicationModel = new ApplicationModel();

    ApplicationPanel applicationPanel = new ApplicationPanel(applicationModel);

    Dialogs.componentDialogBuilder(applicationPanel)
            .title("Codion Input Components Demo")
            .icon(Icons.icons().logoTransparent())
            .show();
  }
}
// end::demoPanel[]
