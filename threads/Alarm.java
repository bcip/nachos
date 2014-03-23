//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatue = Machine.interrupt().disable();

		WaitThread nextThread;

		while ((nextThread = waitQueue.peek()) != null
				&& nextThread.wakeTime() <= Machine.timer().getTime()) {
			waitQueue.poll().thread().ready();
		}

		Machine.interrupt().restore(intStatue);

		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x
	 *            the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		long wakeTime = Machine.timer().getTime() + x;

		boolean intStatue = Machine.interrupt().disable();

		waitQueue.add(new WaitThread(wakeTime, KThread.currentThread()));

		KThread.sleep();

		Machine.interrupt().restore(intStatue);
	}

	private class WaitThread {
		WaitThread(long wakeTime, KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());

			this.wakeTime = wakeTime;

			this.thread = thread;
		}

		public int compareTo(Object o) {
			WaitThread waitThread = (WaitThread) o;

			if (wakeTime < waitThread.wakeTime)
				return -1;
			else if (wakeTime > waitThread.wakeTime)
				return 1;
			else
				return thread.compareTo(waitThread.thread);
		}

		public long wakeTime() {
			return wakeTime;
		}

		public KThread thread() {
			return thread;
		}

		long wakeTime;
		KThread thread;
	}

	java.util.PriorityQueue<WaitThread> waitQueue = new java.util.PriorityQueue<WaitThread>();
}
