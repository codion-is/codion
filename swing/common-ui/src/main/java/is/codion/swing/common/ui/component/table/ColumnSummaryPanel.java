/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
 * A panel that shows a summary value for a numerical column property.
 * For instances use the {@link #columnSummaryPanel(ColumnSummaryModel)} factory method.
 */
public final class ColumnSummaryPanel extends JPanel {

  private ColumnSummaryPanel(ColumnSummaryModel model) {
    setLayout(new BorderLayout());
    add(createSummaryField(requireNonNull(model, "model")), BorderLayout.CENTER);
  }

  /**
   * @param columnSummaryModel the {@link ColumnSummaryModel} instance
   * @return a new {@link ColumnSummaryPanel} instance.
   */
  public static ColumnSummaryPanel columnSummaryPanel(ColumnSummaryModel columnSummaryModel) {
    return new ColumnSummaryPanel(columnSummaryModel);
  }

  private static JTextField createSummaryField(ColumnSummaryModel model) {
    JPopupMenu menu = createPopupMenu(model);
    return Components.textField()
            .linkedValueObserver(model.summaryText())
            .horizontalAlignment(SwingConstants.RIGHT)
            .editable(false)
            .focusable(false)
            .popupMenu(menu)
            .mouseListener(new MouseAdapter() {
              @Override
              public void mouseReleased(MouseEvent e) {
                if (!model.locked().get()) {
                  menu.show(e.getComponent(), e.getX(), e.getY() - menu.getPreferredSize().height);
                }
              }
            })
            .build();
  }

  private static JPopupMenu createPopupMenu(ColumnSummaryModel model) {
    JPopupMenu popupMenu = new JPopupMenu();
    ButtonGroup group = new ButtonGroup();
    for (ColumnSummaryModel.Summary summary : model.summaries()) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(Control.builder(() -> model.summary().set(summary))
              .name(summary.toString())
              .build());
      model.summary().addDataListener(newSummary -> item.setSelected(newSummary.equals(summary)));
      item.setSelected(model.summary().get().equals(summary));
      group.add(item);
      popupMenu.add(item);
    }

    return popupMenu;
  }
}
