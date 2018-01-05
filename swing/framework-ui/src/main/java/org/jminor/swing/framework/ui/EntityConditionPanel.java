/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.EventListener;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.framework.model.ForeignKeyConditionModel;
import org.jminor.framework.model.PropertyConditionModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel for configuring the PropertyConditionModels for a given EntityTableModel.
 */
public final class EntityConditionPanel extends JPanel {

  private static final int DEFAULT_WIDTH = 200;
  private static final int DEFAULT_HEIGHT = 40;

  private final Map<PropertyConditionModel, ColumnConditionPanel> panels = new HashMap<>();

  /**
   * Instantiates a new EntityConditionPanel for the given table model
   * @param tableModel the table model
   */
  public EntityConditionPanel(final EntityTableModel tableModel) {
    setLayout(UiUtil.createBorderLayout());

    final JPanel editPanel = new JPanel(new BorderLayout());
    editPanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

    final JList propertyList = initializePropertyList(tableModel.getConditionModel(), editPanel);
    final JScrollPane scroller = new JScrollPane(propertyList);

    final JPanel propertyBase = new JPanel(new BorderLayout());
    propertyBase.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.LIMIT_QUERY)));

    propertyBase.add(scroller, BorderLayout.NORTH);
    propertyBase.add(editPanel, BorderLayout.CENTER);
    add(propertyBase, BorderLayout.CENTER);

    tableModel.getConditionModel().refresh();

    add(initializeConfigurationPanel(tableModel), BorderLayout.SOUTH);
  }

  private JPanel initializeConfigurationPanel(final EntityTableModel tableModel) {
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.FILTER_SETTINGS)));
    panel.add(ControlProvider.createCheckBox(Controls.toggleControl(tableModel,
            "queryConditionRequired", FrameworkMessages.get(FrameworkMessages.REQUIRE_QUERY_CONDITION))));

    return panel;
  }

  private JList initializePropertyList(final EntityTableConditionModel conditionModel, final JPanel editorPanel) {
    final List<PropertyConditionModel<? extends Property>> searchCondition = getSortedConditions(conditionModel);
    final JList<PropertyConditionModel<? extends Property>> propertyList = new JList<>(new DefaultListModel<>());
    for (final PropertyConditionModel model : searchCondition) {
      ((DefaultListModel<PropertyConditionModel<? extends Property>>) propertyList.getModel()).addElement(model);
      model.addConditionStateListener(new RepaintListener(propertyList));
    }
    propertyList.setCellRenderer(new ConditionListCellRenderer());
    propertyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    propertyList.addListSelectionListener(new ConditionModelSelectionListener(editorPanel, propertyList));

    return propertyList;
  }

  private List<PropertyConditionModel<? extends Property>> getSortedConditions(final EntityTableConditionModel conditionModel) {
    final List<PropertyConditionModel<? extends Property>> conditionModels =
            new ArrayList<>(conditionModel.getPropertyConditionModels());
    conditionModels.sort(new ConditionModelComparator());

    return conditionModels;
  }

  private static final class RepaintListener implements EventListener {
    private final JList propertyList;

    private RepaintListener(final JList propertyList) {
      this.propertyList = propertyList;
    }

    @Override
    public void eventOccurred() {
      propertyList.repaint();
    }
  }

  private static final class ConditionListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                  final boolean isSelected, final boolean cellHasFocus) {
      final Component cellRenderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      final PropertyConditionModel selected = (PropertyConditionModel) value;
      ((JLabel)cellRenderer).setText(selected.getColumnIdentifier().toString());
      cellRenderer.setForeground(selected.isEnabled() ? Color.red : Color.black);

      return cellRenderer;
    }
  }

  private static final class ConditionModelComparator implements
          Comparator<PropertyConditionModel<? extends Property>>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final PropertyConditionModel<? extends Property> o1,
                       final PropertyConditionModel<? extends Property> o2) {
      final Property propertyOne = o1.getColumnIdentifier();
      final Property propertyTwo = o2.getColumnIdentifier();
      if (propertyOne.getCaption() != null && propertyTwo.getCaption() != null) {
        return propertyOne.getCaption().compareTo(propertyTwo.getCaption());
      }
      else {
        return propertyOne.getPropertyId().compareTo(propertyTwo.getPropertyId());
      }
    }
  }

  private final class ConditionModelSelectionListener implements ListSelectionListener {

    private final JPanel editorPanel;
    private final JList propertyList;

    private ConditionModelSelectionListener(final JPanel editorPanel, final JList propertyList) {
      this.editorPanel = editorPanel;
      this.propertyList = propertyList;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void valueChanged(final ListSelectionEvent e) {
      editorPanel.removeAll();
      final PropertyConditionModel selected = (PropertyConditionModel) propertyList.getSelectedValue();
      if (selected != null) {
        ColumnConditionPanel panel = panels.get(selected);
        if (panel == null) {
          if (selected instanceof ForeignKeyConditionModel) {
            panel = new ForeignKeyConditionPanel((ForeignKeyConditionModel) selected, true, true);
          }
          else {
            panel = new PropertyConditionPanel(selected, true, true);
          }
          panels.put(selected, panel);
        }

        editorPanel.add(panel, BorderLayout.NORTH);
        revalidate();
        repaint();
      }
    }
  }
}
