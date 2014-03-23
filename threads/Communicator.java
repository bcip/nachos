//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();

		speakerCondition = new Condition2(lock);
		listenerCondition = new Condition2(lock);
		returnCondition = new Condition2(lock);

		AS = WS = AL = WL = 0;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();

		while (AS != 0) {
			WS++;
			speakerCondition.sleep();
			WS--;
		}

		AS++;

		this.word = word;

		if (AL != 0)
			returnCondition.wake();
		else {
			if (WL != 0)
				listenerCondition.wake();

			returnCondition.sleep();

			AS--;
			AL--;

			if (WS != 0)
				speakerCondition.wake();
		}

		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lock.acquire();

		while (AL != 0) {
			listenerCondition.sleep();

			WL--;
		}

		AL++;

		if (AS != 0)
			returnCondition.wake();
		else {
			if (WS != 0)
				speakerCondition.wake();

			returnCondition.sleep();

			AL--;
			AS--;

			if (WL != 0)
				listenerCondition.wake();
		}
		int word = this.word;

		lock.release();

		return word;
	}

	Lock lock;
	Condition2 speakerCondition, listenerCondition, returnCondition;
	int AS, WS, AL, WL;
	int word;
}
