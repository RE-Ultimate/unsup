package com.unascribed.sup.agent.pieces;

import java.util.ResourceBundle;

import javahttpclient.internal.common.Logger;

public class DummyLogger implements Logger {

	private final String name;
	
	public DummyLogger(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isLoggable(Level level) {
		return false;
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {

	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {

	}

}
