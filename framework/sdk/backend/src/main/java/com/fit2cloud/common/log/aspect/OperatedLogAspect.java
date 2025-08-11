package com.fit2cloud.common.log.aspect;

import com.fit2cloud.autoconfigure.ServerInfo;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.log.annotation.OperatedLog;
import com.fit2cloud.common.log.annotation.OperatedLogFieldConver;
import com.fit2cloud.common.log.constants.OperatedTypeEnum;
import com.fit2cloud.common.log.conver.ResourceConvert;
import com.fit2cloud.common.log.entity.OperatedLogVO;
import com.fit2cloud.common.log.utils.IpUtil;
import com.fit2cloud.common.log.utils.LogUtil;
import com.fit2cloud.common.log.utils.SpelUtil;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.common.utils.CurrentUserUtils;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.common.utils.SensitiveFieldUtils;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Objects;

/**
 * @author jianneng
 * @date 2022/9/15 10:26
 **/
@Aspect
@Component
public class OperatedLogAspect {

    @Pointcut("@annotation(com.fit2cloud.common.log.annotation.OperatedLog)")
    public void annotation() {

    }

    @Around("annotation()")
    public Object aroundMethod(ProceedingJoinPoint pjd) throws Throwable {
        Long startTime = System.currentTimeMillis();
        Object res = null;
        ResultHolder errorResult = ResultHolder.success("ok");
        OperatedLogVO logVO = initLog(pjd, errorResult);
        try {
            res = pjd.proceed();
            if (res instanceof ResultHolder) {
                errorResult = (ResultHolder) res;
            }
        } catch (Exception e) {
            errorResult = ResultHolder.error("error");
            if (e instanceof Fit2cloudException) {
                Fit2cloudException fit2cloudException = (Fit2cloudException) e;
                errorResult.setCode(fit2cloudException.getCode());
                errorResult.setMessage(fit2cloudException.getMessage());
            } else {
                errorResult.setMessage(e.getMessage());
            }
        }
        Long endTime = System.currentTimeMillis();
        saveLog(logVO, endTime - startTime, errorResult);
        //请求完成后清理MDC
        MDC.clear();
        if (errorResult.getCode() != 200) {
            throw new Fit2cloudException(errorResult.getCode(), errorResult.getMessage());
        }
        return res;
    }

    private OperatedLogVO initLog(ProceedingJoinPoint pjd, ResultHolder errorResult) {
        OperatedLogVO logVO = createLog(errorResult);
        try {
            MethodSignature methodSignature = (MethodSignature) pjd.getSignature();
            Method method = methodSignature.getMethod();
            Object[] args = pjd.getArgs();
            OperatedLog annotation = method.getAnnotation(OperatedLog.class);
            // 日志注解内容
            if (annotation != null) {
                Class logvoClass = logVO.getClass();
                String paramStr = "";
                // 操作
                logVO.setOperated(annotation.operated().getOperate());
                String operatedName = OperatedTypeEnum.getDescriptionByOperate(logVO.getOperated());
                logVO.setOperatedName(operatedName);
                // 参数解析
                paramStr = JsonUtil.toJSONString(args);
                if (StringUtils.isNotEmpty(annotation.content())) {
                    try {
                        logVO.setContent(OperatedTypeEnum.getDescriptionByOperate(SpelUtil.getElValueByKey(pjd, annotation.content())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (StringUtils.isEmpty(logVO.getContent())) {
                        // 操作内容解析
                        logVO.setContent(SpelUtil.getElValueByKey(pjd, annotation.content()));
                    }
                }
                // 资源类型
                logVO.setResourceType(annotation.resourceType().getName());
                if (StringUtils.isNotEmpty(annotation.resourceId())) {
                    // 资源ID,这里特殊处理，到时候查询用到
                    logVO.setResourceId(SpelUtil.getElValueByKey(pjd, annotation.resourceId()) + "@" + annotation.resourceType().getCode());
                    Field field = logvoClass.getDeclaredField("resourceId");
                    OperatedLogFieldConver fieldAnnotation = field.getAnnotation(OperatedLogFieldConver.class);
                    if (Objects.nonNull(fieldAnnotation)) {
                        String a = CommonUtil.exec(fieldAnnotation.conver(), logVO.getResourceId(), ResourceConvert::conver);
                        logVO.setResourceName(a);
                    }
                }
                if (StringUtils.isNotEmpty(annotation.joinResourceId())) {
                    // 关联资源ID
                    logVO.setJoinResourceId(SpelUtil.getElValueByKey(pjd, annotation.joinResourceId()) + "@" + annotation.joinResourceType().getCode());
                    Field field = logvoClass.getDeclaredField("joinResourceId");
                    OperatedLogFieldConver fieldAnnotation = field.getAnnotation(OperatedLogFieldConver.class);
                    if (Objects.nonNull(fieldAnnotation)) {
                        String a = CommonUtil.exec(fieldAnnotation.conver(), logVO.getJoinResourceId(), ResourceConvert::conver);
                        logVO.setJoinResourceName(a);
                    }
                }
                // 配置请求信息
                extractRequestInfo(errorResult, logVO, paramStr);
            }
        } catch (Exception e) {
            LogUtil.error("记录日志失败:{}", e.getMessage());
            logVO.setStatus(1);
            logVO.setResponse(e.getMessage());
        }
        return logVO;
    }

    private void saveLog(OperatedLogVO logVO, Long time, ResultHolder resultHolder) {
        logVO.setTime(time);
        logVO.setStatus(resultHolder.getCode() == 200 ? 1 : 0);
        logVO.setCode(resultHolder.getCode());
        if (resultHolder.getCode() != 200) {
            logVO.setResponse(resultHolder.getMessage());
        }
        setMDC(logVO);
        //必须
        LogUtil.info(JsonUtil.toJSONString(logVO));
    }


    /**
     * 请求头信息以及请求结果
     *
     * @param errorResult
     * @param logVO
     * @param paramStr
     */
    private void extractRequestInfo(ResultHolder errorResult, OperatedLogVO logVO, String paramStr) {
        // 请求头信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        logVO.setUrl(request.getRequestURL().toString());
        logVO.setMethod(request.getMethod());
        logVO.setParams(paramStr);
        logVO.setSourceIp(IpUtil.getIpAddress(request));
        if (errorResult.getCode() != 200) {
            logVO.setResponse(errorResult.getMessage());
        }
        if (StringUtils.isNotEmpty(logVO.getParams())) {
            logVO.setParams(SensitiveFieldUtils.desensitization(logVO.getParams()));
        }
    }

    @NotNull
    private OperatedLogVO createLog(ResultHolder errorResult) {
        OperatedLogVO logVO = new OperatedLogVO();
        logVO.setModule(ServerInfo.moduleInfo.getName());
        logVO.setRequestTime(new Date().getTime());
        logVO.setStatus(errorResult.getCode() == 200 ? 1 : 0);
        logVO.setCode(errorResult.getCode());
        UserDto userDto = CurrentUserUtils.getUser();
        logVO.setUser(userDto.getName());
        logVO.setUserId(userDto.getId());
        return logVO;
    }

    private void setMDC(OperatedLogVO logVO) {
        MDC.put("module", logVO.getModule());
        MDC.put("operated", logVO.getOperated());
        MDC.put("operatedName", logVO.getOperatedName());
        MDC.put("resourceId", logVO.getResourceId());
        MDC.put("resourceName", logVO.getResourceName());
        MDC.put("resourceType", logVO.getResourceType());
        MDC.put("joinResourceId", logVO.getJoinResourceId());
        MDC.put("joinResourceName", logVO.getJoinResourceName());
        MDC.put("user", logVO.getUser());
        MDC.put("userId", logVO.getUserId());
        MDC.put("url", logVO.getUrl());
        MDC.put("content", logVO.getContent());
        MDC.put("requestTime", String.valueOf(logVO.getRequestTime()));
        MDC.put("method", logVO.getMethod());
        MDC.put("params", logVO.getParams());
        MDC.put("status", String.valueOf(logVO.getStatus()));
        MDC.put("sourceIp", logVO.getSourceIp());
        MDC.put("time", String.valueOf(logVO.getTime()));
        MDC.put("code", String.valueOf(logVO.getCode()));
        MDC.put("response", logVO.getResponse());
    }


}
