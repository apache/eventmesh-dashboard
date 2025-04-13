package org.apache.eventmesh.dashboard.console.modle.deploy;

import org.apache.eventmesh.dashboard.console.entity.message.TopicEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeployTopicDO extends BaseConifgDO {

    private TopicEntity topic;


}
