/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class TrackEditPanel extends EntityEditPanel {

  public TrackEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(TRACK_ALBUMID_FK);
    final JTextField txtAlbum = createForeignKeyLookupField(TRACK_ALBUMID_FK);
    txtAlbum.setColumns(18);
    final JTextField txtName = createTextField(TRACK_NAME);
    txtName.setColumns(18);
    createForeignKeyComboBox(TRACK_MEDIATYPEID_FK);
    createForeignKeyComboBox(TRACK_GENREID_FK);
    final TextInputPanel txtComposer = createTextInputPanel(TRACK_COMPOSER);
    txtComposer.getTextField().setColumns(18);
    final JTextField txtMilliseconds = createTextField(TRACK_MILLISECONDS);
    final JTextField txtBytes = createTextField(TRACK_BYTES);
    txtBytes.setColumns(18);
    final JTextField txtUnitPrice = createTextField(TRACK_UNITPRICE);
    txtUnitPrice.setColumns(18);
    final JTextField txtDuration = createTextField(TRACK_MINUTES_SECONDS_DERIVED, true);
    final JPanel durationPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    durationPanel.add(createPropertyPanel(TRACK_MILLISECONDS, txtMilliseconds));
    durationPanel.add(createPropertyPanel(new JLabel("(min/sec)"), txtDuration, true));

    setLayout(new FlexibleGridLayout(4, 2, 5, 5, true, false));
    addPropertyPanel(TRACK_ALBUMID_FK);
    addPropertyPanel(TRACK_NAME);
    addPropertyPanel(TRACK_GENREID_FK);
    addPropertyPanel(TRACK_COMPOSER);
    addPropertyPanel(TRACK_MEDIATYPEID_FK);
    addPropertyPanel(TRACK_BYTES);
    addPropertyPanel(TRACK_UNITPRICE);
    add(durationPanel);
  }
}