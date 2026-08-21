package com.seibel.distanthorizons.common.util.threading;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Queues work that must run on the Minecraft server thread. The platform's
 * server-tick callback is responsible for calling {@link #runTasks(long)}.
 */
public class ServerThreadTaskHandler
{
	public static final ServerThreadTaskHandler INSTANCE = new ServerThreadTaskHandler();

	private final ConcurrentLinkedQueue<QueuedTask<?>> taskQueue = new ConcurrentLinkedQueue<>();
	private volatile Thread serverThread;



	private ServerThreadTaskHandler() { }



	/**
	 * Queues a task for the server thread.
	 *
	 * @param timeLimited if true, the handler may defer later tasks after this
	 *                    task when the current tick's time budget is exhausted
	 */
	public <T> CompletableFuture<T> queueTask(boolean timeLimited, Supplier<T> task)
	{
		CompletableFuture<T> future = new CompletableFuture<>();
		this.taskQueue.add(new QueuedTask<>(timeLimited, task, future));
		return future;
	}

	/**
	 * Runs queued tasks on the calling server thread. At least one queued task
	 * is run, even when that task alone exceeds the supplied budget.
	 */
	public void runTasks(long maxRunTimeNano)
	{
		if (this.serverThread == null)
		{
			this.serverThread = Thread.currentThread();
		}

		long deadlineNano = System.nanoTime() + maxRunTimeNano;
		QueuedTask<?> queuedTask;
		while ((queuedTask = this.taskQueue.poll()) != null)
		{
			queuedTask.run();
			if (queuedTask.timeLimited && System.nanoTime() >= deadlineNano)
			{
				break;
			}
		}
	}

	/** Completes queued tasks exceptionally without running them. */
	public void cancelPendingTasks()
	{
		QueuedTask<?> queuedTask;
		while ((queuedTask = this.taskQueue.poll()) != null)
		{
			queuedTask.future.completeExceptionally(
				new CancellationException("The Minecraft server stopped before the queued task could run."));
		}
		this.serverThread = null;
	}

	public boolean isCurrentThread()
	{
		return this.serverThread != null && Thread.currentThread() == this.serverThread;
	}

	public int getQueueSize() { return this.taskQueue.size(); }



	private static class QueuedTask<T>
	{
		private final boolean timeLimited;
		private final Supplier<T> task;
		private final CompletableFuture<T> future;

		private QueuedTask(boolean timeLimited, Supplier<T> task, CompletableFuture<T> future)
		{
			this.timeLimited = timeLimited;
			this.task = task;
			this.future = future;
		}

		private void run()
		{
			try
			{
				this.future.complete(this.task.get());
			}
			catch (Throwable throwable)
			{
				this.future.completeExceptionally(throwable);
			}
		}
	}
}
