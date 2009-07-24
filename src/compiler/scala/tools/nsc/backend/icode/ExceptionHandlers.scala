/* NSC -- new scala compiler
 * Copyright 2005 LAMP/EPFL
 * @author  Martin Odersky
 */

// $Id$

package scala.tools.nsc
package backend
package icode;

import scala.collection._

/**
 * Exception handlers are pieces of code that `handle' exceptions on
 * the covered basic blocks. Since Scala's exception handling uses
 * pattern matching instead of just class names to identify handlers,
 * all our handlers will catch `Throwable' and rely on proper ordering
 * in the generated code to preserve nesting.
 */
trait ExceptionHandlers { self: ICodes =>
  import global.{Symbol, NoSymbol};

  class ExceptionHandler(val method: IMethod, val label: String, val cls: Symbol) {
    private var _startBlock: BasicBlock = _;
    var finalizer: Finalizer = _;

    /** Needed for the MSIL backend. */
    var resultKind: TypeKind = _;

    def setStartBlock(b: BasicBlock) = {
      _startBlock = b;
      b.exceptionHandlerHeader = true
    }
    def startBlock = _startBlock;

    /** The list of blocks that are covered by this exception handler */
    var covered: immutable.Set[BasicBlock] = immutable.HashSet.empty[BasicBlock]

    def addCoveredBlock(b: BasicBlock): ExceptionHandler = {
      covered = covered + b
      this
    }

    /** Is `b' covered by this exception handler? */
    def covers(b: BasicBlock): Boolean = covered(b);

    /** The body of this exception handler. May contain 'dead' blocks (which will not
      * make it into generated code because linearizers may not include them) */
    var blocks: List[BasicBlock] = Nil;

    def addBlock(b: BasicBlock): Unit = blocks = b :: blocks;

    override def toString() = "exh_" + label + "(" + cls.simpleName + ")";

    /** A standard copy constructor */
    def this(other: ExceptionHandler) = {
      this(other.method, other.label, other.cls);
      covered    = other.covered;
      setStartBlock(other.startBlock);
      finalizer  = other.finalizer;
    }

    def dup: ExceptionHandler = new ExceptionHandler(this);
  }

  class Finalizer(method: IMethod, label: String) extends ExceptionHandler(method, label, NoSymbol) {
    override def toString() = "finalizer_" + label;

    override def dup: Finalizer = new Finalizer(method, label);
  }

  object NoFinalizer extends Finalizer(null, "<no finalizer>") {
    override def startBlock: BasicBlock = error("NoFinalizer cannot have a start block.");
    override def setStartBlock(b: BasicBlock): Unit = error("NoFinalizer cannot have a start block.");
  }
}
