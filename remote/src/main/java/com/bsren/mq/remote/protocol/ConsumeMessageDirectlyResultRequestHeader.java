package com.bsren.mq.remote.protocol;

import com.bsren.mq.remote.CommandHeader;
import lombok.Data;

@Data
public class ConsumeMessageDirectlyResultRequestHeader implements CommandHeader {

    private String clientId;

    private String brokerName;




}
