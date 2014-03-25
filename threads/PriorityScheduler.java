//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this, enqueueTimeCounter++);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());

			if (occupyingThread != null)
				getThreadState(occupyingThread).release(this);

			if (thread == null)
				return;

			getThreadState(thread).acquire(this);
			occupyingThread = thread;
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			ThreadState nextThread = pickNextThread();

			acquire(nextThread.getThread());
			
			if(nextThread != null)
				updateEffectivePriority(nextThread.getThread());
			
			return nextThread.getThread();
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			if(!waitingQueue.isEmpty())
				return getThreadState(waitingQueue.first());
			
			return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		public int getDonatingPriority() {
			return donatingPriority;
		}

		public int compareTo(Object o) {
			PriorityQueue queue = (PriorityQueue) o;

			if (donatingPriority > queue.donatingPriority)
				return -1;
			if (donatingPriority < queue.donatingPriority)
				return 1;

			if (id < queue.id)
				return -1;
			if (id > queue.id)
				return 1;

			return 0;
		}

		public void prepareToUpdateEffectivePriority(KThread thread) {
			boolean success = waitingQueue.remove(thread);

			Lib.assertTrue(success);
		}

		public void updateEffectivePriority(KThread thread) {
			waitingQueue.add(thread);

			updateDonatingPriority();
		}

		protected void updateDonatingPriority() {
			int newDonatingPriority;

			if (waitingQueue.isEmpty())
				newDonatingPriority = priorityMinimum;
			else if (transferPriority)
				newDonatingPriority = getThreadState(waitingQueue.first())
						.getEffectivePriority();
			else
				newDonatingPriority = priorityMinimum;

			if (newDonatingPriority == donatingPriority)
				return;

			Lib.assertTrue(occupyingThread != null);

			getThreadState(occupyingThread).prepareToUpdateDonatingPriority(
					this);

			donatingPriority = newDonatingPriority;

			getThreadState(occupyingThread).updateDonatingPriority(this);
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;

		/** The threads waiting in this ThreadQueue. */
		protected TreeSet<KThread> waitingQueue = new TreeSet<KThread>();

		/** The thread occupying this ThreadQueue. */
		protected KThread occupyingThread;

		protected int donatingPriority;

		/**
		 * The number that <tt>waitForAccess</tt> has been called. Used know the
		 * time when each thread enqueue.
		 */
		protected long enqueueTimeCounter;

		protected int id = numPriorityQueueCreated++;
	}

	protected static int numPriorityQueueCreated = 0;

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
		}
		
		public KThread getThread(){
			return thread;
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			return effectivePriority;
		}

		/**
		 * Return the time when the associated thread begin to wait.
		 */
		public long getEnqueueTime() {
			return enqueueTime;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			updateEffectivePriority();
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @param enqueueTime
		 *            the time when the thread begin to wait.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue, long enqueueTime) {
			this.enqueueTime = enqueueTime;

			waitingFor = waitQueue;

			waitQueue.updateEffectivePriority(thread);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			acquires.add(waitQueue);

			updateEffectivePriority();
		}

		/**
		 * Called when <tt>waitQueue</tt> no longer be acquired by the
		 * associated thread.
		 * 
		 * @param waitQueue
		 *            the queue
		 */
		public void release(PriorityQueue waitQueue) {
			acquires.remove(waitQueue);

			updateEffectivePriority();
		}

		public int compareTo(Object o) {
			ThreadState state = (ThreadState) o;

			if (effectivePriority > state.effectivePriority)
				return -1;
			if (effectivePriority < state.effectivePriority)
				return 1;

			if (enqueueTime < state.enqueueTime)
				return -1;
			if (enqueueTime > state.enqueueTime)
				return 1;

			return thread.compareTo(state.thread);
		}

		/**
		 * Remove <tt>waitQueue</tt> from <tt>acquires</tt> to prepare to update
		 * <tt>donatingPriority</tt> of <tt>waitQueue</tt>.
		 * 
		 * @param waitQueue
		 */
		public void prepareToUpdateDonatingPriority(PriorityQueue waitQueue) {
			boolean success = acquires.remove(waitQueue);

			Lib.assertTrue(success);
		}

		public void updateDonatingPriority(PriorityQueue waitQueue) {
			acquires.add(waitQueue);

			updateEffectivePriority();
		}

		private void updateEffectivePriority() {
			int newEffectivePriority = Math.max(priority, maxDonatedPriority);
			if (newEffectivePriority == effectivePriority)
				return;

			effectivePriority = newEffectivePriority;

			if (waitingFor == null)
				return;

			waitingFor.updateEffectivePriority(thread);
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority = priorityDefault;
		/** The effective priority of the associated thread. */
		protected int effectivePriority = priorityDefault;
		/** The maximum of priorities donated to the associated thread. */
		protected int maxDonatedPriority = priorityMinimum;
		/** The ThreadQueue that the associated thread waiting for. */
		protected PriorityQueue waitingFor = null;
		/** The TreeMap storing the number of donated priorities. */
		protected TreeSet<PriorityQueue> acquires = new TreeSet<PriorityQueue>();
		/**
		 * The time when the thread begin to wait. That time is measured by
		 * counting how many times <tt>PriorityQueue.waitForAccess</tt> called
		 * before.
		 */
		protected long enqueueTime;
	}
}
