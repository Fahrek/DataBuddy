package com.leap12.databuddy.connections;

import com.leap12.common.ClientConnection;
import com.leap12.common.HttpRequest;
import com.leap12.common.Log;
import com.leap12.databuddy.BaseConnection;
import com.leap12.databuddy.Commands;
import com.leap12.databuddy.Commands.CmdResponse;
import com.leap12.databuddy.Commands.RequestStatus;
import com.leap12.databuddy.Commands.Role;
import com.leap12.databuddy.connections.handler.HttpHandler;

public class HandshakeConnection extends BaseConnection {

	@Override
	protected void onAttached( ClientConnection connection ) throws Exception {
		connection.setInactivityTimeout( 10000 );
		connection.setKeepAlive( false ); // we don't know the client protocol yet, could be HTTP or GAME
	}

	@Override
	protected void onReceivedMsg( String msg ) throws Exception {
		logDebugMessageWithNewlineChars( msg );

		// If we are a proper auth command, then deal with it
		if ( Commands.CMD_AUTH.isCommand( msg ) ) {
			getClientConnection().setKeepAlive( true );
			try {
				UserConnection connection = handleAuthenticateUser( msg );
				getClientConnection().setDelegate( connection );
			} catch ( Exception e ) {
				Log.e( e );
				writeResponse( e.getMessage() );
				getClientConnection().stop();
			}
		}

		// Apparently we didn't get an auth cmd, if its a HTTP request, lets try to support it just for fun
		else if ( msg.contains( "HTTP" ) ) { // FIXME: unsafe -- first, lets make sure its an HTTP request before we try to parse one.
			HttpRequest request = new HttpRequest( msg );
			getClientConnection().setKeepAlive( false );
			HttpHandler handler = new HttpHandler( this, request );
			handler.handleRequest();
		}
	}

	/**
	 * expects:
	 *
	 * <pre>
	 * auth request_auth=user&username=theUsername&password=thePassword
	 * or
	 * auth request_auth=sysop&username=theUsername&password=thePassword
	 * </pre>
	 *
	 * @param msg
	 * @return Appropriate connection;
	 */
	private UserConnection handleAuthenticateUser( String msg ) throws Exception {
		CmdResponse<Role> request = Commands.CMD_AUTH.executeCommand( this, msg );
		if ( RequestStatus.SUCCESS == request.getStatus() ) {

			// TODO validate user -- maybe send them to the appropriate connection and let that connection do the validation? This would better
			// support an anonymous type

			return toConnection( request );
		}
		throw new Exception( request.getError() );
	}

	private UserConnection toConnection( CmdResponse<Role> request ) {
		Role role = request.getValue();
		switch ( role ) {
		case sysop:
			return new SysOpConnection();
		case user:
			return new UserConnection();
		}
		throw new IllegalStateException( "Unknown Role " + role );
	}

}
