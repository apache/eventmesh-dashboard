package org.apache.eventmesh.dashboard.console.adapter;

import com.alibaba.fastjson2.JSON;
import org.checkerframework.checker.units.qual.C;

import java.util.HashMap;
import java.util.Map;

public class DynamicsAdapter {

    private static final DynamicsAdapter INSTANCE = new DynamicsAdapter();

    public static final DynamicsAdapter getInstance(){
        return INSTANCE;
    }

    private Map<Class<?>,Class<?>> objectMapper = new HashMap<>();


    public DynamicsAdapter(){

    }

    public <T>T  to(Object source,Class<?> target){
        String sourceString = JSON.toJSONString(source);
        return (T)JSON.to(target,sourceString);
    }


}
