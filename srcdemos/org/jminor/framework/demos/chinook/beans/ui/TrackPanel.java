/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

public class TrackPanel extends EntityEditPanel {

  public TrackPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(TRACK_ALBUMID_FK);
    final JTextField txtAlbum = createEntityLookupField(TRACK_ALBUMID_FK);
    txtAlbum.setColumns(18);
    final JTextField txtName = createTextField(TRACK_NAME);
    txtName.setColumns(18);
    final JComboBox cmbMediaType = createEntityComboBox(TRACK_MEDIATYPEID_FK);
    final JComboBox cmbGenre = createEntityComboBox(TRACK_GENREID_FK);
    final TextInputPanel txtComposer = createTextInputPanel(TRACK_COMPOSER);
    ((JTextField) txtComposer.getTextComponent()).setColumns(18);
    final JTextField txtMilliseconds = createTextField(TRACK_MILLISECONDS);
    final JTextField txtBytes = createTextField(TRACK_BYTES);
    txtBytes.setColumns(18);
    final JTextField txtUnitPrice = createTextField(TRACK_UNITPRICE);
    txtUnitPrice.setColumns(18);
    final JTextField txtDuration = createTextField(TRACK_MINUTES_SECONDS_DERIVED, LinkType.READ_ONLY);
    final JPanel durationPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    durationPanel.add(createPropertyPanel(TRACK_MILLISECONDS, txtMilliseconds));
    durationPanel.add(createPropertyPanel(new JLabel("(min/sec)"), txtDuration, true));

    setLayout(new FlexibleGridLayout(4, 2, 5, 5, true, false));
    add(createPropertyPanel(TRACK_ALBUMID_FK, txtAlbum));
    add(createPropertyPanel(TRACK_NAME, txtName));
    add(createPropertyPanel(TRACK_GENREID_FK, cmbGenre));
    add(createPropertyPanel(TRACK_COMPOSER, txtComposer));
    add(createPropertyPanel(TRACK_MEDIATYPEID_FK, cmbMediaType));
    add(createPropertyPanel(TRACK_BYTES, txtBytes));
    add(createPropertyPanel(TRACK_UNITPRICE, txtUnitPrice));
    add(durationPanel);
  }
}