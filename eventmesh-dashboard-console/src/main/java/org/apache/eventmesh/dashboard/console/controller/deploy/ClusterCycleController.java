package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateClusterByDeployScriptHandler;
import org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateClusterByOnlyDataHandler;
import org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateRuntimeByDeployScriptHandler;
import org.apache.eventmesh.dashboard.console.controller.deploy.create.CreateRuntimeByOnlyDataHandler;
import org.apache.eventmesh.dashboard.console.modle.cluster.VerifyNameDTO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateClusterByOnlyDataDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByDeployScriptDO;
import org.apache.eventmesh.dashboard.console.modle.deploy.create.CreateRuntimeByOnlyDataDO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 1. 用户首页列表
 * <p>
 * 2. 集群首页概要 kubernetes 集群在 eventmesh 集群里面创建的 只属于这个集群 kubernetes在创建的时候可以设为独立集群 如果创建集群
 * <p>
 * 1. 全托管创建 因为全托管创建，不需要用户管理任何东西。
 * <p>
 * 2. 全托管共享集群 创建时绑定集群流程
 * <p>
 * 1.创建 kubernetes 集群
 * <p>
 * 2. 创建 storage 集群
 * <p>
 * 3. 创建 meta 集群
 * <p>
 * 4. 创建 runtime 集群
 * <p>
 * 5. 创建时 进行绑定 3. 已经 deploy config 创建，比较麻烦。这种 kubernetes 资源不会共享
 * <p>
 * 1. 提供 kubernetes集群配置，多个
 * <p>
 * 2. 选择 storage deploy config  or  storage id or name
 * <p>
 * 3. 选择 meta deploy config or  meta id or name
 * <p>
 * 4. 选择 runtime deploy config or runtime idor name
 * <p>
 * 5. 点击 创建，提供部署流程 4. 先创建，后绑定
 * <p>
 * 1. 直接创建
 * <p>
 * 2. 在内部进行绑定 5. 配置一个kubernetes集群，可以定时校验 deploy config 的效果。校验的目的是什么 创建节点的时候，先看自己生是否有 kubernetes。 然后检查上级cluster 是否有 可用的 kubernetes
 */
@RestController
@RequestMapping("clusterCycleDeploy")
public class ClusterCycleController {


    @Autowired
    private CreateRuntimeByDeployScriptHandler createRuntimeByDeployScriptHandler;

    @Autowired
    private CreateRuntimeByOnlyDataHandler createRuntimeByOnlyDataHandler;

    @Autowired
    private CreateClusterByOnlyDataHandler createClusterByOnlyDataHandler;

    @Autowired
    private CreateClusterByDeployScriptHandler createClusterByDeployScriptHandler;


    /**
     * @param verifyNameDTO
     * @return
     */
    public String verifyName(VerifyNameDTO verifyNameDTO) {

        return "";
    }

    @PostMapping("createRuntimeByOnlyDataHandler")
    public void createRuntimeByOnlyDataHandler(@RequestBody @Validated CreateRuntimeByOnlyDataDO createRuntimeByOnlyDataDO) {
        this.createRuntimeByOnlyDataHandler.handler(createRuntimeByOnlyDataDO);
    }

    @PostMapping("createRuntimeByDeployScript")
    public void createRuntimeByDeployScript(@RequestBody @Validated CreateRuntimeByDeployScriptDO createRuntimeByDeployScriptDO) {
        this.createRuntimeByDeployScriptHandler.handler(createRuntimeByDeployScriptDO);
    }

    @PostMapping("createClusterByDeployScript")
    public void createClusterByDeployScript(@RequestBody @Validated CreateClusterByDeployScriptDO createClusterByDeployScriptDO) {
        this.createClusterByDeployScriptHandler.handler(createClusterByDeployScriptDO);
    }

    @PostMapping("createClusterByOnlyScript")
    public void createClusterByOnlyScript(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }


    @PostMapping("pauseCluster")
    public void pauseCluster(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }

    @PostMapping("pauseRuntime")
    public void pauseRuntime(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }

    @PostMapping("relationship")
    public void relationship(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }

    @PostMapping("unrelationship")
    public void unrelationship(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }

    @PostMapping("uninstallCluster")
    public void uninstallCluster(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }

    @PostMapping("uninstallRuntime")
    public void uninstallRuntime(@RequestBody @Validated CreateClusterByOnlyDataDO createClusterByOnlyDataDO) {
        this.createClusterByOnlyDataHandler.handler(createClusterByOnlyDataDO);
    }

}
