package org.apache.eventmesh.dashboard.console.mapper.serviceuser;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.serviceuser.ServiceUserEntity;
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
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:serviceuser-test.sql")
class ServiceUserMapperTest {

    @Autowired
    private ServiceUserMapper serviceUserMapper;

    @Test
    public void testSelectAll() {
        List<ServiceUserEntity> serviceUserEntities = serviceUserMapper.selectAll();
        assertEquals(3, serviceUserEntities.size());
    }

    @Test
    public void testSelectById() {
        ServiceUserEntity serviceUserEntity1 = new ServiceUserEntity();
        serviceUserEntity1.setId(3L);
        ServiceUserEntity serviceUserEntity = serviceUserMapper.selectById(serviceUserEntity1);
        assertEquals(3, serviceUserEntity.getId());
    }

    @Test
    public void testSelectByName() {
        ServiceUserEntity serviceUserEntity1 = new ServiceUserEntity();
        serviceUserEntity1.setName("name01");
        List<ServiceUserEntity> serviceUserEntities = serviceUserMapper.selectByName(serviceUserEntity1);
        assertEquals(1, serviceUserEntities.size());
    }

    @Test
    public void testInsert() {
        ServiceUserEntity serviceUserEntity = new ServiceUserEntity(0, "pwd", 13L, "name4", "11", 1);
        serviceUserMapper.insert(serviceUserEntity);
        assertNotNull(serviceUserEntity);
        assertEquals(4, serviceUserEntity.getId());
        // serviceuser.sql中新加三条数据，这条数据id自增为4
    }

    @Test
    public void testUpdateNameById() {
        ServiceUserEntity serviceUserEntity = new ServiceUserEntity();
        serviceUserEntity.setId(3L);
        serviceUserEntity.setPassword("123");
        serviceUserMapper.updatePasswordById(serviceUserEntity);
        serviceUserEntity = serviceUserMapper.selectById(serviceUserEntity);
        assertEquals("123", serviceUserEntity.getPassword());
    }

}