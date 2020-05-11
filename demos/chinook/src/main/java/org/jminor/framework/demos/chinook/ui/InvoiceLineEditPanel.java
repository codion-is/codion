/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.swing.common.ui.KeyEvents;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityLookupField;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static org.jminor.common.model.table.SortingDirective.ASCENDING;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class InvoiceLineEditPanel extends EntityEditPanel {

  private JTextField tableSearchField;

  public InvoiceLineEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
    editModel.setPersistValue(INVOICELINE_TRACK_FK, false);
  }

  public void setTableSearchFeld(final JTextField tableSearchField) {
    this.tableSearchField = tableSearchField;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(INVOICELINE_TRACK_FK);

    final EntityLookupField trackLookupField = createForeignKeyLookupField(INVOICELINE_TRACK_FK);
    configureTrackLookup(trackLookupField);
    trackLookupField.setColumns(15);
    final JTextField quantityField = createTextField(INVOICELINE_QUANTITY);
    KeyEvents.removeTransferFocusOnEnter(quantityField);//otherwise the action added below wont work
    quantityField.addActionListener(getSaveControl());

    setLayout(new BorderLayout(5, 5));
    add(createPropertyPanel(INVOICELINE_TRACK_FK), BorderLayout.WEST);
    add(createPropertyPanel(INVOICELINE_QUANTITY), BorderLayout.CENTER);
    add(createPropertyPanel(new JLabel(" "), tableSearchField), BorderLayout.EAST);
  }

  private void configureTrackLookup(final EntityLookupField trackField) {
    final EntityLookupField.TableSelectionProvider trackSelectionProvider =
            new EntityLookupField.TableSelectionProvider(trackField.getModel());
    final SwingEntityTableModel tableModel = trackSelectionProvider.getTable().getModel();
    tableModel.setColumns(TRACK_ARTIST_DENORM, TRACK_ALBUM_FK, TRACK_NAME);
    tableModel.setSortingDirective(TRACK_ARTIST_DENORM, ASCENDING);
    tableModel.addSortingDirective(TRACK_ALBUM_FK, ASCENDING);
    tableModel.addSortingDirective(TRACK_NAME, ASCENDING);
    trackSelectionProvider.setPreferredSize(new Dimension(500, 300));
    trackField.setSelectionProvider(trackSelectionProvider);
  }
}