package com.bsren.mq.remote.service;

import com.bsren.mq.remote.RemotingCommand;
import com.bsren.mq.remote.service.RemotingService;

public interface RemotingClient extends RemotingService {

    RemotingCommand invoke(String address, RemotingCommand remotingCommand);

}
