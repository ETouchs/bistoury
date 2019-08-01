package qunar.tc.bistoury.ui.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import qunar.tc.bistoury.serverside.bean.ApiResult;
import qunar.tc.bistoury.serverside.bean.ApiStatus;
import qunar.tc.bistoury.serverside.util.ResultHelper;

/**
 * @author leix.xie
 * @date 2019/7/2 16:05
 * @describe
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResult runtimeExHandler(Exception e) {
        LOGGER.error("发生系统异常：{}", e.getMessage(), e);
        return ResultHelper.fail(ApiStatus.SYSTEM_ERROR.getCode(), "系统异常：" + e.getMessage());
    }

    @ExceptionHandler(PermissionDenyException.class)
    @ResponseBody
    public ApiResult PermissionDenyExHandler(Exception e) {
        LOGGER.error("权限异常：{}", e.getMessage(), e);
        return ResultHelper.fail(ApiStatus.SYSTEM_ERROR.getCode(), "权限不足，拒绝访问：" + e.getMessage());
    }
}
