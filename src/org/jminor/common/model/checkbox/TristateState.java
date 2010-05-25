package org.jminor.common.model.checkbox;

/**
 * http://www.javaspecialists.eu/archive/Issue145.html
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

  public abstract TristateState next();
}

