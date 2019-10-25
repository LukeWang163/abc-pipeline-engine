/**
 * Copyright (c) 2014-2018, RapidMiner GmbH, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 */
package base.operators.core.concurrency;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;


/**
 * Context providing methods for the parallel execution of computationally expensive tasks. Tasks
 * may be specified via {@link Runnable}s, {@link Callable}s, or {@link ForkJoinTask}s.
 * <p>
 * You should always use the {@code ConcurrencyContext} provided by the current execution
 * environment (e.g., via injection). More specifically, as user of the RapidMiner API, you should
 * never implement this interface on your own.
 * <p>
 * Despite managing multithreaded computations, {@code ConcurrencyContext} implementations
 * themselves need <strong>not</strong> be <strong>thread-safe</strong>. As a consequence, you
 * should never invoke a context's methods recursively, e.g., from within a submitted task. Consider
 * submitting an appropriate {@link ForkJoinTask} instead.
 *
 * @author Gisa Schaefer, Michael Knopf
 * @since 0.1
 */
public interface ConcurrencyContext {

	/**
	 * Executes the given {@link Runnable}s in parallel.
	 * <p>
	 * If one of the computations encounters an unchecked exception, the context attempts to cancel
	 * the other {@code Runnable}s and throws an {@link ExecutionException} that wraps the unchecked
	 * exception. If more than one {@code Runnable} encounter an exception, only the first observed
	 * exception is reported.
	 * <p>
	 * The method blocks until all {@code Runnable}s have completed.
	 * <p>
	 * Note that the cancellation of one of the {@code Runnable}s might not interrupt the execution
	 * of other {@code Runnable}s that have already been started. As a consequence, it is
	 * recommended that all long-running {@code Runnable}s periodically check for the cancellation
	 * of the computation via {@link #checkStatus()}.
	 *
	 * @param runnables
	 *            the {@code Runnable}s
	 * @throws ExecutionException
	 *             if one or more of the computations threw an unchecked exception
	 * @throws ExecutionStoppedException
	 *             if the execution was stopped before completion
	 * @throws IllegalArgumentException
	 *             if the given list of {@code Runnable}s is or contains {@code null}
	 * @since 0.1
	 */
	void run(List<Runnable> runnables) throws ExecutionException, ExecutionStoppedException, IllegalArgumentException;

	/**
	 * Executes the given {@link Callable}s in parallel, returning their results upon completion.
	 * <p>
	 * If one of the {@code Callable}s encounters an exception, the context attempts to cancel the
	 * other {@code Callable}s and throws an {@link ExecutionException} that wraps the unchecked
	 * exception. If more than one {@code Callable} encounters an exception, only the first observed
	 * exception is reported.
	 * <p>
	 * The method blocks until all {@code Callables} have completed.
	 * <p>
	 * Note that the cancellation of one of the {@code Callable}s might not interrupt the execution
	 * of other {@code Callable}s that have already been started. As a consequence, it is
	 * recommended that all long-running {@code Callable}s periodically check for the cancellation
	 * of the computation via {@link #checkStatus()}.
	 *
	 * @param <T>
	 *            the type of the values returned from the callables
	 * @param callables
	 *            the {@link Callable}s to execute in parallel
	 * @return a list containing the results of the callables
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws ExecutionStoppedException
	 *             if the execution was stopped before completion
	 * @throws IllegalArgumentException
	 *             if the given list of {@code Callable}s is or contains {@code null}
	 * @since 0.1
	 */
	<T> List<T> call(List<Callable<T>> callables)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException;

	/**
	 * Submits the given {@link Callable}s for execution in parallel, returning {@link Future}s
	 * which can be used to access their results later.
	 * <p>
	 * The method does <strong>not</strong> block.
	 * <p>
	 * Note that the cancellation of one of the {@code Callable}s might not interrupt the execution
	 * of other {@code Callable}s that have already been started. As a consequence, it is
	 * recommended that all long-running {@code Callable}s periodically check for the cancellation
	 * of the computation via {@link #checkStatus()}.
	 * <p>
	 * This is an advanced method which should only be used if you know what you are doing. Usage of
	 * {@link #call(List)} is recommended instead.
	 *
	 * @param <T>
	 *            the type of the values returned from the callables
	 * @param callables
	 *            the {@link Callable}s to execute in parallel
	 * @return a list containing futures for the results of the callables
	 * @throws IllegalArgumentException
	 *             if the given list of {@code Callable}s is or contains {@code null}
	 * @since 0.2.1
	 */
	public <T> List<Future<T>> submit(List<Callable<T>> callables) throws IllegalArgumentException;

	/**
	 * Waits for the results of the given {@link Future}s, returning their results upon completion.
	 * <p>
	 * The method blocks until all {@code Future}s have their results.
	 * <p>
	 * This is an advanced method which should only be used if you know what you are doing. Usage of
	 * {@link #call(List)} is recommended instead.
	 *
	 * @param <T>
	 *            the type of the values returned from the futures
	 * @param futures
	 *            the {@link Future}s for which their results should be collected
	 * @return a list containing the results of the futures
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws ExecutionStoppedException
	 *             if the execution was stopped before completion
	 * @throws IllegalArgumentException
	 *             if the given list of {@code futures}s is or contains {@code null}
	 * @since 0.2.1
	 */
	public <T> List<T> collectResults(List<Future<T>> futures)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException;

	/**
	 * Performs the given task, returning its result upon completion.
	 * <p>
	 * If the computation encounters an unchecked exception, the task is cancelled and an
	 * {@link ExecutionException} is thrown that wraps the unchecked exception.
	 * <p>
	 * The method blocks until the <em>given task and all recursively invoked subtasks</em> have
	 * completed.
	 * <p>
	 * Note that a cancellation of the provided task might not interrupt the execution of subtasks
	 * (e.g., spawned via {@link ForkJoinTask#fork()}) that have already been started. As a
	 * consequence, it is recommended that all long-running subtasks periodically check for the
	 * cancellation of the computation via {@link #checkStatus()}.
	 *
	 * @param <T>
	 *            the type of the task's result
	 * @param task
	 *            the task
	 * @return the task's result
	 * @throws ExecutionException
	 *             if the computation threw an unchecked exception
	 * @throws ExecutionStoppedException
	 *             if the execution was stopped before completion
	 * @throws IllegalArgumentException
	 *             if the given {@code ForkJoinTask} is {@code null}
	 * @since 0.1
	 */
	<T> T invoke(ForkJoinTask<T> task) throws ExecutionException, ExecutionStoppedException, IllegalArgumentException;

	/**
	 * Performs the given tasks in parallel, returning their results upon completion.
	 * <p>
	 * If one of the computations encounters an unchecked exception, all tasks are cancelled and an
	 * {@link ExecutionException} is thrown that wraps the unchecked exception. If more than one
	 * task encounter an exception, only the first observed exception is reported.
	 * <p>
	 * The method blocks until the <em>given tasks and all recursively invoked</em> subtasks have
	 * completed.
	 * <p>
	 * Note that a cancellation of the provided tasks might not interrupt the execution of subtasks
	 * (e.g., spawned via {@link ForkJoinTask#fork()}) that have already been started. As a
	 * consequence, it is recommended that all long-running subtasks periodically check for the
	 * cancellation of the computation via {@link #checkStatus()}.
	 *
	 * @param <T>
	 *            the type of the tasks' results
	 * @param tasks
	 *            the tasks
	 * @return the tasks' results
	 * @throws ExecutionException
	 *             if one or more of the computations threw an unchecked exception
	 * @throws ExecutionStoppedException
	 *             if the execution was stopped before completion
	 * @throws IllegalArgumentException
	 *             if the given list of {@code ForkJoinTask}s is or contains {@code null}
	 * @since 0.1
	 */
	<T> List<T> invokeAll(List<ForkJoinTask<T>> tasks)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException;

	/**
	 * Checks whether the current computation should be continued.
	 * <p>
	 * This utility method polls the status of the {@code ConcurrencyContext}. If the status
	 * indicates that the current computation should be aborted, an
	 * {@link ExecutionStoppedException} is thrown, otherwise the method returns immediately.
	 * <p>
	 * Long-running tasks should call this method periodically. Refraining to do so might cause the
	 * task to delay the handling of events such as a cancellation by the user. It is sufficient to
	 * call this method about once per second. On the contrary, invoking this method too often might
	 * slow down the computation.
	 * <p>
	 * Note that you <strong>must not consume</strong> the <strong>exceptions</strong> thrown by
	 * this method. If you need to catch the exception, e.g., to free resources, ensure that the
	 * original exception is rethrown.
	 *
	 * @throws ExecutionStoppedException
	 *             if the context requests the computation to stop
	 * @since 0.1
	 */
	void checkStatus() throws ExecutionStoppedException;

	/**
	 * Returns the targeted parallelism level of this context.
	 * <p>
	 * The targeted parallelism level provides an <em>upper bound</em> for the number of tasks that
	 * will be executed in parallel by this context. It does not guarantee that this bound will be
	 * matched during execution. Note that the targeted parallelism level need not match the number
	 * of processors available to the Java {@link Runtime}.
	 * <p>
	 * You can use the targeted parallelism level as an indicator for partitioning your computation.
	 * For instance, a parallelism level of {@code 8} indicates that should partition your task into
	 * <em>at least</em> eight tasks to fully utilize the context. A parallelism level of {@code 1}
	 * indicates a single threaded execution of all submitted tasks.
	 *
	 * @return the targeted parallelism level of this context
	 * @since 0.1
	 */
	int getParallelism();

}
