package is.codion.swing.framework.ui;

import is.codion.common.Conjunction;
import is.codion.common.event.EventListener;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.value.TextValues;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.control.Control.controlBuilder;
import static java.util.Objects.requireNonNull;

/**
 * A simple table condition panel, presenting a single text field for condition input.
 */
public final class EntityTableSimpleConditionPanel extends AbstractEntityTableConditionPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTableSimpleConditionPanel.class.getName());

  private final JTextField simpleSearchTextField = new JTextField(12);
  private final EventListener onSearchListener;

  /**
   * Instantiates a new EntityTableSimpleConditionPanel
   * @param tableConditionModel the table condition model
   * @param columnModel the column model
   * @param onSearchListener notified when this condition panel triggers a search
   */
  public EntityTableSimpleConditionPanel(final EntityTableConditionModel tableConditionModel,
                                         final SwingFilteredTableColumnModel<?, ?> columnModel,
                                         final EventListener onSearchListener) {
    super(tableConditionModel, columnModel.getAllColumns());
    this.onSearchListener = requireNonNull(onSearchListener);
    setLayout(Layouts.borderLayout());
    add(initializeSimpleConditionPanel(tableConditionModel), BorderLayout.CENTER);
  }

  /**
   * @return the search field
   */
  public JTextField getSimpleSearchTextField() {
    return simpleSearchTextField;
  }

  /**
   * Sets the search text in case simple search is enabled
   * @param searchText the search text
   */
  public void setSearchText(final String searchText) {
    simpleSearchTextField.setText(searchText);
  }

  @Override
  public void selectConditionPanel() {
    simpleSearchTextField.requestFocusInWindow();
  }

  private JPanel initializeSimpleConditionPanel(final EntityTableConditionModel conditionModel) {
    final Control simpleSearchControl = controlBuilder().command(this::performSimpleSearch).name(FrameworkMessages.get(FrameworkMessages.SEARCH)).build();
    final JButton simpleSearchButton = new JButton(simpleSearchControl);
    simpleSearchTextField.addActionListener(simpleSearchControl);
    final JPanel panel = new JPanel(Layouts.borderLayout());
    TextValues.textValue(simpleSearchTextField).link(conditionModel.getSimpleConditionStringValue());
    panel.setBorder(BorderFactory.createTitledBorder(MESSAGES.getString("condition")));
    panel.add(simpleSearchTextField, BorderLayout.WEST);
    panel.add(simpleSearchButton, BorderLayout.EAST);

    return panel;
  }

  private void performSimpleSearch() {
    final Conjunction previousConjunction = getTableConditionModel().getConjunction();
    try {
      getTableConditionModel().setConjunction(Conjunction.OR);
      onSearchListener.onEvent();
    }
    finally {
      getTableConditionModel().setConjunction(previousConjunction);
    }
  }
}
