package com.jnet.anno.utils.annotation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mugw
 * @version 1.0
 * @description 支持按 userId 和 slideId 管理的撤销还原管理器
 * @date 2025/5/28 14:29:25
 */
@Slf4j
@Data
@Component
public class UndoRedoManager {

    // 存储结构：key = "userId:slideId"
    private static final Map<String, SerializableUndoRedoStack<UndoRedoEvent>> storage = new ConcurrentHashMap<>();

    /**
     * 获取 key
     */
    private String getKey(Long userId, Long slideId) {
        return userId + ":" + slideId;
    }

    /**
     * 初始化或获取指定用户的撤销栈
     */
    public SerializableUndoRedoStack<UndoRedoEvent> getStack(Long userId, Long slideId) {
        String key = getKey(userId, slideId);
        return storage.computeIfAbsent(key, k -> new SerializableUndoRedoStack<>(null));
    }

    /**
     * 添加事件（更新当前状态）
     */
    public void addEvent(UndoRedoEvent event, int historySize) {
        SerializableUndoRedoStack<UndoRedoEvent> stack = getStack(event.getUserId(),event.getSlideId());
        stack.addLatest(event, historySize);
    }

    /**
     * 撤销操作
     */
    public UndoRedoEvent undo(Long userId, Long slideId) {
        SerializableUndoRedoStack<UndoRedoEvent> stack = getStack(userId, slideId);
        return stack.undoOnce();
    }

    /**
     * 重做操作
     */
    public UndoRedoEvent redo(Long userId, Long slideId) {
        SerializableUndoRedoStack<UndoRedoEvent> stack = getStack(userId, slideId);
        return stack.redoOnce();
    }

    /**
     * 查询是否可以撤销
     */
    public boolean canUndo(Long userId, Long slideId) {
        SerializableUndoRedoStack<UndoRedoEvent> stack = getStack(userId, slideId);
        return stack.canUndo();
    }

    /**
     * 查询是否可以重做
     */
    public boolean canRedo(Long userId, Long slideId) {
        SerializableUndoRedoStack<UndoRedoEvent> stack = getStack(userId, slideId);
        return stack.canRedo();
    }

    /**
     * 清空指定用户的撤销记录
     */
    public void clearForUserAndSlide(Long userId, Long slideId) {
        String key = getKey(userId, slideId);
        storage.remove(key);
    }

    /**
     * 每天凌晨执行一次，清空所有撤销记录
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天 00:00 执行
    public void clearAll() {
        storage.clear();
        log.info("午夜定时任务触发：已清空所有撤销/重做记录");
    }
}

