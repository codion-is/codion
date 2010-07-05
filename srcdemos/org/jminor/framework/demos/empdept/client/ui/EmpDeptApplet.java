package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.framework.client.ui.EntityApplet;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

/**
 * User: Björn Darri
 * Date: 3.7.2010
 * Time: 23:02:08
 */
public class EmpDeptApplet extends EntityApplet {

  public EmpDeptApplet() {
    super(new EntityPanelProvider(EmpDept.T_DEPARTMENT).setPanelClass(DepartmentPanel.class));
  }
}
