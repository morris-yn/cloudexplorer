package com.fit2cloud.gateway.config;

import com.fit2cloud.autoconfigure.ServerInfo;
import com.fit2cloud.common.constants.GlobalErrorCodeConstants;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.log.annotation.OperatedLog;
import com.fit2cloud.common.log.constants.OperatedTypeEnum;
import com.fit2cloud.common.log.entity.OperatedLogVO;
import com.fit2cloud.common.log.utils.IpUtil;
import com.fit2cloud.common.log.utils.LogUtil;
import com.fit2cloud.common.utils.CurrentUserUtils;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.dto.UserDto;
import com.fit2cloud.request.LoginRequest;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.server.context.SecurityContextServerWebExchange;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * @author jianneng
 * @date 2022/9/15 10:26
 **/
@Aspect
@Component
public class LoginLogAspect {

    @Pointcut("@annotation(com.fit2cloud.common.log.annotation.OperatedLog)" + "||"
            + "execution(* com.fit2cloud.controller.*.*(..))")
    public void annotation() {

    }

    @Around("annotation()")
    public Object aroundMethod(ProceedingJoinPoint pjd) throws Throwable {
        Long startTime = System.currentTimeMillis();
        //创建一个请求id
        String requestId = String.valueOf(UUID.randomUUID());
        MDC.put("requestId", requestId);
        Object res = null;
        ResultHolder errorResult = ResultHolder.error(GlobalErrorCodeConstants.BUSINESS_ERROR.getCode(), GlobalErrorCodeConstants.BUSINESS_ERROR.getMessage());
        try {
            res = pjd.proceed();
            Mono<ResponseEntity<ResultHolder<Object>>> result = (Mono) res;
            if (result.block().getStatusCode().value() != 200) {
                errorResult = ResultHolder.error(result.block().getStatusCode().value(), result.block().getBody().getMessage());
            } else {
                errorResult = ResultHolder.success("ok");
            }
        } catch (Exception e) {
            if (e instanceof Fit2cloudException) {
                Fit2cloudException fit2cloudException = (Fit2cloudException) e;
                errorResult.setCode(fit2cloudException.getCode());
                errorResult.setMessage(fit2cloudException.getMessage());
            }
            errorResult.setMessage(e.getMessage());
            e.printStackTrace();
        }
        Long endTime = System.currentTimeMillis();
        saveLog(pjd, res, endTime - startTime, errorResult);
        //请求完成后清理MDC
        MDC.clear();
        errorResult.setRequestId(requestId);
        return res;
    }

    private void saveLog(ProceedingJoinPoint pjd, Object res, Long time, ResultHolder errorResult) {
        try {
            MethodSignature methodSignature = (MethodSignature) pjd.getSignature();
            Method method = methodSignature.getMethod();
            Object[] args = pjd.getArgs();
            OperatedLog annotation = method.getAnnotation(OperatedLog.class);
            if (annotation != null) {
                OperatedLogVO logVO = createLog(time, errorResult, annotation, args);
                logVO.setMethod("GET");
                // 操作
                logVO.setOperated(annotation.operated().getOperate());
                logVO.setOperatedName(OperatedTypeEnum.getDescriptionByOperate(logVO.getOperated()));
                // 资源类型
                logVO.setResourceType(annotation.resourceType().getCode());
                // 请求地址
                Arrays.stream(args).filter(c -> c instanceof SecurityContextServerWebExchange)
                        .map(c -> (SecurityContextServerWebExchange) c)
                        .findFirst()
                        .ifPresent(s -> {
                            String ipAddress = IpUtil.getIpAddress(s.getDelegate().getRequest());
                            logVO.setSourceIp(ipAddress);
                        });
                // 上下文设置
                setMDC(logVO);
                LogUtil.info(logVO);
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage());
            e.printStackTrace();
        }
    }


    @NotNull
    private OperatedLogVO createLog(Long time, ResultHolder errorResult, OperatedLog annotation, Object[] args) {
        OperatedLogVO logVO = new OperatedLogVO();
        logVO.setModule(ServerInfo.module);
        logVO.setRequestTime(new Date().getTime());
        logVO.setTime(time);
        logVO.setStatus(errorResult.getCode() == 200 ? 1 : 0);
        logVO.setCode(errorResult.getCode());
        logVO.setMsg(errorResult.getMessage());
        if (annotation != null) {
            if (StringUtils.equalsIgnoreCase(annotation.operated().getOperate(), OperatedTypeEnum.LOGIN.getOperate()) ||
                    StringUtils.equalsIgnoreCase(annotation.operated().getOperate(), OperatedTypeEnum.LOGOUT.getOperate())) {
                LoginRequest loginRequest = JsonUtil.parseObject(JsonUtil.toJSONString(args[0]), LoginRequest.class);
                logVO.setUser(loginRequest.getUsername());
            }
        }
        if (StringUtils.isEmpty(logVO.getUser()) && Optional.ofNullable(CurrentUserUtils.getUser()).isPresent()) {
            UserDto userDto = CurrentUserUtils.getUser();
            logVO.setUser(userDto.getUsername());
            logVO.setUserId(userDto.getId());
        }
        return logVO;
    }

    private void setMDC(OperatedLogVO logVO) {
        MDC.put("module", logVO.getModule());
        MDC.put("operated", logVO.getOperated());
        MDC.put("operatedName", logVO.getOperatedName());
        MDC.put("resourceId", logVO.getResourceId());
        MDC.put("resourceType", logVO.getResourceType());
        MDC.put("joinResourceId", logVO.getJoinResourceId());
        MDC.put("user", logVO.getUser());
        MDC.put("userId", logVO.getUserId());
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
