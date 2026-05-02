package com.unascribed.sup.agent.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParserException;
import com.unascribed.sup.agent.data.HashFunction;
import com.unascribed.sup.data.Version;

public class RawHandler extends AbstractFormatHandler {

	public RawHandler(URI src, boolean autoaccept, boolean forceFlavorDefaults, JsonObject baseState) {
		super(src, autoaccept, forceFlavorDefaults, baseState);
	}

	@Override
	public CheckResult check() throws IOException, URISyntaxException, JsonParserException {
		var fplan = new FilePlan();
		fplan.url = src;
		var frag = src.getFragment();
		fplan.state = frag == null ? FileState.MAGIC : new FileState(HashFunction.byName(frag.substring(0, frag.indexOf(':'))), frag.substring(frag.indexOf(':')+1), -1);
		var plan = new UpdatePlan<>(false, baseState);
		var path = src.getPath();
		plan.files.put(path.substring(path.lastIndexOf('/')+1), fplan);
		return new CheckResult(new Version("debug", 0), new Version("debug", 1), plan, Collections.emptyMap());
	}

}
