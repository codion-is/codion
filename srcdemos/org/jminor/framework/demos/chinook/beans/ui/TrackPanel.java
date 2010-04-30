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
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

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
        final JTextField txtAlbum = createEntityLookupField(TRACK_ALBUMID_FK);
        txtAlbum.setColumns(18);
        setDefaultFocusComponent(txtAlbum);
        final JTextField txtName = createTextField(TRACK_NAME);
        txtName.setColumns(18);
        final JComboBox cmbMediaType = createEntityComboBox(TRACK_MEDIATYPEID_FK);
        final JComboBox cmbGenre = createEntityComboBox(TRACK_GENREID_FK);
        final TextInputPanel txtComposer = createTextInputPanel(TRACK_COMPOSER);
        ((JTextField) txtComposer.getTextComponent()).setColumns(18);
        final JTextField txtMilliseconds = createTextField(TRACK_MILLISECONDS);
        txtMilliseconds.setColumns(18);
        final JTextField txtBytes = createTextField(TRACK_BYTES);
        txtBytes.setColumns(18);
        final JTextField txtUnitPrice = createTextField(TRACK_UNITPRICE);
        txtUnitPrice.setColumns(18);
        final JTextField txtDuration = createTextField(TRACK_MINUTES_SECONDS_DERIVED, LinkType.READ_ONLY);

        add(createPropertyPanel(TRACK_ALBUMID_FK, txtAlbum));
        add(createPropertyPanel(TRACK_NAME, txtName));
        add(createPropertyPanel(TRACK_GENREID_FK, cmbGenre));
        add(createPropertyPanel(TRACK_COMPOSER, txtComposer));
        add(createPropertyPanel(TRACK_MEDIATYPEID_FK, cmbMediaType));
        add(createPropertyPanel(TRACK_BYTES, txtBytes));
        add(createPropertyPanel(TRACK_UNITPRICE, txtUnitPrice));
        add(createPropertyPanel(TRACK_MILLISECONDS, txtMilliseconds));
        add(new JLabel());
        add(createPropertyPanel(TRACK_MINUTES_SECONDS_DERIVED, txtDuration));
      }
    };
  }
}