package org.apache.eventmesh.dashboard.console.controller.deploy;

import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterByCompleteDataDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.CreateClusterBySimpleDataDTO;
import org.apache.eventmesh.dashboard.console.modle.cluster.VerifyNameDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 1. 用户首页列表 2. 集群首页概要 kubernetes 集群在 eventmesh 集群里面创建的 只属于这个集群 kubernetes在创建的时候可以设为独立集群 如果创建集群 1. 全托管创建 因为全托管创建，不需要用户管理任何东西。 2. 全托管共享集群 创建时绑定集群流程 1.
 * 创建 kubernetes 集群 2. 创建 storage 集群 3. 创建 meta 集群 4. 创建 runtime 集群 5. 创建时 进行绑定 3. 已经 deploy config 创建，比较麻烦。这种 kubernetes 资源不会共享 1. 提供 kubernetes
 * 集群配置，多个 2. 选择 storage deploy config  or  storage id or name 3. 选择 meta deploy config or  meta id or name 4. 选择 runtime deploy config or runtime id
 * or name 5. 点击 创建，提供部署流程 4. 先创建，后绑定 1. 直接创建 2. 在内部进行绑定 5. 配置一个kubernetes集群，可以定时校验 deploy config 的效果。校验的目的是什么 创建节点的时候，先看自己生是否有 kubernetes。 然后检查上级
 * cluster 是否有 可用的 kubernetes
 */
@RestController
@RequestMapping("clusterCycleDeploy")
public class ClusterCycleController {


    @Autowired
    private CreateCycleService createCycleService;


    /**
     * @param verifyNameDTO
     * @return
     */
    public String verifyName(VerifyNameDTO verifyNameDTO) {

        return "";
    }

    /**
     * 先创建，后绑定 1. 直接创建 2. 在内部进行绑定 3. 然后进行产检，才能是真诚的
     * TODO 添加时状态为：创建中
     *      关联号集群之后，点击确认，才是运行
     *      还是 自动检查？
     *
     * @param createClusterBySimpleDataDTO
     */
    @PostMapping("createClusterBySimpleData")
    public void createClusterBySimpleData(@RequestBody CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
        createCycleService.createClusterBySimpleData(createClusterBySimpleDataDTO);
    }

    /**
     * 2. 全托管共享集群 创建时绑定集群流程 1. 创建 kubernetes 集群 2. 创建 storage 集群 3. 创建 meta 集群 4. 创建 runtime 集群 5. 创建时 进行绑定
     *
     * @param createClusterByCompleteDataDTO
     */
    @PostMapping("createClusterByCompleteData")
    public void createClusterByCompleteData(@RequestBody CreateClusterByCompleteDataDTO createClusterByCompleteDataDTO) {
        createCycleService.createClusterByCompleteData(createClusterByCompleteDataDTO);
    }

    @PostMapping("createClusterByConfigData")
    public void createClusterByConfigData(@RequestBody CreateClusterBySimpleDataDTO createClusterBySimpleDataDTO) {
        //createCycleService.createRuntimeByBySimpleData();
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
