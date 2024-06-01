package org.apache.eventmesh.dashboard.core.remoting.rocketmq;

import org.apache.eventmesh.dashboard.common.model.remoting.GlobalResult;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKManager;
import org.apache.eventmesh.dashboard.core.function.SDK.SDKTypeEnum;
import org.apache.eventmesh.dashboard.core.function.SDK.config.CreateRocketmqConfig;
import org.apache.eventmesh.dashboard.core.remoting.AbstractRemotingService;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;

import java.util.AbstractMap;
import java.util.Objects;
import java.util.Set;

/**
 * rocketmq 其他不同的是。 以nameservier 为主。那么可以多集群。一个eventmesh 可以操作多个集群
 */
public abstract class AbstractRocketMQRemotingService extends AbstractRemotingService {


    protected DefaultMQAdminExt defaultMQAdminExt;

    protected CreateRocketmqConfig createRocketmqConfig;


    @Override
    public void createConfig(){
        createRocketmqConfig = new CreateRocketmqConfig();
        createRocketmqConfig.setNameServerUrl(this.getMetaString());
    }

    @Override
    protected void doInit() {
        AbstractMap.SimpleEntry<String, DefaultMQAdminExt> clientSimple = SDKManager.getInstance().createClient(SDKTypeEnum.STORAGE_ROCKETMQ_ADMIN, createRocketmqConfig);
        this.defaultMQAdminExt = clientSimple.getValue();
    }

    protected <T> T cluster(GlobalResult t, Function function) {
        try {

            /*for(ColonyDO clusterDO : this.cache.getClusterDOList()){
                    for(RuntimeMetadata runtimeMetadata : clusterDO.getRuntimeEntityList()){

                    }
            }*/

            Set<String> masterSet =
                    CommandUtil.fetchMasterAddrByClusterName(defaultMQAdminExt, createRocketmqConfig.getClusterName());
            for (String masterName : masterSet) {
                Object newResult = function.apply(masterName, t);
                if (Objects.nonNull(newResult)) {
                    return (T) newResult;
                }
            }
            t.setCode(200);
        } catch (Exception exception) {
            t.setCode(400);
            t.setErrorMessages(exception.getMessage());
            t.setThrowable(exception);
        } finally {
            return (T) t;
        }
    }

    ;

    protected <T> T clusterName(GlobalResult t, Function function) {
        try {



            Object newResult = function.apply(createRocketmqConfig.getClusterName(), t);
            if (Objects.nonNull(newResult)) {
                return (T) newResult;
            }
            t.setCode(200);
        } catch (Exception exception) {
            t.setCode(400);
            t.setErrorMessages(exception.getMessage());
            t.setThrowable(exception);
        } finally {
            return (T) t;
        }
    }

    protected interface Function<T> {

        T apply(String masterName, T t) throws Exception;
    }


}
