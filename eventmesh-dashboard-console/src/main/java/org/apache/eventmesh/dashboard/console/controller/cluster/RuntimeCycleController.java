package org.apache.eventmesh.dashboard.console.controller.cluster;


import org.apache.eventmesh.dashboard.console.modle.cluster.runtimeCycle.CreateRuntimeDTO;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("runtimeCycleDeploy")
public class RuntimeCycleController {


    public String createRuntime(CreateRuntimeDTO createRuntimeDTO) {
        // 创建 部署流程
        // 得到集群 中 runtime 的配置
        // 创建 runtime 配置信息
        // 检查资源
        // 锁定资源
        // 锁定 端口
        // 组个调用 kubernetes 创建资源
        // 检查
        // 失败，释放资源，端口，删除 kubernetes内容
        return null;
    }


    /**
     * 暂停 那些集群可以暂停。被依赖的集群不允许暂停。暂停的含义是什么 暂停是否释放资源
     *
     * @return
     */
    public Integer pauseCluster() {
        // 查询集群

        // 判断集群类型

        // 查询依赖
        return null;
    }

    /**
     * 重启 重新开始集群
     *
     * @return
     */
    public Integer restartCluster() {
        // 查询集群

        // 判断集群类型

        // 查询依赖
        return null;
    }

    /**
     * 注销集群
     *
     * @return
     */
    public Integer cancelCluster() {
        // 查询集群

        // 判断集群类型

        // 查询依赖

        // 如果是全程托管，释放k8s 集群
        return null;
    }

}
