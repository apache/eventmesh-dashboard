package org.apache.eventmesh.dashboard.console.mapper.deploy;

import org.apache.eventmesh.dashboard.console.entity.cases.DeployScriptEntity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeployScriptMapper {


    @Insert("""
        insert into deploy_script () values()
        """)
    void insert(DeployScriptEntity deployScriptEntity);


    @Update("""
        update deploy_script set description=#{description} content=#{content} where id=#{id}")"
        """)
    void update(DeployScriptEntity deployScriptEntity);


    @Update("""
        update deploy_script set status = 1 where id=#{id}")"
        """)
    void deleteById(DeployScriptEntity deployScriptEntity);


    @Select("""
        <script>
            select * from deploy_script where organization_id=#{id}
            <if test = "name!=null">
               and name like concat('%',#{name},'%')
            </if>
        </script>
        """)
    void queryByName(DeployScriptEntity deployScriptEntity);
}
