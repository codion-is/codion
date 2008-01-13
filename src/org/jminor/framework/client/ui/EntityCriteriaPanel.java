/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.TableStatus;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.AbstractSearchModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Property;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class EntityCriteriaPanel extends JPanel {

  private final IntField queryRangeMin = new IntField(4);
  private final IntField queryRangeMax = new IntField(4);

  private final HashMap<PropertySearchModel, PropertySearchPanel> panels = new HashMap<PropertySearchModel, PropertySearchPanel>();

  public EntityCriteriaPanel(final EntityTableModel tableModel) {
    setLayout(new BorderLayout(5,5));

    final JPanel editPanel = new JPanel(new FlowLayout());
    editPanel.setPreferredSize(new Dimension(200,150));
    editPanel.setBorder(BorderFactory.createCompoundBorder());

    final JList propertyList = initializePropertyList(tableModel, editPanel);
    final JScrollPane scroller = new JScrollPane(propertyList);

    final JPanel propertyBase = new JPanel(new BorderLayout());
    propertyBase.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.LIMIT_QUERY)));

    propertyBase.add(scroller, BorderLayout.NORTH);
    propertyBase.add(editPanel, BorderLayout.CENTER);
    add(propertyBase, BorderLayout.CENTER);

    tableModel.refreshSearchComboBoxModels();

    //south panel
    final JPanel southPanel = initializeSouthPanel(tableModel);
    if (southPanel != null)
      add(southPanel, BorderLayout.SOUTH);
  }

  private JPanel initializeSouthPanel(final EntityTableModel tableModel) {
    if (!tableModel.isQueryRangeEnabled() && !tableModel.isFilterQueryByMaster())
      return null;

    final JPanel ret = new JPanel(new BorderLayout());
    if (tableModel.isQueryRangeEnabled()) {
      final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.QUERY_RANGE)));
      queryRangeMin.setText(tableModel.getQueryRangeFrom()+"");
      queryRangeMax.setText(tableModel.getQueryRangeTo()+"");
      panel.add(new JLabel(FrameworkMessages.get(FrameworkMessages.SHOW_RANGE)));
      panel.add(queryRangeMin);
      panel.add(new JLabel(FrameworkMessages.get(FrameworkMessages.TO)));
      panel.add(queryRangeMax);
      final TableStatus status = tableModel.getTableStatus();
      if (status.getRecordCount() > 0)
        panel.add(new JLabel(FrameworkMessages.get(FrameworkMessages.OF) + " " + status.getRecordCount()
                + " " + FrameworkMessages.get(FrameworkMessages.ROWS)));
      ret.add(panel, BorderLayout.NORTH);
    }
    if (tableModel.isFilterQueryByMaster()) {
      final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.FILTER_SETTINGS)));
      panel.add(ControlProvider.createCheckBox(ControlFactory.toggleControl(tableModel,
              "showAllWhenNotFiltered", FrameworkMessages.get(FrameworkMessages.SHOW_ALL_WHEN_NO_FILTER), null)));
      ret.add(panel, BorderLayout.SOUTH);
    }

    return ret;
  }

  private JList initializePropertyList(final EntityTableModel entityModel, final JPanel editorPanel) {
    final List<AbstractSearchModel> searchCriterias = getSortedCriterias(entityModel);
    final Vector<AbstractSearchModel> models = new Vector<AbstractSearchModel>(searchCriterias);
    final JList propertyList = new JList(models);
    for (final AbstractSearchModel model : searchCriterias) {
      model.evtSearchStateChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          propertyList.repaint();
        }
      });
    }
    propertyList.setCellRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component cellRenderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        final PropertySearchModel selected = (PropertySearchModel) value;
        ((JLabel)cellRenderer).setText(selected.getProperty().toString());

        if (selected.stSearchEnabled.isActive())
          cellRenderer.setForeground(Color.red);
        else
          cellRenderer.setForeground(Color.black);

        return cellRenderer;
      }
    });
    propertyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    propertyList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        editorPanel.removeAll();
        final PropertySearchModel selected = (PropertySearchModel) propertyList.getSelectedValue();
        if (selected != null) {
          PropertySearchPanel panel = panels.get(selected);
          if (panel == null) {
            panels.put(selected, panel = new PropertySearchPanel(selected,true,true,entityModel.getDbConnectionProvider()));
          }
          editorPanel.add(panel);
          revalidate();
          repaint();
        }
      }
    });

    return propertyList;
  }

  private List<AbstractSearchModel> getSortedCriterias(final EntityTableModel entityModel) {
    final List<AbstractSearchModel> searchCriterias = entityModel.getPropertySearchModels();
    Collections.sort(searchCriterias, new Comparator<AbstractSearchModel>() {
      public int compare(final AbstractSearchModel searchModelOne, final AbstractSearchModel searchModelTwo) {
        final Property propertyOne = ((PropertySearchModel) searchModelOne).getProperty();
        final Property propertyTwo = ((PropertySearchModel) searchModelTwo).getProperty();
        if (propertyOne.getCaption() != null && propertyTwo.getCaption() != null)
          return propertyOne.getCaption().compareTo(propertyTwo.getCaption());
        else
          return propertyOne.propertyID.compareTo(propertyTwo.propertyID);
      }
    });
    return searchCriterias;
  }

  /**
   * @return Value for property 'queryRangeFrom'.
   */
  public int getQueryRangeFrom() {
    return queryRangeMin.getInt();
  }

  /**
   * @return Value for property 'queryRangeTo'.
   */
  public int getQueryRangeTo() {
    return queryRangeMax.getInt();
  }
}
