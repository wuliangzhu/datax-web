package com.wugui.datax.executor.service.command;

import com.wugui.datatx.core.biz.model.TriggerParam;
import com.wugui.datatx.core.enums.IncrementTypeEnum;
import com.wugui.datatx.core.log.JobLogger;
import com.wugui.datatx.core.util.Constants;
import com.wugui.datatx.core.util.DateUtil;
import com.wugui.datax.executor.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.wugui.datatx.core.util.Constants.SPLIT_COMMA;
import static com.wugui.datax.executor.service.jobhandler.DataXConstant.*;

/**
 * DataX command build
 *
 * @author jingwk 2020-06-07
 */
public class BuildCommand {

    /**
     * DataX command build
     * @param tgParam
     * @param tmpFilePath
     * @param dataXPyPath
     * @return
     */
    public static String[] buildDataXExecutorCmd(TriggerParam tgParam, String tmpFilePath, String dataXPyPath) {
        // command process
        //"--loglevel=debug"
        List<String> cmdArr = new ArrayList<>();
        cmdArr.add("python");
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isNotEmpty(dataXHomePath)) {
            dataXPyPath = dataXHomePath.contains("bin") ? dataXHomePath + DEFAULT_DATAX_PY : dataXHomePath + "bin" + File.separator + DEFAULT_DATAX_PY;
        }
        cmdArr.add(dataXPyPath);
        String doc = buildDataXParam(tgParam);
        if (StringUtils.isNotBlank(doc)) {
            cmdArr.add(doc.replaceAll(SPLIT_SPACE, TRANSFORM_SPLIT_SPACE));
        }
        cmdArr.add(tmpFilePath);
        return cmdArr.toArray(new String[cmdArr.size()]);
    }

    private static String buildDataXParam(TriggerParam tgParam) {
        StringBuilder doc = new StringBuilder();
        String jvmParam = StringUtils.isNotBlank(tgParam.getJvmParam()) ? tgParam.getJvmParam().trim() : tgParam.getJvmParam();
        String partitionStr = tgParam.getPartitionInfo();
        if (StringUtils.isNotBlank(jvmParam)) {
            doc.append(JVM_CM).append(TRANSFORM_QUOTES).append(jvmParam).append(TRANSFORM_QUOTES);
        }

        Integer incrementType = tgParam.getIncrementType();
        String replaceParam = StringUtils.isNotBlank(tgParam.getReplaceParam()) ? tgParam.getReplaceParam().trim() : null;

        if (incrementType != null && replaceParam != null) {

            if (IncrementTypeEnum.TIME.getCode() == incrementType) {
                if (doc.length() > 0) doc.append(SPLIT_SPACE);
                String replaceParamType = tgParam.getReplaceParamType();

                replaceParam = buildSplitParams(replaceParam, tgParam.getStartTime());
                // step 默认Integer.MAX,所以triggerTime肯定小于startTime+step;如果要修改就设置这个值，这样step就会生效，一步一步执行
                long step = getStepByParams(replaceParam);
                Date endDate = getEndTime(tgParam.getStartTime(), step, tgParam.getTriggerTime());

                if (StringUtils.isBlank(replaceParamType) || replaceParamType.equals("Timestamp")) {
                    long startTime = tgParam.getStartTime().getTime() / 1000;
                    long endTime = endDate.getTime() / 1000;
                    doc.append(PARAMS_CM).append(TRANSFORM_QUOTES).append(String.format(replaceParam, startTime, endTime));
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(replaceParamType);
                    String endTime = sdf.format(endDate).replaceAll(SPLIT_SPACE, PERCENT);
                    String startTime = sdf.format(tgParam.getStartTime()).replaceAll(SPLIT_SPACE, PERCENT);
                    doc.append(PARAMS_CM).append(TRANSFORM_QUOTES).append(String.format(replaceParam, startTime, endTime));
                }
                //buildPartitionCM(doc, partitionStr);
                doc.append(TRANSFORM_QUOTES);

            } else if (IncrementTypeEnum.ID.getCode() == incrementType) {
                long startId = tgParam.getStartId();
                long endId = tgParam.getEndId();
                if (doc.length() > 0) doc.append(SPLIT_SPACE);
                doc.append(PARAMS_CM).append(TRANSFORM_QUOTES).append(String.format(replaceParam, startId, endId));
                doc.append(TRANSFORM_QUOTES);
            }
        }

        if (incrementType != null && IncrementTypeEnum.PARTITION.getCode() == incrementType) {
            if (StringUtils.isNotBlank(partitionStr)) {
                List<String> partitionInfo = Arrays.asList(partitionStr.split(SPLIT_COMMA));
                if (doc.length() > 0) doc.append(SPLIT_SPACE);
                doc.append(PARAMS_CM).append(TRANSFORM_QUOTES).append(String.format(PARAMS_CM_V_PT, buildPartition(partitionInfo))).append(TRANSFORM_QUOTES);
            }
        }

        JobLogger.log("------------------Command parameters:" + doc);
        return doc.toString();
    }


    private void buildPartitionCM(StringBuilder doc, String partitionStr) {
        if (StringUtils.isNotBlank(partitionStr)) {
            doc.append(SPLIT_SPACE);
            List<String> partitionInfo = Arrays.asList(partitionStr.split(SPLIT_COMMA));
            doc.append(String.format(PARAMS_CM_V_PT, buildPartition(partitionInfo)));
        }
    }

    private static String buildPartition(List<String> partitionInfo) {
        String field = partitionInfo.get(0);
        int timeOffset = Integer.parseInt(partitionInfo.get(1));
        String timeFormat = partitionInfo.get(2);
        String partitionTime = DateUtil.format(DateUtil.addDays(new Date(), timeOffset), timeFormat);
        return field + Constants.EQUAL + partitionTime;
    }

    public static String buildSplitParams(String content, Date date) {
        String[] params = content.split(" ");

        String ret = "";
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            String[] kv = param.split("=");
            if (kv[0].startsWith("-Dsplit")) {
                String formatString = kv[1];
                SimpleDateFormat sf = new SimpleDateFormat(formatString);
                String value = sf.format(date);
                value = value.replaceAll("%", "");
                params[i] = kv[0] + "=" + value;
            }

            ret += params[i] + " ";
        }
        return ret.trim();
    }

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
