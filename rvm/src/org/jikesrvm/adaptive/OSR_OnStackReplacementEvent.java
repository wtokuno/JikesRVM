/*
 * This file is part of Jikes RVM (http://jikesrvm.sourceforge.net).
 * The Jikes RVM project is distributed under the Common Public License (CPL).
 * A copy of the license is included in the distribution, and is also
 * available at http://www.opensource.org/licenses/cpl1.0.php
 *
 * (C) Copyright IBM Corp 2002
 */

package org.jikesrvm.adaptive;

import org.jikesrvm.adaptive.controller.VM_Controller;
import org.jikesrvm.adaptive.controller.VM_ControllerInputEvent;
import org.jikesrvm.adaptive.controller.VM_ControllerMemory;
import org.jikesrvm.adaptive.controller.VM_ControllerPlan;
import org.jikesrvm.adaptive.util.VM_AOSLogging;
import org.jikesrvm.classloader.VM_NormalMethod;
import org.jikesrvm.compilers.common.VM_CompiledMethod;
import org.jikesrvm.compilers.common.VM_CompiledMethods;
import org.jikesrvm.compilers.common.VM_RuntimeCompiler;
import org.jikesrvm.compilers.opt.OPT_CompilationPlan;
import org.jikesrvm.compilers.opt.OPT_OptimizationPlanElement;
import org.jikesrvm.compilers.opt.OPT_Options;
import org.jikesrvm.scheduler.VM_Thread;
import org.vmmagic.unboxed.Offset;

/**
 * Event generated by a thread aware of on-stack-replacement request.
 * The event is feed to the controller with suspended thread, and hot
 * method id. Since it does not need to go through analytic model, it does
 * not extend the VM_HotMethodEvent.
 */

public final class OSR_OnStackReplacementEvent implements VM_ControllerInputEvent {

  /* the suspended thread. */
  public VM_Thread suspendedThread;

  /* remember where it comes from */
  public int whereFrom;

  /* the compiled method id */
  public int CMID;

  /* the threadSwithFrom fp offset */
  public Offset tsFromFPoff;

  /* the osr method's fp offset */
  public Offset ypTakenFPoff;

  /**
   * This function will generate a controller plan and
   * inserted in the recompilation queue.
   */
  public void process() {

    VM_CompiledMethod compiledMethod = VM_CompiledMethods.getCompiledMethod(CMID);

    VM_NormalMethod todoMethod = (VM_NormalMethod) compiledMethod.getMethod();

    double priority;
    OPT_Options options;
    OPT_OptimizationPlanElement[] optimizationPlan;

    VM_ControllerPlan oldPlan = VM_ControllerMemory.findLatestPlan(todoMethod);

    if (oldPlan != null) {
      OPT_CompilationPlan oldCompPlan = oldPlan.getCompPlan();
      priority = oldPlan.getPriority();
      options = oldCompPlan.options;
      optimizationPlan = oldCompPlan.optimizationPlan;
    } else {
      priority = 5.0;
      options = (OPT_Options) VM_RuntimeCompiler.options;
      optimizationPlan = (OPT_OptimizationPlanElement[]) VM_RuntimeCompiler.optimizationPlan;
    }

    OPT_CompilationPlan compPlan = new OPT_CompilationPlan(todoMethod, optimizationPlan, null, options);

    OSR_OnStackReplacementPlan plan =
        new OSR_OnStackReplacementPlan(this.suspendedThread,
                                       compPlan,
                                       this.CMID,
                                       this.whereFrom,
                                       this.tsFromFPoff,
                                       this.ypTakenFPoff,
                                       priority);

    VM_Controller.compilationQueue.insert(priority, plan);

    VM_AOSLogging.logOsrEvent("OSR inserts compilation plan successfully!");

    // do not hold the reference anymore.
    suspendedThread = null;
    CMID = 0;
  }
}
