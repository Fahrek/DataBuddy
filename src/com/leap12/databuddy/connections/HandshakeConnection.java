package com.leap12.databuddy.connections;

import com.leap12.common.ClientConnection;
import com.leap12.common.Log;
import com.leap12.databuddy.BaseConnection;
import com.leap12.databuddy.Commands;
import com.leap12.databuddy.Commands.CmdResponse;
import com.leap12.databuddy.Commands.RequestStatus;
import com.leap12.databuddy.Commands.Role;

public class HandshakeConnection extends BaseConnection {

	@Override
	protected void onAttached(ClientConnection connection) throws Exception {
		connection.setInactivityTimeout(10000);
		connection.setKeepAlive(false); // we don't know the client protocol yet, could be HTTP or GAME
	}

	@Override
	protected void onReceivedMsg(String msg) throws Exception {
		String output = msg.replace("\r\n", "\\r\\n_DB_BREAK_");
		output = output.replace("\r", "\\r_DB_BREAK_");
		output = output.replace("\n", "\\n_DB_BREAK_");
		output = output.replace("_DB_BREAK_", "\n");

		Log.d(output);

		if (Commands.CMD_AUTH.isCommand(msg)) {
			getClientConnection().setKeepAlive(true);
			try {
				UserConnection connection = handleAuthenticateUser(msg);
				getClientConnection().setDelegate(connection);
			} catch (Exception e) {
				Log.e(e);
				writeResponse(e.getMessage());
				getClientConnection().stop();
			}
		} else if (msg.contains("HTTP")) {
			getClientConnection().setKeepAlive(false);
			writeMsg(""
					+ "Content-type: text/html\n\n"
					+ "<html>"
					+ "<body>"
					+ "<b>Hello World</b>"
					+ "</body>"
					+ "</html>\r\n\r\n");
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
	private UserConnection handleAuthenticateUser(String msg) throws Exception {
		CmdResponse<Role> request = Commands.CMD_AUTH.executeCommand(this, msg);
		if (RequestStatus.SUCCESS == request.getStatus()) {

			// TODO validate user -- maybe send them to the appropriate connection and let that connection do the validation? This would better support an anonymous type

			return toConnection(request);
		}
		throw new Exception(request.getError());
	}

	private UserConnection toConnection(CmdResponse<Role> request) {
		Role role = request.getValue();
		switch (role) {
		case sysop:
			return new SysOpConnection();
		case user:
			return new UserConnection();
		}
		throw new IllegalStateException("Unknown Role " + role);
	}

}
