/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.ui.MinutesSecondsPanelValue.MinutesSecondsPanel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.panel.Panels;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.Action;
import javax.swing.JPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class TrackEditPanel extends EntityEditPanel {

  public TrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(18);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Track.ALBUM_FK);

    createForeignKeySearchField(Track.ALBUM_FK);
    createTextField(Track.NAME);
    final EntityComboBox mediaTypeBox = createForeignKeyComboBox(Track.MEDIATYPE_FK)
            .build();
    final Action newMediaTypeAction = EntityPanel.builder(MediaType.TYPE)
            .editPanelClass(MediaTypeEditPanel.class)
            .createEditPanelAction(mediaTypeBox);
    final JPanel mediaTypePanel = Panels.createEastButtonPanel(mediaTypeBox, newMediaTypeAction);
    final EntityComboBox genreBox = createForeignKeyComboBox(Track.GENRE_FK)
            .build();
    final Action newGenreAction = EntityPanel.builder(Genre.TYPE)
            .editPanelClass(GenreEditPanel.class)
            .createEditPanelAction(genreBox);
    final JPanel genrePanel = Panels.createEastButtonPanel(genreBox, newGenreAction);
    createTextInputPanel(Track.COMPOSER)
            .buttonFocusable(false);
    createIntegerField(Track.MILLISECONDS)
            .groupingUsed(true)
            .columns(8)
            .build();
    createIntegerField(Track.BYTES)
            .groupingUsed(true)
            .build();
    createTextField(Track.UNITPRICE);

    final ComponentValue<Integer, MinutesSecondsPanel> minutesSecondsValue = new MinutesSecondsPanelValue();
    minutesSecondsValue.link(getEditModel().value(Track.MILLISECONDS));
    final JPanel durationPanel = new JPanel(gridLayout(1, 2));
    durationPanel.add(createInputPanel(Track.MILLISECONDS));
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