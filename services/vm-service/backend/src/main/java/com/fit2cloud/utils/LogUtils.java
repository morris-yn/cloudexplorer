package com.fit2cloud.utils;

import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.mapper.VmDefaultConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
public class LogUtils {

    private static VmDefaultConfigMapper vmDefaultConfigMapper;

    @Autowired
    public LogUtils(VmDefaultConfigMapper vmDefaultConfigMapper){
        this.vmDefaultConfigMapper = vmDefaultConfigMapper;
    }

    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    public static void setLog(Integer type,String info){
        logExecutor.submit(() -> {
            vmDefaultConfigMapper.setLog(type,info);
        });
    }
}
