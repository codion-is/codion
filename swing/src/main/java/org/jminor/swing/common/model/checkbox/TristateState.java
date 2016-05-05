package org.jminor.swing.common.model.checkbox;

/**
 * http://www.javaspecialists.eu/archive/Issue145.html
 * @author Heinz M. Kabutz
 */
public enum TristateState {
  SELECTED {
    @Override
    public TristateState next() {
      return INDETERMINATE;
    }
  },
  INDETERMINATE {
    @Override
    public TristateState next() {
      return DESELECTED;
    }
  },
  DESELECTED {
    @Override
    public TristateState next() {
      return SELECTED;
    }
  };

  /**
   * @return the next logical state, INDETERMINATE-DESELECTED-SELECTED-...
   */
  public abstract TristateState next();
}

