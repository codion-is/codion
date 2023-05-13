/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import is.codion.common.event.EventDataListener;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultIcons implements Icons {

  private static final String BUTTON_FOREGROUND_PROPERTY = "Button.foreground";

  private final Map<Ikon, FontImageIcon> icons = new HashMap<>();
  private final Map<Integer, FontImageIcon> logos = new HashMap<>();

  private final OnIconColorChangedListener onIconColorChangedListener = new OnIconColorChangedListener();

  static {
    UIManager.addPropertyChangeListener(new OnLookAndFeelChangedListener());
  }

  DefaultIcons() {
    ICON_COLOR.addWeakDataListener(onIconColorChangedListener);
  }

  @Override
  public void add(Ikon... ikons) {
    for (Ikon ikon : requireNonNull(ikons)) {
      if (icons.containsKey(requireNonNull(ikon))) {
        throw new IllegalArgumentException("Icon has already been added: " + ikon);
      }
    }
    for (Ikon ikon : ikons) {
      icons.put(ikon, FontImageIcon.of(ikon));
    }
  }

  @Override
  public ImageIcon icon(Ikon ikon) {
    if (!icons.containsKey(requireNonNull(ikon))) {
      throw new IllegalArgumentException("No icon has been added for key: " + ikon);
    }

    return icons.get(ikon).imageIcon();
  }

  private final class OnIconColorChangedListener implements EventDataListener<Color> {

    @Override
    public void onEvent(Color color) {
      if (color != null) {
        icons.values().forEach(icon -> icon.setColor(color));
        logos.values().forEach(logo -> logo.setColor(color));
      }
    }
  }

  private static final class OnLookAndFeelChangedListener implements PropertyChangeListener {

    private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
      if (propertyChangeEvent.getPropertyName().equals(LOOK_AND_FEEL_PROPERTY)) {
        if (ICON_COLOR != null) {
          ICON_COLOR.set(UIManager.getColor(BUTTON_FOREGROUND_PROPERTY));
        }
      }
    }
  }
}
