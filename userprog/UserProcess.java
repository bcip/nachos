//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.LinkedList;

import java.io.EOFException;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		boolean status = Machine.interrupt().disable();
		processId = (processNumber++);
		fileList = new FileDescriptor[16];
		if (parentProcess == null) {
			stdin = UserKernel.console.openForReading();
			stdout = UserKernel.console.openForWriting();
		} else {
			stdin = parentProcess.stdin;
			stdout = parentProcess.stdout;
		}
		fileList[0] = new FileDescriptor(null, stdin);
		fileList[1] = new FileDescriptor(null, stdout);
		childList = new LinkedList<UserProcess>();
		exitMap = new HashMap<Integer, Integer>();
		exitMapLock = new Lock();
		Machine.interrupt().restore(status);
		stdLock = new Lock();
		// int numPhysPages = Machine.processor().getNumPhysPages();
		// pageTable = new TranslationEntry[numPhysPages];
		// for (int i = 0; i < numPhysPages; i++)
		// pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return new UserProcess();
		// return (UserProcess)
		// Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		/*
		 * int numSuccessedBytes = 0; while (numSuccessedBytes < length) { int
		 * paddr = translate(vaddr, false); if (paddr < 0) break; data[offset] =
		 * memory[paddr]; offset++; vaddr++; numSuccessedBytes++; }
		 * 
		 * return numSuccessedBytes;
		 */

		if (vaddr < 0 || vaddr >= numPages * pageSize)
			return 0;
		if (vaddr + length >= numPages * pageSize)
			length = numPages * pageSize - vaddr;

		int firstPage = Machine.processor().pageFromAddress(vaddr);
		int lastPage = Machine.processor().pageFromAddress(vaddr + length - 1);
		int bytesTransferred = 0;

		for (int i = firstPage; i <= lastPage; i++) {
			int start = Math.max(i * pageSize, vaddr);
			int end = Math.min((i + 1) * pageSize, vaddr + length);

			int firstPhyAddress = translate(start, false);
			if (firstPhyAddress < 0)
				break;
			System.arraycopy(memory, firstPhyAddress, data, offset
					+ bytesTransferred, end - start);
			bytesTransferred += (end - start);
			pageTable[i].used = true;
		}
		return bytesTransferred;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		if (vaddr < 0 || vaddr >= numPages * pageSize)
			return 0;
		if (vaddr + length >= numPages * pageSize)
			length = numPages * pageSize - vaddr;

		int bytesTransferred = 0;
		int firstPage = Machine.processor().pageFromAddress(vaddr);
		int lastPage = Machine.processor().pageFromAddress(vaddr + length - 1);

		for (int i = firstPage; i <= lastPage; i++) {
			int start = Math.max(i * pageSize, vaddr);
			int end = Math.min((i + 1) * pageSize, vaddr + length);

			int firstPhyAddress = translate(start, true);
			if (firstPhyAddress < 0)
				break;
			System.arraycopy(data, offset + bytesTransferred, memory,
					firstPhyAddress, end - start);
			bytesTransferred += (end - start);
			pageTable[i].used = true;
			pageTable[i].dirty = true;
		}
		return bytesTransferred;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	private int translate(int vaddr, boolean write) {
		int vpn = vaddr / pageSize;
		int offset = vaddr - vpn * pageSize;

		if (vpn < 0 || vpn >= numPages)
			return -1;

		TranslationEntry translationEntry = pageTable[vpn];

		if (!translationEntry.valid || write && translationEntry.readOnly)
			return -1;

		return translationEntry.ppn * pageSize + offset;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		UserKernel.pageLock.acquire();

		if (numPages > UserKernel.availablePages.size()) {
			coff.close();
			UserKernel.pageLock.release();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		pageTable = new TranslationEntry[numPages];
		for (int i = 0; i < numPages; i++) {
			int nextPage = UserKernel.availablePages.poll();
			pageTable[i] = new TranslationEntry(i, nextPage, true, false,
					false, false);
		}

		UserKernel.pageLock.release();

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				/*
				 * // allocate phisical page pageTable[vpn] = new
				 * TranslationEntry(vpn, UserKernel.nextAvailablePage(), true,
				 * section.isReadOnly(), false, false);
				 */

				pageTable[vpn].readOnly = section.isReadOnly();

				section.loadPage(i, pageTable[vpn].ppn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {

		/*
		 * for (int vpn = 0; vpn < numPages; vpn++) {
		 * UserKernel.freePage(pageTable[vpn].ppn);
		 */

		UserKernel.pageLock.acquire();

		for (int i = 0; i < numPages; i++) {
			UserKernel.availablePages.add(pageTable[i].ppn);
		}

		UserKernel.pageLock.release();

		for (int i = 0; i < 16; i++) {
			if (fileList[i] != null) {
				handleClose(i);
			}

		}
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		if (processId != 0)
			return -1;
		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private int handleCreate(int address) {
		if (address < 0)
			return -1;
		String fileName = readVirtualMemoryString(address, 256);
		if (fileName == null)
			return -1;
		int empty = 0;
		for (; empty < 16; empty++) {
			if (fileList[empty] == null) {
				break;
			}
		}
		if (empty == 16) {
			return -1;
		}
		OpenFile newFile = ThreadedKernel.fileSystem.open(fileName, true);
		if (!UserKernel.createFile(fileName)) {
			return -1;
		}
		if (newFile == null) {
			return -1;
		} else {
			fileList[empty] = new FileDescriptor(fileName, newFile);
			return empty;
		}
	}

	public int handleOpen(int address) {
		if (address < 0)
			return -1;
		String fileName = readVirtualMemoryString(address, 256);
		if (fileName == null)
			return -1;
		int empty = 0;
		for (; empty < 16; empty++) {
			if (fileList[empty] == null) {
				break;
			}
		}
		if (empty == 16) {
			return -1;
		}
		OpenFile newFile = ThreadedKernel.fileSystem.open(fileName, false);
		if (!UserKernel.openFile(fileName)) {
			return -1;
		}
		if (newFile == null) {
			return -1;
		} else {
			fileList[empty] = new FileDescriptor(fileName, newFile);
			return empty;
		}
	}

	public int handleRead(int index, int address, int bufsize) {
		if (index < 0 || index >= 16 || fileList[index] == null || address < 0
				|| bufsize < 0) {
			return -1;
		}

		FileDescriptor tmp = fileList[index];
		int numBytesWrited = 0;

		while (bufsize > 0) {
			byte[] buffer = new byte[Math.min(bufsize, maxBufSise)];
			bufsize -= buffer.length;
			int numBytesRead = tmp.file.read(buffer, 0, buffer.length);

			if (numBytesRead < 0) {
				return -1;
			} else {
				int numBytesNewlyWrited = writeVirtualMemory(address, buffer,
						0, numBytesRead);
				if (numBytesNewlyWrited < numBytesRead)
					return -1;
				numBytesWrited += numBytesRead;
				address += numBytesRead;
				if (numBytesRead < buffer.length)
					break;
			}
		}
		return numBytesWrited;
	}

	public int handleWrite(int index, int address, int bufsize) {
		if (index < 0 || index >= 16 || fileList[index] == null || address < 0
				|| bufsize < 0) {
			return -1;
		}

		FileDescriptor tmp = fileList[index];
		int numBytesWrited = 0;

		while (bufsize > 0) {
			byte[] buffer = new byte[Math.min(bufsize, maxBufSise)];
			bufsize -= buffer.length;
			int numBytesRead = readVirtualMemory(address, buffer);

			if (numBytesRead < buffer.length) {
				return -1;
			} else {
				stdLock.acquire();
				int numBytesNewlyWrited = tmp.file.write(buffer, 0,
						buffer.length);
				stdLock.release();
				numBytesWrited += numBytesRead;
				address += numBytesRead;
				if (numBytesNewlyWrited < numBytesRead)
					break;
			}

		}

		return numBytesWrited;
	}

	public int handleClose(int index) {
		if (index < 0 || index >= 16 || fileList[index] == null) {
			return -1;
		}

		FileDescriptor tmp = fileList[index];
		String filename = tmp.filename;
		tmp.file.close();
		fileList[index] = null;

		if (UserKernel.closeFile(filename)) {
			return 0;
		}

		return -1;
	}

	public int handleUnlink(int address) {
		if (address < 0) {
			return -1;
		}

		String fileName = readVirtualMemoryString(address, 256);
		if (fileName == null)
			return -1;

		if (UserKernel.unlinkFile(fileName)) {
			return 0;
		}
		return -1;
	}

	private int handleExit(int status) {
		Machine.interrupt().disable();

		unloadSections();

		for (UserProcess child : childList) {
			child.parentProcess = null;
		}

		if (parentProcess != null) {
			parentProcess.exitMapLock.acquire();
			parentProcess.exitMap.put(processId, status);
			parentProcess.exitMapLock.release();
		}

		/*
		 * ListIterator<UserProcess> iter = childList.listIterator();
		 * 
		 * while (iter.hasNext()) { UserProcess child = iter.next();
		 * child.parentProcess = null; } exitMap.clear(); childList.clear();
		 */
		if (processId == 0) {
			Kernel.kernel.terminate();
		} else {
			UThread.finish();
		}

		Lib.assertNotReached();

		return -1;
	}

	private int handleJoin(int pid, int address) {
		UserProcess child = null;

		for (UserProcess curChild : childList) {
			if (curChild.processId == pid) {
				child = curChild;
				break;
			}

		}

		if (child == null) {
			return -1;
		}

		if (child.thread != null) {
			child.thread.join();
		}

		childList.remove(child);
		child.parentProcess = null;

		exitMapLock.acquire();
		if (!exitMap.containsKey(child.processId)) {
			return 0;
		}
		int exitstatus = exitMap.get(child.processId);
		exitMap.remove(child.processId);
		exitMapLock.release();

		if (exitstatus == unknowException || exitstatus == exceptionalExit) {
			return 0;
		}

		byte[] buffer = new byte[4];
		Lib.bytesFromInt(buffer, 0, exitstatus);
		int numOfBytesTransfer = writeVirtualMemory(address, buffer);
		if (numOfBytesTransfer == 4) {
			return 1;
		} else {
			return 0;
		}
	}

	private int handleExec(int address, int numofArgs, int argsOffset) {
		if (address < 0) {
			return -1;
		}

		String fileaddress = readVirtualMemoryString(address, 256);

		if (fileaddress == null) {
			return -2;
		}

		if (!fileaddress.endsWith("coff") && !fileaddress.endsWith("COFF")) {
			return -3;
		}

		if (numofArgs < 0) {
			return -4;
		}

		String[] arguments = new String[numofArgs];
		for (int i = 0; i < numofArgs; i++) {
			byte[] buffer = new byte[4];
			int numofBytes = readVirtualMemory(argsOffset + (i * 4), buffer);
			if (numofBytes != 4) {
				return -5;
			}

			int argAddr = Lib.bytesToInt(buffer, 0);
			String argument = readVirtualMemoryString(argAddr, 256);
			if (argument == null) {
				return -6;
			}
			arguments[i] = argument;
		}

		UserProcess child = UserProcess.newUserProcess();
		if (child.execute(fileaddress, arguments)) {
			childList.add(child);
			child.parentProcess = this;
			return child.processId;
		} else {
			return -7;
		}
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * public int pos = 0;
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case syscallExit:
			return handleExit(a0);
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			handleExit(unknowSystemCall);
			Lib.assertNotReached("Unknown system call!" + syscall);
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		case Processor.exceptionAddressError:
		case Processor.exceptionBusError:
		case Processor.exceptionIllegalInstruction:
		case Processor.exceptionOverflow:
		case Processor.exceptionPageFault:
		case Processor.exceptionReadOnly:
		case Processor.exceptionTLBMiss:
			handleExit(exceptionalExit);
			break;
		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			handleExit(unknowException);
			Lib.assertNotReached("Unexpected exception");
		}
	}
	
	public static void selfTest(){
		boolean pass = true;
		
		UserProcess test = new UserProcess();
		
		test.load("halt.coff", new String[]{});
		
		int vaddr = Processor.makeAddress(2,0) - 500;
		int wint = 0x012345678;
		int rint = 0;
		
		byte[] wbuffer = new byte[4];
		byte[] rbuffer = new byte[4];
		
		Lib.bytesFromInt(wbuffer,0,wint);
		test.writeVirtualMemory(vaddr, wbuffer);
		test.readVirtualMemory(vaddr, rbuffer);
		
		rint = Lib.bytesToInt(rbuffer, 0);
		
		if(rint != wint){
			pass = false;
			System.err.println("FAIL: Read/Write failed to virtual memory!");
		}
		
		byte[] memory = Machine.processor().getMemory();
		rbuffer[0] = memory[Processor.makeAddress(2,0)-500];
		rbuffer[1] = memory[Processor.makeAddress(2,0)-499];
		rbuffer[2] = memory[Processor.makeAddress(2,0)-498];
		rbuffer[3] = memory[Processor.makeAddress(2,0)-497];

		rint = Lib.bytesToInt(rbuffer, 0);
		if(rint!=wint){
			pass = false;
			System.err.println("FAIL: Read/Write performed on wrong physical memory!");
		}
		
		if(pass){
			System.out.println("success in task2");
		}
		test.unloadSections();
		processNumber = 0;
	}

	public class FileDescriptor {
		public String filename = null;
		public OpenFile file = null;

		public FileDescriptor(String filename, OpenFile file) {
			this.file = file;
			this.filename = filename;
		}
	}

	private FileDescriptor[] fileList;

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';

	private final int processId;
	private static int processNumber = 0;

	protected LinkedList<UserProcess> childList;

	// protected UserProcess parent;

	protected UThread thread;

	protected Map<Integer, Integer> exitMap;
	protected Lock exitMapLock;

	protected OpenFile stdin;
	protected OpenFile stdout;

	private UserProcess parentProcess;
	// private LinkedList<UserProcess> childProcesses;
	// private UThread thread;

	private static final int exceptionalExit = -611;
	private static final int unknowException = -612;
	private static final int unknowSystemCall = -613;

	private static final int maxBufSise = 1 << 20;

	private static Lock stdLock;
}
