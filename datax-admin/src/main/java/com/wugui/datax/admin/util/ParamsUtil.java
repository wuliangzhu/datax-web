package com.wugui.datax.admin.util;

import java.util.Date;

/**
 * 添加一些针对replaceparams的工具
 */
public class ParamsUtil {
    public static long getStepByParams(String content) {
        if (content == null || content.length() == 0){
            return Integer.MAX_VALUE;
        }

        String[] params = content.split(" ");

        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            String[] kv = param.split("=");
            if (kv[0].startsWith("-DloopStep")) {
                return Long.parseLong(kv[1]);
            }

        }

        return Integer.MAX_VALUE;
    }

    /**
     * 如果 triggerTime 在 startTime + step 之后，说明执行时间超过了 step，则以step为准，返回 startTime + step;
     * 否则返回 triggerTime
     * @param startTime
     * @param step
     * @param triggerTime
     * @return
     */
    public static Date getEndTime(Date startTime, long step, Date triggerTime) {
        long startTs = startTime.getTime();
        long triggerTs = triggerTime.getTime();

        if (startTs + step < triggerTs) {
            return new Date(startTs + step);
        }

        return triggerTime;
    }
}
