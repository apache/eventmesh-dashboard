package org.apache.eventmesh.dashboard.core.remoting;

import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import org.apache.eventmesh.dashboard.common.model.metadata.RuntimeMetadata;
import org.apache.eventmesh.dashboard.core.cluster.ClusterDO;
import org.apache.eventmesh.dashboard.core.cluster.ColonyDO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 默认是一个集群，操作是基于集群操作还是单个操作
 */
public abstract class AbstractRemotingService {

    @Setter
    protected ColonyDO colonyDO;

    protected ClusterDO clusterDO;


    public void init(){
        this.clusterDO = colonyDO.getClusterDO();
        this.createConfig();
        this.doInit();
    }

    public abstract void createConfig();

    public List<String> getMeta(){
        List<String> list = new ArrayList<>();
        for(ColonyDO c : colonyDO.getMetaColonyDOList().values()){
            for(RuntimeMetadata runtimeMetadata : c.getClusterDO().getRuntimeMap().values()){
                list.add(runtimeMetadata.getHost()+":"+runtimeMetadata.getPort());
            }
        }
        return list;
    }

    public String getMetaString(){
        StringBuilder sb = new StringBuilder();
        List<String> list = new ArrayList<>();
        for(ColonyDO c : colonyDO.getMetaColonyDOList().values()){
            for(RuntimeMetadata runtimeMetadata : c.getClusterDO().getRuntimeMap().values()){
                sb.append(runtimeMetadata.getHost()+":"+runtimeMetadata.getPort());
                sb.append(";");
            }
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

    public void update(){

    }

    public Long getClusterId(){
        return clusterDO.getClusterInfo().getClusterId();
    }

    public Long getId(){
        return clusterDO.getClusterInfo().getId();
    }

    public <T>T toDataOjbect(Object object,Class<?> clazz){
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            return (T) jsonObject.toJavaObject(clazz);
        }else if(object instanceof  String){
            return (T)JSONObject.parseObject((String)object,clazz);
        }else if(object instanceof Map){

        }
        return null;
    }


    protected abstract void  doInit();
}
