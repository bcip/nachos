//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}

	/**
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer tickets from
	 *            waiting threads to the owning thread.
	 * @return a new lottery thread queue.
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
	public static final int priorityMaximum = Integer.MAX_VALUE;

	protected class PriorityQueue extends PriorityScheduler.PriorityQueue {
		PriorityQueue(boolean transferPriority) {
			super(transferPriority);
		}

		protected PriorityScheduler.ThreadState pickNextThread() {
			if (waitingQueue.isEmpty())
				return null;

			int sum = 0;
			for (PriorityScheduler.ThreadState thread : waitingQueue)
				sum += thread.getEffectivePriority();
			sum = (int) Math.floor(Math.random() * sum);
			for (PriorityScheduler.ThreadState thread : waitingQueue) {
				sum -= thread.getEffectivePriority();
				if (sum < 0)
					return thread;
			}
			return waitingQueue.first();
		}

		protected void updateDonatingPriority() {
			int newDonatingPriority = priorityMinimum;

			if (waitingQueue.isEmpty())
				newDonatingPriority = priorityMinimum;
			else if (transferPriority)
				for (PriorityScheduler.ThreadState thread : waitingQueue)
					newDonatingPriority += thread.getEffectivePriority();
			else
				newDonatingPriority = priorityMinimum;

			if (newDonatingPriority == donatingPriority)
				return;

			if (occupyingThread != null)
				getThreadState(occupyingThread)
						.prepareToUpdateDonatingPriority(this);

			donatingPriority = newDonatingPriority;

			if (occupyingThread != null)
				getThreadState(occupyingThread).updateDonatingPriority(this);
		}
	}

	protected class ThreadState extends PriorityScheduler.ThreadState {
		public ThreadState(KThread thread) {
			super(thread);
		}

		protected void updateEffectivePriority() {
			int newEffectivePriority = priority;
			if (!acquires.isEmpty())
				for (PriorityScheduler.PriorityQueue queue : acquires)
					newEffectivePriority += queue.getDonatingPriority();

			if (newEffectivePriority == effectivePriority)
				return;

			if (waitingFor != null)
				waitingFor.prepareToUpdateEffectivePriority(thread);

			effectivePriority = newEffectivePriority;

			if (waitingFor != null)
				waitingFor.updateEffectivePriority(thread);
		}
	}
}
