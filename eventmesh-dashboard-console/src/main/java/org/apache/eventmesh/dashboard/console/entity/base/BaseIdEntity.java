package org.apache.eventmesh.dashboard.console.entity.base;

import java.io.Serializable;
import java.time.LocalDateTime;


public class BaseIdEntity implements Serializable {

    private Long id;

    private LocalDateTime createTime;

    private Long createUserId;

    private LocalDateTime updateTime;

    private Long updateUserId;

    /**
     *  数据展示过程，操作必须是具体的行为
     *  过程有：等待，完成，中
     *  具体操作是一个有动词+名词的行为：停止扩容，
     *  一个操作允许有多个过程。是否可以在过程中取消行为
     *  集群创建中
     *  关闭集群：停止集群的运行，可以让重启集群
     *  删除集群： 关闭集群之后，可以备份数据，删除集群数据
     *  扩容中，
     *  缩容中
     *  重启前检查：
     *      检查 集群依赖情况
     *      检查 资源（最低集群） -> 资源上锁
     *
     *  重启：
     *      检查 集群依赖情况
     *      申请资源 -> 申请失败
     *               -> 申请成功
     *                  -> 部署A集群
     *                  -> 部署B集群
     *               -> 部署成功
     *               -> 测试
     *                 -> 开始心跳测试
     *                 -> 心跳测试中
     *                 -> 心跳测试成功
     *                 -> 开始数据测试
     *                 -> 数据测试中
     *                 -> 数据测试成功
     *  等待资源释放
     *    -> 资源释放中
     *      -> 资源释放失败
     *    -> 完成资源释放 -> 重启集群 ->
     *                  -> 删除集群数据 -> 删除集群数据中 -> 完成集群数据删除
     *                     ->
     *
     * 数据状态：
     * 集群状态：
     *  是否与集群状态共享
     *  启动中 运行 停运中 已停运  卸载中 完成卸载  作废
     * 运维状态：
     *
     * 检查状态，独立一个字段。用 二进制方式
     *  1. 心跳失败
     *  2. 配置异常
     *  3.
     *
     */
    private Long status;

    private Integer delete;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Long updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public Integer getDelete() {
        return delete;
    }

    public void setDelete(Integer delete) {
        this.delete = delete;
    }
}
