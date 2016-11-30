package org.jminor.swing.common.model.checkbox;

import javax.swing.JToggleButton.ToggleButtonModel;
import java.awt.event.ItemEvent;

/**
 * A ToggleButtonModel implementation which includes an undetermined state.
 * http://www.javaspecialists.eu/archive/Issue145.html
 * @author Heinz M. Kabutz
 */
public final class TristateButtonModel extends ToggleButtonModel {
  private TristateState state = TristateState.DESELECTED;

  public TristateButtonModel(final TristateState state) {
    setState(state);
  }

  public TristateButtonModel() {
    this(TristateState.DESELECTED);
  }

  public void setIndeterminate() {
    setState(TristateState.INDETERMINATE);
  }

  public boolean isIndeterminate() {
    return state == TristateState.INDETERMINATE;
  }

  // Overrides of superclass methods
  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    // Restore state display
    displayState();
  }

  @Override
  public void setSelected(final boolean selected) {
    setState(selected ? TristateState.SELECTED : TristateState.DESELECTED);
  }

  @Override
  public void setArmed(final boolean b) {/*N/A*/}

  @Override
  public void setPressed(final boolean b) {/*N/A*/}

  public void iterateState() {
    setState(state.next());
  }

  private void setState(final TristateState state) {
    //Set internal state
    this.state = state;
    displayState();
    if (state == TristateState.INDETERMINATE && isEnabled()) {
      // force the events to fire

      // Send ChangeEvent
      fireStateChanged();

      // Send ItemEvent
      final int indeterminate = 3;
      fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, indeterminate));
    }
  }

  private void displayState() {
    super.setSelected(state != TristateState.DESELECTED);
    super.setArmed(state == TristateState.INDETERMINATE);
    super.setPressed(state == TristateState.INDETERMINATE);
  }

  public TristateState getState() {
    return state;
  }
}