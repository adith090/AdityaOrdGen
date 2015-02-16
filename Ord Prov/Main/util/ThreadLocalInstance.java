package com.m1.bcc.spl.util;

import com.m1.bcc.spl.model.CommandTransDetails;

/*******************************************************************************
 * MODIFICATION HISTORY
 *******************************************************************************
 * DATE MODIFIED(dd/mm/yyyy)	MODIFIED BY				COMMENTS
 *******************************************************************************
 * 20/11/2013					Ravikumar G				Bug#20538 - Created to have thread local copy of instance variable for multithreading
 ******************************************************************************/

public class ThreadLocalInstance {
	private static final ThreadLocal<CommandTransDetails> threadLocal = new ThreadLocal<CommandTransDetails>();
	
	public static void set(CommandTransDetails commandTransDetails) {
		threadLocal.set(commandTransDetails);
	}
	
	public static void remove() {
		threadLocal.remove();
	}
	
	public static CommandTransDetails get() {
		return threadLocal.get();
	}
}
