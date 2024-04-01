package org.apache.eventmesh.dashboard.console.mapper.acl;
import java.sql.Timestamp;

import org.apache.eventmesh.dashboard.console.EventMeshDashboardApplication;
import org.apache.eventmesh.dashboard.console.entity.acl.AclEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventMeshDashboardApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:use-test-schema.sql", "classpath:eventmesh-dashboard.sql"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:acl-test.sql")

public class AclMapperTest {

    @Autowired
    private AclMapper aclMapper;

    @Test
    public void testBatchInsert() {

        List<AclEntity> aclEntities = new ArrayList<>();
        for (int i = 7; i < 10; i++) {
            AclEntity aclEntity = new AclEntity();

            aclEntity.setClusterId(1L);
            aclEntity.setPattern("pattern1");
            aclEntity.setOperation(0);
            aclEntity.setPermissionType(1);
            aclEntity.setHost("127.0.0.1");
            aclEntity.setResourceType(1);
            aclEntity.setResourceName("2");
            aclEntity.setPatternType(1);
            aclEntities.add(aclEntity);
        }

        aclMapper.batchInsert(aclEntities);
        assertEquals(3, aclEntities.size());
        // aclEntities.size()返回的是新加的size，是3，而不是acl表中总的size，是5
    }

    @Test
    public void testInsert() {
//        AclEntity aclEntity = new AclEntity("", 0, "0", "", "0", "source_name", 1);
        AclEntity aclEntity = new AclEntity();
        aclEntity.setClusterId(0L);
        aclEntity.setPattern("pattern1");
        aclEntity.setOperation(0);
        aclEntity.setPermissionType(1);
        aclEntity.setHost("host");
        aclEntity.setResourceType(1);
        aclEntity.setResourceName("resn");
        aclEntity.setPatternType(0);
        aclEntity.setStatus(0);
        aclEntity.setId(0L);
        aclEntity.setClusterId(0L);

        aclMapper.insert(aclEntity);
        assertNotNull(aclEntity);
        assertEquals(5, aclEntity.getId());
    }

    @Test
    public void testDelete() {
        AclEntity aclEntity = new AclEntity();
        aclEntity.setId(4L);
        aclMapper.deleteById(aclEntity);
        assertEquals(4, aclEntity.getId());
        // 删除的就是id=4这条数据
        // 通过改status为0，实现删除
    }

    @Test
    public void testUpdate() {
        AclEntity aclEntity = new AclEntity();
        aclEntity.setId(3L);
        aclEntity.setResourceType(10);
        aclMapper.updateResourceTypeById(aclEntity);
        aclEntity = aclMapper.selectById(aclEntity);
        assertEquals(10, aclEntity.getResourceType());
    }

    @Test
    public void testSelect() {
        AclEntity aclEntity = new AclEntity();
        aclEntity.setId(3L);
        aclMapper.selectById(aclEntity);
        assertEquals(3, aclEntity.getId());
    }

}
