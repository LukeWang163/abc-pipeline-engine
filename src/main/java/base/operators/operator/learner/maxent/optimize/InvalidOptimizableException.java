/* Copyright (C) 2006 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://mallet.cs.umass.edu/
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package base.operators.operator.learner.maxent.optimize;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 * Exception thrown by optimization algorithms, when the problem is usually
 *  due to a problem with the given Maximizable instance.
 * <p>
 * If the optimizer throws this in your code, usually there are two possible
 *  causes: (a) you are computing the gradients approximately, (b) your value
 *  function and gradient do not match (this can be checking using TestMaximizable.
 *
 */
public class InvalidOptimizableException extends OptimizationException {

  public InvalidOptimizableException()
  {
  }

  public InvalidOptimizableException(String message)
  {
    super (message);
  }

  public InvalidOptimizableException(String message, Throwable cause)
  {
    super (message, cause);
  }

  public InvalidOptimizableException(Throwable cause)
  {
    super (cause);
  }
}
