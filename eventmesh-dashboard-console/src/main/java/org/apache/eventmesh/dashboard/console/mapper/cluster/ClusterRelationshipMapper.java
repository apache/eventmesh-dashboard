package org.apache.eventmesh.dashboard.console.mapper.cluster;

import org.apache.eventmesh.dashboard.console.entity.cluster.ClusterRelationshipEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ClusterRelationshipMapper {


    @Insert(" insert into cluster_relationship (cluster_type,cluster_id,relationship_type,relationship_id)values( #{clusterType},#{clusterId},#{relationshipType},#{relationshipId})")
    Integer addClusterRelationshipEntry(ClusterRelationshipEntity clusterRelationshipEntity);

    @Update("update cluster_relationship set status = 3 where id = #{id} ")
    Integer relieveRelationship(ClusterRelationshipEntity clusterRelationshipEntity);

    @Select(" select * from cluster_relationship where status = 3")
    List<ClusterRelationshipEntity> selectAll();

    @Select(" select * from cluster_relationship where update_date = #{updateData} and status in( 2 ,3)")
    List<ClusterRelationshipEntity> selectNewlyIncreased();
}
