//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.userprog;

import java.util.HashMap;
import java.util.Map;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		console = new SynchConsole(Machine.console());

		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		super.selfTest();

		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");

		char c;

		do {
			c = (char) console.readByte(true);
			console.writeByte(c);
		} while (c != 'q');

		System.out.println("");
	}

	/**
	 * Returns the current process.
	 * 
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 * 
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 * 
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();

		UserProcess process = UserProcess.newUserProcess();

		String shellProgram = Machine.getShellProgramName();
		Lib.assertTrue(process.execute(shellProgram, new String[] {}));

		KThread.currentThread().finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;

	// dummy variables to make javac smarter
	private static Coff dummy1 = null;
	
	public static class FileManager{
		public int count = 1;
		public boolean unlink = false;
	}
	
	private static HashMap<String, FileManager> fileManager = new HashMap<String, FileManager>();
	
	private static Lock fileLock = new Lock();
	
	public static boolean createFile(String filename){
		fileLock.acquire();
		if(!fileManager.containsKey(filename)){
			fileManager.put(filename, new FileManager());
			fileLock.release();
			return true;
		}else{
			fileLock.release();
			return false;
		}
	}
	
	public static boolean openFile(String filename){
		fileLock.acquire();
		FileManager tmp = fileManager.get(filename);
		if(tmp != null){
			tmp.count++;
			fileLock.release();
			return true;
		}
		fileLock.release();
		return false;
	}
	
	public static boolean closeFile(String filename){
		fileLock.acquire();
		FileManager tmp = fileManager.get(filename);
		if(tmp != null){
			if(tmp.count > 0){
				tmp.count --;
			}
			if(tmp.unlink && tmp.count == 0){
				fileSystem.remove(filename);
				fileManager.remove(filename);
			}
			fileLock.release();
			return true;
		}
		fileLock.release();
		return false;
	}
	
	public static boolean unlinkFile(String filename){
		fileLock.acquire();
		FileManager tmp = fileManager.get(filename);
		if(tmp == null){
			fileLock.release();
			return false;
		}
		
		if(tmp.count == 0){
			fileSystem.remove(filename);
			fileManager.remove(filename);
		}
		else{
			tmp.unlink = true;
		}
		fileLock.release();
		return true;
	}
}
