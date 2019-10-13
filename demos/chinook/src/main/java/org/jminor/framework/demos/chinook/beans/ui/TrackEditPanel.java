/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.UpdateTrigger;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

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

    createForeignKeyLookupField(TRACK_ALBUMID_FK).setColumns(18);
    createTextField(TRACK_NAME).setColumns(18);
    final EntityComboBox mediaTypeBox = createForeignKeyComboBox(TRACK_MEDIATYPEID_FK);
    final Action newMediaTypeAction = EntityEditPanel.createEditPanelAction(mediaTypeBox,
            new EntityPanelProvider(T_MEDIATYPE)
                    .setEditPanelClass(MediaTypeEditPanel.class));
    final JPanel mediaTypePanel = UiUtil.createEastButtonPanel(mediaTypeBox, newMediaTypeAction, false);
    final EntityComboBox genreBox = createForeignKeyComboBox(TRACK_GENREID_FK);
    final Action newGenreAction = EntityEditPanel.createEditPanelAction(genreBox,
            new EntityPanelProvider(T_GENRE)
                    .setEditPanelClass(GenreEditPanel.class));
    final JPanel genrePanel = UiUtil.createEastButtonPanel(genreBox, newGenreAction, false);
    createTextInputPanel(TRACK_COMPOSER).getTextField().setColumns(18);
    final IntegerField millisecondsField = (IntegerField) createTextField(TRACK_MILLISECONDS);
    millisecondsField.setGroupingUsed(true);
    final IntegerField bytesField = (IntegerField) createTextField(TRACK_BYTES);
    bytesField.setGroupingUsed(true);
    bytesField.setColumns(18);
    createTextField(TRACK_UNITPRICE).setColumns(18);
    final JTextField durationField = createTextField(TRACK_MINUTES_SECONDS_DERIVED, UpdateTrigger.READ_ONLY);
    final JPanel durationPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    durationPanel.add(createPropertyPanel(TRACK_MILLISECONDS, millisecondsField));
    durationPanel.add(createPropertyPanel(new JLabel("(min/sec)"), durationField, true));

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