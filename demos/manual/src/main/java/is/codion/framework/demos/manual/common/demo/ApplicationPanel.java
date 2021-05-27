package is.codion.framework.demos.manual.common.demo;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icons.Icons;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Arrays;

// tag::demoPanel[]
public final class ApplicationPanel extends JPanel {

  public ApplicationPanel(ApplicationModel model) {
    super(Layouts.borderLayout());

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    JPanel inputPanel = new JPanel(Layouts.flexibleGridLayout(11, 2));

    inputPanel.add(new JLabel("Short String"));
    inputPanel.add(ComponentBuilders.textField()
            .columns(20)
            .upperCase()
            .maximumLength(20)
            .selectAllOnFocusGained()
            .transferFocusOnEnter(true)
            .linkedValue(model.getShortStringValue())
            .build());

    inputPanel.add(new JLabel("Long String"));
    inputPanel.add(ComponentBuilders.textInputPanel()
            .columns(20)
            .maximumLength(400)
            .buttonFocusable(true)
            .selectAllOnFocusGained()
            .transferFocusOnEnter(true)
            .linkedValue(model.getLongStringValue())
            .build());

    inputPanel.add(new JLabel("Date Time"));
    inputPanel.add(ComponentBuilders.localDateTimeField("dd-MM-yyyy HH:mm")
            .transferFocusOnEnter(true)
            .linkedValue(model.getLocalDateTimeValue())
            .build());

    inputPanel.add(new JLabel("Formatted String"));
    inputPanel.add(ComponentBuilders.formattedTextField()
            .formatMask("(##) ##-##")
            .valueContainsLiterals(true)
            .focusLostBehaviour(JFormattedTextField.COMMIT)
            .transferFocusOnEnter(true)
            .linkedValue(model.getFormattedStringValue())
            .build());

    inputPanel.add(new JLabel("Integer"));
    inputPanel.add(ComponentBuilders.integerField()
            .range(0, 1_000_000)
            .groupingUsed(true)
            .groupingSeparator('.')
            .transferFocusOnEnter(true)
            .linkedValue(model.getIntegerValue())
            .build());

    inputPanel.add(new JLabel("Double"));
    inputPanel.add(ComponentBuilders.doubleField()
            .range(0, 1_000_000_000)
            .groupingUsed(true)
            .maximumFractionDigits(2)
            .decimalSeparator(',')
            .groupingSeparator('.')
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

    add(ComponentBuilders.textField()
            .columns(20)
            .editable(false)
            .focusable(false)
            .border(BorderFactory.createTitledBorder("Message"))
            .linkedValueObserver(model.getMessageValue())
            .build(), BorderLayout.SOUTH);

    Components.setPreferredWidth(this, 380);
  }

  public static void main(String[] args) {
    ApplicationModel model = new ApplicationModel();

    ApplicationPanel panel = new ApplicationPanel(model);

    Dialogs.componentDialogBuilder(panel)
            .title("Codion Input Components Demo")
            .icon(Icons.icons().logoTransparent())
            .show();
  }
}
// end::demoPanel[]
