package com.unascribed.sup.agent.signing;

public abstract class AbstractSigProvider implements SigProvider {
	
	private final String origin;

	public AbstractSigProvider(String origin) {
		this.origin = origin;
	}
	
	@Override
	public String origin() {
		return origin;
	}

}
