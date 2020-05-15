/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.ui.MinutesSecondsPanelValue.MinutesSecondsPanel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;

import javax.swing.Action;
import javax.swing.JPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.Components.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

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
    setPreferredHeight(mediaTypeBox, getPreferredTextFieldHeight());
    final Action newMediaTypeAction = new EntityPanelBuilder(T_MEDIATYPE).setEditPanelClass(MediaTypeEditPanel.class)
            .createEditPanelAction(mediaTypeBox);
    final JPanel mediaTypePanel = Components.createEastButtonPanel(mediaTypeBox, newMediaTypeAction);
    final EntityComboBox genreBox = createForeignKeyComboBox(TRACK_GENRE_FK);
    setPreferredHeight(genreBox, getPreferredTextFieldHeight());
    final Action newGenreAction = new EntityPanelBuilder(T_GENRE)
            .setEditPanelClass(GenreEditPanel.class).createEditPanelAction(genreBox);
    final JPanel genrePanel = Components.createEastButtonPanel(genreBox, newGenreAction);
    createTextInputPanel(TRACK_COMPOSER).getTextField().setColumns(18);
    final IntegerField millisecondsField = (IntegerField) createTextField(TRACK_MILLISECONDS);
    millisecondsField.setGroupingUsed(true);
    final IntegerField bytesField = (IntegerField) createTextField(TRACK_BYTES);
    bytesField.setGroupingUsed(true);
    bytesField.setColumns(18);
    createTextField(TRACK_UNITPRICE).setColumns(18);

    final ComponentValue<Integer, MinutesSecondsPanel> minutesSecondsValue = new MinutesSecondsPanelValue();
    minutesSecondsValue.link(getEditModel().value(TRACK_MILLISECONDS));
    final JPanel durationPanel = new JPanel(gridLayout(1, 2));
    durationPanel.add(createPropertyPanel(TRACK_MILLISECONDS, millisecondsField));
    durationPanel.add(minutesSecondsValue.getComponent());

    setLayout(flexibleGridLayout(4, 2));
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