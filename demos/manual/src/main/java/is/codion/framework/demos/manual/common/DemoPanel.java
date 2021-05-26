package is.codion.framework.demos.manual.common;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.util.Arrays;

public final class DemoPanel extends JPanel {
  // tag::inputDemo[]
  private static class DemoModel {

    private final Value<String> upperCaseStringValue = Value.value();
    private final Value<String> lowerCaseStringValue = Value.value();
    private final Value<LocalDateTime> localDateTimeValue = Value.value();
    private final Value<String> formattedStringValue = Value.value();
    private final Value<Integer> integerValue = Value.value();
    private final Value<Double> doubleValue = Value.value();
    private final Value<Boolean> booleanValue = Value.value();
    private final Value<Boolean> booleanSelectionValue = Value.value();
    private final Value<Integer> integerItemValue = Value.value();
    private final Value<String> stringSelectionValue = Value.value();
    private final Value<String> messageValue = Value.value();

    public DemoModel() {
      bindEvents();
    }

    public Value<String> getUpperCaseStringValue() {
      return upperCaseStringValue;
    }

    public Value<String> getLowerCaseStringValue() {
      return lowerCaseStringValue;
    }

    public Value<LocalDateTime> getLocalDateTimeValue() {
      return localDateTimeValue;
    }

    public Value<Integer> getIntegerValue() {
      return integerValue;
    }

    public Value<Double> getDoubleValue() {
      return doubleValue;
    }

    public Value<String> getFormattedStringValue() {
      return formattedStringValue;
    }

    public Value<Boolean> getBooleanValue() {
      return booleanValue;
    }

    public Value<Boolean> getBooleanSelectionValue() {
      return booleanSelectionValue;
    }

    public Value<Integer> getIntegerItemValue() {
      return integerItemValue;
    }

    public Value<String> getStringSelectionValue() {
      return stringSelectionValue;
    }

    public ValueObserver<String> getMessageValue() {
      return messageValue.getObserver();
    }

    private void bindEvents() {
      upperCaseStringValue.addDataListener(this::setMessage);
      lowerCaseStringValue.addDataListener(this::setMessage);
      formattedStringValue.addDataListener(this::setMessage);
      localDateTimeValue.addDataListener(this::setMessage);
      integerValue.addDataListener(this::setMessage);
      doubleValue.addDataListener(this::setMessage);
      booleanValue.addDataListener(this::setMessage);
      booleanValue.addDataListener(this::setMessage);
      integerItemValue.addDataListener(this::setMessage);
      stringSelectionValue.addValidator(this::setMessage);
      stringSelectionValue.addDataListener(this::setMessage);
    }

    private <T> void setMessage(T value) {
      messageValue.set("Last Value:" + (value == null ? " " : value.toString()));
    }
  }

  public DemoPanel(DemoModel model) {
    super(Layouts.borderLayout());

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel inputPanel = new JPanel(Layouts.flexibleGridLayout(11, 2));

    inputPanel.add(new JLabel("Upper Case String"));
    inputPanel.add(ComponentBuilders.textField()
            .columns(12)
            .upperCase()
            .maximumLength(20)
            .selectAllOnFocusGained()
            .transferFocusOnEnter(true)
            .linkedValue(model.getUpperCaseStringValue())
            .build());

    inputPanel.add(new JLabel("Lower Case String"));
    inputPanel.add(ComponentBuilders.textField()
            .columns(12)
            .lowerCase()
            .maximumLength(10)
            .selectAllOnFocusGained()
            .transferFocusOnEnter(true)
            .linkedValue(model.getLowerCaseStringValue())
            .build());

    inputPanel.add(new JLabel("Local Date Time"));
    inputPanel.add(ComponentBuilders.localDateTimeField("dd-MM-yyyy HH:mm")
            .transferFocusOnEnter(true)
            .linkedValue(model.getLocalDateTimeValue())
            .build());

    inputPanel.add(new JLabel("Formatted String"));
    inputPanel.add(ComponentBuilders.formattedTextField()
            .formatMask("##:##")
            .valueContainsLiterals(true)
            .focusLostBehaviour(JFormattedTextField.COMMIT)
            .transferFocusOnEnter(true)
            .linkedValue(model.getFormattedStringValue())
            .build());

    inputPanel.add(new JLabel("Integer"));
    inputPanel.add(ComponentBuilders.integerField()
            .groupingUsed(true)
            .range(0, 1_000_000)
            .groupingSeparator('.')
            .preferredWidth(60)
            .transferFocusOnEnter(true)
            .linkedValue(model.getIntegerValue())
            .build());

    inputPanel.add(new JLabel("Double"));
    inputPanel.add(ComponentBuilders.doubleField()
            .groupingUsed(true)
            .maximumFractionDigits(2)
            .range(0, 1_000_000_000)
            .decimalSeparator(',')
            .groupingSeparator('.')
            .preferredWidth(80)
            .transferFocusOnEnter(true)
            .linkedValue(model.getDoubleValue())
            .build());

    inputPanel.add(new JLabel("Boolean"));
    inputPanel.add(ComponentBuilders.checkBox()
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValue(model.getBooleanValue())
            .transferFocusOnEnter(true)
            .linkedValue(model.getBooleanValue())
            .build());

    inputPanel.add(new JLabel("Boolean Selection"));
    inputPanel.add(ComponentBuilders.booleanComboBox()
            .transferFocusOnEnter(true)
            .linkedValue(model.getBooleanSelectionValue())
            .build());

    ItemComboBoxModel<Integer> itemComboBoxModel = ItemComboBoxModel.createModel(Arrays.asList(
            Item.item(1, "One"), Item.item(2, "Two"), Item.item(3, "Three"),
            Item.item(4, "Four"), Item.item(5, "Five"), Item.item(6, "Six"),
            Item.item(7, "Seven"), Item.item(8, "Eight"), Item.item(9, "Nine")
    ));

    inputPanel.add(new JLabel("Integer Item"));
    inputPanel.add(ComponentBuilders.itemComboBox(itemComboBoxModel)
            .completionMode(Completion.Mode.AUTOCOMPLETE)
            .transferFocusOnEnter(true)
            .linkedValue(model.getIntegerItemValue())
            .build());

    DefaultComboBoxModel<String> comboBoxModel =
            new DefaultComboBoxModel<>(new String[] {"Hello", "Everybody", "How", "Are", "You"});

    inputPanel.add(new JLabel("String Selection"));
    inputPanel.add(ComponentBuilders.comboBox(String.class, comboBoxModel)
            .editable(true)
            .transferFocusOnEnter(true)
            .linkedValue(model.getStringSelectionValue())
            .build());

    add(inputPanel, BorderLayout.CENTER);
    add(ComponentBuilders.label()
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValueObserver(model.getMessageValue())
            .build(), BorderLayout.NORTH);
  }

  public static void main(String[] args) {
    DemoModel model = new DemoModel();

    DemoPanel panel = new DemoPanel(model);

    Dialogs.componentDialogBuilder(panel).show();
  }
  // end::inputDemo[]
}
