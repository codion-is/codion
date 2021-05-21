/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.ui.MinutesSecondsPanelValue.MinutesSecondsPanel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.Action;
import javax.swing.JPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

public class TrackEditPanel extends EntityEditPanel {

  public TrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Track.ALBUM_FK);

    foreignKeySearchFieldBuilder(Track.ALBUM_FK)
            .columns(18)
            .build();
    textFieldBuilder(Track.NAME).columns(18).build();
    final EntityComboBox mediaTypeBox = foreignKeyComboBoxBuilder(Track.MEDIATYPE_FK)
            .preferredHeight(getPreferredTextFieldHeight())
            .build();
    final Action newMediaTypeAction = EntityPanel.builder(MediaType.TYPE)
            .editPanelClass(MediaTypeEditPanel.class)
            .createEditPanelAction(mediaTypeBox);
    final JPanel mediaTypePanel = Components.createEastButtonPanel(mediaTypeBox, newMediaTypeAction);
    final EntityComboBox genreBox = foreignKeyComboBoxBuilder(Track.GENRE_FK)
            .preferredHeight(getPreferredTextFieldHeight())
            .build();
    final Action newGenreAction = EntityPanel.builder(Genre.TYPE)
            .editPanelClass(GenreEditPanel.class)
            .createEditPanelAction(genreBox);
    final JPanel genrePanel = Components.createEastButtonPanel(genreBox, newGenreAction);
    textInputPanelBuilder(Track.COMPOSER)
            .columns(18)
            .buttonFocusable(false)
            .build();
    final IntegerField millisecondsField = (IntegerField) textFieldBuilder(Track.MILLISECONDS).build();
    millisecondsField.setGroupingUsed(true);
    final IntegerField bytesField = (IntegerField) textFieldBuilder(Track.BYTES)
            .columns(18)
            .build();
    bytesField.setGroupingUsed(true);
    textFieldBuilder(Track.UNITPRICE)
            .columns(18)
            .build();

    final ComponentValue<Integer, MinutesSecondsPanel> minutesSecondsValue = new MinutesSecondsPanelValue();
    minutesSecondsValue.link(getEditModel().value(Track.MILLISECONDS));
    final JPanel durationPanel = new JPanel(gridLayout(1, 2));
    durationPanel.add(createInputPanel(Track.MILLISECONDS, millisecondsField));
    durationPanel.add(minutesSecondsValue.getComponent());

    setLayout(flexibleGridLayout(4, 2));
    addInputPanel(Track.ALBUM_FK);
    addInputPanel(Track.NAME);
    addInputPanel(Track.GENRE_FK, genrePanel);
    addInputPanel(Track.COMPOSER);
    addInputPanel(Track.MEDIATYPE_FK, mediaTypePanel);
    addInputPanel(Track.BYTES);
    addInputPanel(Track.UNITPRICE);
    add(durationPanel);
  }
}