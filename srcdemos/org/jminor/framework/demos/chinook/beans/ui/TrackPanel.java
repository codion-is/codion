/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class TrackPanel extends EntityPanel {

  public TrackPanel(final EntityModel model) {
    super(model, "Tracks");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(5, 2, 5, 5));
        final JTextField txtAlbum = createEntityLookupField(Chinook.TRACK_ALBUMID_FK);
        txtAlbum.setColumns(18);
        setDefaultFocusComponent(txtAlbum);
        final JTextField txtName = createTextField(Chinook.TRACK_NAME);
        txtName.setColumns(18);
        final JComboBox cmbMediaType = createEntityComboBox(Chinook.TRACK_MEDIATYPEID_FK);
        final JComboBox cmbGenre = createEntityComboBox(Chinook.TRACK_GENREID_FK);
        final TextInputPanel txtComposer = createTextInputPanel(Chinook.TRACK_COMPOSER);
        ((JTextField) txtComposer.getTextComponent()).setColumns(18);
        final JTextField txtMilliseconds = createTextField(Chinook.TRACK_MILLISECONDS);
        txtMilliseconds.setColumns(18);
        final JTextField txtBytes = createTextField(Chinook.TRACK_BYTES);
        txtBytes.setColumns(18);
        final JTextField txtUnitPrice = createTextField(Chinook.TRACK_UNITPRICE);
        txtUnitPrice.setColumns(18);
        final JTextField txtDuration = createTextField(Chinook.TRACK_MINUTES_SECONDS_TRANSIENT, LinkType.READ_ONLY);

        add(createPropertyPanel(Chinook.TRACK_ALBUMID_FK, txtAlbum));
        add(createPropertyPanel(Chinook.TRACK_NAME, txtName));
        add(createPropertyPanel(Chinook.TRACK_GENREID_FK, cmbGenre));
        add(createPropertyPanel(Chinook.TRACK_COMPOSER, txtComposer));
        add(createPropertyPanel(Chinook.TRACK_MEDIATYPEID_FK, cmbMediaType));
        add(createPropertyPanel(Chinook.TRACK_BYTES, txtBytes));
        add(createPropertyPanel(Chinook.TRACK_UNITPRICE, txtUnitPrice));
        add(createPropertyPanel(Chinook.TRACK_MILLISECONDS, txtMilliseconds));
        add(new JLabel());
        add(createPropertyPanel(Chinook.TRACK_MINUTES_SECONDS_TRANSIENT, txtDuration));
      }
    };
  }
}