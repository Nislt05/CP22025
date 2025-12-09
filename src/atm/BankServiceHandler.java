package atm;

import common.CommandDTO;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface BankServiceHandler {
	void send(CommandDTO commandDTO, CompletionHandler<Integer, ByteBuffer> handlers);
}
