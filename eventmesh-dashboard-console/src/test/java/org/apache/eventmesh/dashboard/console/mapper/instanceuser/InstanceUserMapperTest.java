package org.apache.eventmesh.dashboard.console.mapper.instanceuser;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.instanceuser.InstanceUserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:instanceuser-test.sql")
class InstanceUserMapperTest {

    @Autowired
    private InstanceUserMapper instanceUserMapper;

    @Test
    public void testSelectAll() {
        List<InstanceUserEntity> instanceUserEntities = instanceUserMapper.selectAll();
        assertEquals(3, instanceUserEntities.size());
    }

    @Test
    public void testSelectById() {
        InstanceUserEntity instanceUserEntity1 = new InstanceUserEntity();
        instanceUserEntity1.setId(3L);
        InstanceUserEntity instanceUserEntity = instanceUserMapper.selectById(instanceUserEntity1);
        assertEquals(3, instanceUserEntity.getId());
    }

    @Test
    public void testSelectByName() {
        InstanceUserEntity instanceUserEntity1 = new InstanceUserEntity();
        instanceUserEntity1.setName("name01");
        List<InstanceUserEntity> instanceUserEntities = instanceUserMapper.selectByName(instanceUserEntity1);
        assertEquals(1, instanceUserEntities.size());
    }

    @Test
    public void testInsert() {
        InstanceUserEntity instanceUserEntity = new InstanceUserEntity(0, "pwd", 13L, "name4", "11", 1);
        instanceUserMapper.insert(instanceUserEntity);
        assertNotNull(instanceUserEntity);
        assertEquals(4, instanceUserEntity.getId());
        // instanceuser.sql中新加三条数据，这条数据id自增为4
    }

    @Test
    public void testUpdateNameById() {
        InstanceUserEntity instanceUserEntity = new InstanceUserEntity();
        instanceUserEntity.setId(3L);
        instanceUserEntity.setPassword("123");
        instanceUserMapper.updatePasswordById(instanceUserEntity);
        instanceUserEntity = instanceUserMapper.selectById(instanceUserEntity);
        assertEquals("123", instanceUserEntity.getPassword());
    }

}