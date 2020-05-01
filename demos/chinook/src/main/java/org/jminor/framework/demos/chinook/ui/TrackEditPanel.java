/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;

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
    setInitialFocusProperty(TRACK_ALBUM_FK);

    createForeignKeyLookupField(TRACK_ALBUM_FK).setColumns(18);
    createTextField(TRACK_NAME).setColumns(18);
    final EntityComboBox mediaTypeBox = createForeignKeyComboBox(TRACK_MEDIATYPE_FK);
    final Action newMediaTypeAction = EntityEditPanel.createEditPanelAction(mediaTypeBox,
            new EntityPanelBuilder(T_MEDIATYPE)
                    .setEditPanelClass(MediaTypeEditPanel.class));
    final JPanel mediaTypePanel = Components.createEastButtonPanel(mediaTypeBox, newMediaTypeAction);
    final EntityComboBox genreBox = createForeignKeyComboBox(TRACK_GENRE_FK);
    final Action newGenreAction = EntityEditPanel.createEditPanelAction(genreBox,
            new EntityPanelBuilder(T_GENRE)
                    .setEditPanelClass(GenreEditPanel.class));
    final JPanel genrePanel = Components.createEastButtonPanel(genreBox, newGenreAction);
    createTextInputPanel(TRACK_COMPOSER).getTextField().setColumns(18);
    final IntegerField millisecondsField = (IntegerField) createTextField(TRACK_MILLISECONDS);
    millisecondsField.setGroupingUsed(true);
    final IntegerField bytesField = (IntegerField) createTextField(TRACK_BYTES);
    bytesField.setGroupingUsed(true);
    bytesField.setColumns(18);
    createTextField(TRACK_UNITPRICE).setColumns(18);
    final JTextField durationField = createTextField(TRACK_MINUTES_SECONDS_DERIVED);
    final JPanel durationPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    durationPanel.add(createPropertyPanel(TRACK_MILLISECONDS, millisecondsField));
    durationPanel.add(createPropertyPanel(new JLabel("(min/sec)"), durationField));

    setLayout(new FlexibleGridLayout(4, 2, 5, 5, true, false));
    addPropertyPanel(TRACK_ALBUM_FK);
    addPropertyPanel(TRACK_NAME);
    add(createPropertyPanel(TRACK_GENRE_FK, genrePanel));
    addPropertyPanel(TRACK_COMPOSER);
    add(createPropertyPanel(TRACK_MEDIATYPE_FK, mediaTypePanel));
    addPropertyPanel(TRACK_BYTES);
    addPropertyPanel(TRACK_UNITPRICE);
    add(durationPanel);
  }
}