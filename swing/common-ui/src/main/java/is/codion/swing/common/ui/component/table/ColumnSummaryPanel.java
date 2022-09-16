/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.util.Objects.requireNonNull;

/**
 * A panel that shows a summary value for a numerical column property in a EntityTableModel.
 */
public final class ColumnSummaryPanel extends JPanel {

  /**
   * @param model the PropertySummaryModel instance
   */
  public ColumnSummaryPanel(ColumnSummaryModel model) {
    setLayout(new BorderLayout());
    add(createSummaryField(requireNonNull(model, "model")), BorderLayout.CENTER);
  }



  private static JTextField createSummaryField(ColumnSummaryModel model) {
    JPopupMenu menu = createPopupMenu(model);
    return Components.textField()
            .linkedValueObserver(model.summaryTextObserver())
            .horizontalAlignment(SwingConstants.RIGHT)
            .editable(false)
            .focusable(false)
            .popupMenu(menu)
            .mouseListener(new MouseAdapter() {
              @Override
              public void mouseReleased(MouseEvent e) {
                if (!model.lockedState().get()) {
                  menu.show(e.getComponent(), e.getX(), e.getY() - menu.getPreferredSize().height);
                }
              }
            })
            .build();
  }

  private static JPopupMenu createPopupMenu(ColumnSummaryModel model) {
    JPopupMenu popupMenu = new JPopupMenu();
    ButtonGroup group = new ButtonGroup();
    for (ColumnSummaryModel.Summary summary : model.availableSummaries()) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(Control.builder(() -> model.summaryValue().set(summary))
              .caption(summary.toString())
              .build());
      model.summaryValue().addDataListener(newSummary -> item.setSelected(newSummary.equals(summary)));
      item.setSelected(model.summaryValue().equals(summary));
      group.add(item);
      popupMenu.add(item);
    }

    return popupMenu;
  }
}
