/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.dashboard.console.function.report.collect;

/**
 * TODO 设计一个 兼容 metadata list 与 全量 collect
 *      纠结，
 *      一个metadata，一个 collect，还是 一个类型的 metadata list 一个 collect？，全量 collect
 *      一个 metadata 在抽象度就非常高，代码就非常简单. 一个 topic 一个 collect
 *      一个 metadata list 需要进行组织，优点是 性能好。 topic 类型 一个 collect
 *     全量 collect ，比如 rocketmq exporter 代码复制过来。优点是无任何 metadata 依赖
 *     问题（痛苦）： rocketmq 目前大部分只支持 一个一个 metadata 请求，
 *                   一个个请求性能比较低
 *                  kafka 大部分支持 全量请求
 * TODO ？
 *     大集群情况下， exporter 独立出来。如何处理。
 *          需要实现一套独立的 SDKManage 管理对象
 *     小量情况下， exporter  不独立
 *     topic  10个 group， 1000个 client,
 *            10个 group，  3万个 client,
 *     1000 topic, 2000个， 1万个，  kafka，24个节点，48c，128G
 *     rocketmq-exporter
 *         eventmesh -> exporter -> broker -> client  3
 *     eventmesh
 *         eventmesh -> broker -> client  2
 *         client -> eventmesh  1  还可以压缩
 *     1s 5s 感知快
 *
 *     消息挤压，kubernetes，授权我帮他扩容
 *
 *
 * TODO  OK
 *     普罗米修斯 ，
 *     如果  一个meta，一个broker， 一个 普罗米修斯， 没有
 *     如果  两个meta  被 多个broker 共享，一个 普罗米修斯  没有问题，不要控制
 *     如果  两个meta  被 多个broker 共享， 多个普罗米修斯  就有问题，会采集重复数据
 *     一个 collect 是对应 meta cluster ，还是 main cluster
 *
 * TODO  OK
 *     meta 数据是否放到 ectd 里面。1 到 5 同步
 */
public interface Collect {

}
