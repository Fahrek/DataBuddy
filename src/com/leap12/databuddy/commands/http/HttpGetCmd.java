package com.leap12.databuddy.commands.http;

import com.leap12.common.http.HttpRequest;

/**
 * <pre>
 * Expects Query Param:
 * context=[topic context]
 * </pre>
 */
public abstract class HttpGetCmd extends HttpContextualCmd {

	@Override
	protected float computeRelevance( HttpRequest request ) throws Exception {
		if ( request.getMethod().isGet() ) {
			return 1;
		}
		return 0;
	}

}
