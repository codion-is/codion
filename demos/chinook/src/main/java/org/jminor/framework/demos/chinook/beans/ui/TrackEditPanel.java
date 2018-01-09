/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;
import org.jminor.swing.framework.ui.EntityUiUtil;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class TrackEditPanel extends EntityEditPanel {

  public TrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(TRACK_ALBUMID_FK);
    final JTextField txtAlbum = createForeignKeyLookupField(TRACK_ALBUMID_FK);
    txtAlbum.setColumns(18);
    final JTextField txtName = createTextField(TRACK_NAME);
    txtName.setColumns(18);
    final EntityComboBox mediaTypeBox = createForeignKeyComboBox(TRACK_MEDIATYPEID_FK);
    final Action newMediaTypeAction = EntityEditPanel.createEditPanelAction(mediaTypeBox,
            new EntityPanelProvider(T_MEDIATYPE, getEditModel().getEntities().getCaption(T_MEDIATYPE))
                    .setEditPanelClass(MediaTypeEditPanel.class));
    final JPanel mediaTypePanel = EntityUiUtil.createEastButtonPanel(mediaTypeBox, newMediaTypeAction, false);
    final EntityComboBox genreBox = createForeignKeyComboBox(TRACK_GENREID_FK);
    final Action newGenreAction = EntityEditPanel.createEditPanelAction(genreBox,
            new EntityPanelProvider(T_GENRE, getEditModel().getEntities().getCaption(T_GENRE))
                    .setEditPanelClass(GenreEditPanel.class));
    final JPanel genrePanel = EntityUiUtil.createEastButtonPanel(genreBox, newGenreAction, false);
    final TextInputPanel txtComposer = createTextInputPanel(TRACK_COMPOSER);
    txtComposer.getTextField().setColumns(18);
    final IntegerField txtMilliseconds = (IntegerField) createTextField(TRACK_MILLISECONDS);
    txtMilliseconds.setGroupingUsed(true);
    final IntegerField txtBytes = (IntegerField) createTextField(TRACK_BYTES);
    txtBytes.setGroupingUsed(true);
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
    add(createPropertyPanel(TRACK_GENREID_FK, genrePanel));
    addPropertyPanel(TRACK_COMPOSER);
    add(createPropertyPanel(TRACK_MEDIATYPEID_FK, mediaTypePanel));
    addPropertyPanel(TRACK_BYTES);
    addPropertyPanel(TRACK_UNITPRICE);
    add(durationPanel);
  }
}