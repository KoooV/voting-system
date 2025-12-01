package com.kov.votingsystem.exception;

public class DuplicateVoteException extends Exception {
	public DuplicateVoteException(String message) {
		super(message);
	}
}
