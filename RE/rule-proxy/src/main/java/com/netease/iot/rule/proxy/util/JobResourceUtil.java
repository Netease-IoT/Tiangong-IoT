package com.netease.iot.rule.proxy.util;

import com.google.gson.JsonObject;


public class JobResourceUtil {

    private Long nowSlot;
    private Long nowMem;
    private Long oldSlot;
    private Long oldMem;
    private Long updateSlot;
    private Long updateMem;

    public JobResourceUtil(JsonObject nowJsonObj) {
        try {
            nowSlot = getRealSlot(nowJsonObj.get(Constants.KEY_SINK).getAsLong(), nowJsonObj.get(Constants.KEY_SOURCE).getAsLong(), nowJsonObj.get(Constants.KEY_ENV).getAsLong());
            nowMem = nowSlot * nowJsonObj.get(Constants.KEY_JTM).getAsLong();
        } catch (Exception e) {
            throw new IllegalArgumentException("cof parsing error", e);
        }
    }

    public JobResourceUtil(JsonObject nowJsonObj, JsonObject oldJsonObj) {
        try {
            nowSlot = getRealSlot(nowJsonObj.get(Constants.KEY_SINK).getAsLong(), nowJsonObj.get(Constants.KEY_SOURCE).getAsLong(), nowJsonObj.get(Constants.KEY_ENV).getAsLong());
            nowMem = nowSlot * nowJsonObj.get(Constants.KEY_JTM).getAsLong();
        } catch (Exception e) {
            throw new IllegalArgumentException("cof parsing error", e);
        }

        try {
            oldSlot = getRealSlot(oldJsonObj.get(Constants.KEY_SINK).getAsLong(), oldJsonObj.get(Constants.KEY_SOURCE).getAsLong(), oldJsonObj.get(Constants.KEY_ENV).getAsLong());
        } catch (Exception e) {
            oldSlot = 0L;
        }
        try {
            oldMem = oldSlot * oldJsonObj.get(Constants.KEY_JTM).getAsLong();
        } catch (Exception e) {
            oldMem = 0L;
        }
    }

    public String check(Long totalSlot, Long totalMem, Long usedSlot, Long usedMem) {
        Long leftSlot = totalSlot - usedSlot;
        Long leftMem = totalMem - usedMem;

        if (nowSlot > oldSlot && ((leftSlot - nowSlot) <= 0)) {
            return String.format("The number of available slots is insufficient ; totalSlot = %s usedSlot = %s nowSlot = %s", totalSlot, usedSlot, nowSlot);
        } else if (nowMem > oldMem && ((leftMem - nowMem) < 0)) {
            return String.format("Insufficient available memory ; totalMemory = %s usedMemory = %s nowMemory = %s", totalMem, usedMem, nowMem);
        } else {
            updateSlot = nowSlot - oldSlot;
            updateMem = nowMem - oldMem;
            return null;
        }
    }

    private Long getRealSlot(Long sink, Long source, Long env) {
        Long result = sink > source ? sink : source;
        return result > env ? result : env;
    }

    public Long getUpdateSlot() {
        return updateSlot;
    }

    public Long getUpdateMem() {
        return updateMem;
    }

    public Long getNowSlot() {
        return nowSlot;
    }

    public void setNowSlot(Long nowSlot) {
        this.nowSlot = nowSlot;
    }

    public Long getNowMem() {
        return nowMem;
    }

    public void setNowMem(Long nowMem) {
        this.nowMem = nowMem;
    }
}
