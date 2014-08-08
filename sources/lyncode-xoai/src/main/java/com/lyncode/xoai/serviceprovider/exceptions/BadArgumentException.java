package com.lyncode.xoai.serviceprovider.exceptions;

public class BadArgumentException extends HarvestException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3852801418680298861L;

	public BadArgumentException() {
	}

	public BadArgumentException(String arg0) {
		super(arg0);
	}

	public BadArgumentException(Throwable arg0) {
		super(arg0);
	}

	public BadArgumentException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
