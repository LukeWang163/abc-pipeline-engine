/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

/**
 */

package base.operators.operator.learner.maxent.optimize;

/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:
 * General exception thrown by optimization algorithms when there
 *  is an optimization-specific problem.  For example, an exception
 *  might be thrown when the gradient is sufficiently large but
 *  no step is possible in that direction.
 */
public class OptimizationException extends RuntimeException {

  public OptimizationException()
  {
    super ();
  }

  public OptimizationException(String message)
  {
    super (message);
  }

  public OptimizationException(String message, Throwable cause)
  {
    super (message, cause);
  }

  public OptimizationException(Throwable cause)
  {
    super (cause);
  }
}
