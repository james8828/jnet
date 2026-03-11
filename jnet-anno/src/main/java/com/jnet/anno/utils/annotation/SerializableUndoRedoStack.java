package com.jnet.anno.utils.annotation;

import com.jnet.anno.constant.Constant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author mugw
 * @version 1.0
 * @description Serialize objects on request for use in undo/redo.存储方案待优化 todo
 * @date 2025/5/28 14:23:56
 */
@Data
@Slf4j
public class SerializableUndoRedoStack<T> {

    private byte[] current = null;
    private Deque<byte[]> undoStack = new ArrayDeque<>();
    private Deque<byte[]> redoStack = new ArrayDeque<>();

    /**
     * 还原操作下标
     */
    private int redoIndex = -1;
    /**
     * 撤销操作下标
     */
    private int undoIndex = -1;

    SerializableUndoRedoStack(T object) {
        if (object != null)
            current = serialize(object, 1024);
    }

    /**
     * Returns true if the undo stack is not empty.
     *
     * @return
     */
    public boolean canUndo() {
        return undoIndex != -1;
    }

    /**
     * Returns true if the redo stack is not empty.
     *
     * @return
     */
    public boolean canRedo() {
        return redoIndex != -1;
    }

    /**
     * Get the total number of allocated bytes.
     *
     * @return
     */
    public synchronized long totalBytes() {
        long total = 0L;
        if (current != null)
            total = current.length;
        for (byte[] bytes : undoStack)
            total += bytes.length;
        for (byte[] bytes : redoStack)
            total += bytes.length;
        return total;
    }

    /**
     * Clear the undo and redo stacks, in addition to the current bytes.
     */
    public synchronized void clear() {
        current = null;
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * Request redo once, updating the current (serialized) object,
     * and return the deserialized version of the object at the top of the 'redo' stack.
     *
     * @return
     */
    public synchronized T redoOnce() {
        if (!canRedo()) {
            log.debug("Cannot redo! Stack is empty.");
            return null;
        }
        if (current != null) {
            undoStack.push(current);
            undoIndex += 1;
        }
        redoIndex -= 1;
        if (!redoStack.isEmpty()) {
            current = redoStack.pop();
        }
        try {
            return deserialize(current);
        } catch (Exception e) {
            log.error("Failed to deserialize state during undo operation", e);
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    /**
     * 回退到上一个状态，并将当前状态压入重做栈。
     *
     * @return 上一个状态的反序列化对象，若无法回退则返回 null
     */
    public synchronized T undoOnce() {
        if (canUndo()) {
            if (current != null) {
                redoStack.push(current);
                redoIndex += 1;
            }
            undoIndex -= 1;
            byte[] currentState = current;
            if (!undoStack.isEmpty()) {
                current = undoStack.pop();
            }
            try {
                return deserialize(currentState);
            } catch (Exception e) {
                log.error("Failed to deserialize state during undo operation", e);
                throw new RuntimeException("Deserialization failed", e);
            }
        } else {
            log.debug("Cannot undo! Stack is empty.");
            return null;
        }

    }

    /**
     * Record a new change event, updating the current object.
     * This will clear any redo status, on the assumption that redo is no longer possible.
     *
     * @param object
     * @param historySize
     */
    public synchronized void addLatest(final T object, int historySize) {
        addLatest(object, historySize, null);
    }

    public synchronized void addLatest(final T object, int historySize, String operation) {
        int initialSize = 1024;
        if (current != null) {
            // If we are low on memory, clear the undo stack
            long remainingMemory = estimateAvailableMemory();
            if (remainingMemory < current.length * 1.5) {
                undoStack.clear();
            } else {
                // Default to something a bit bigger than the last things we had
                initialSize = Math.min(Integer.MAX_VALUE - 64, (int) (current.length * 1.1));
                undoStack.push(current);
            }
        }
        undoIndex += 1;
        current = serialize(object, initialSize);
        // Reset the ability to redo
        if (operation == null && !Constant.ANNO_ACTION_UPDATE.equals(operation)) {
            redoStack.clear();
            redoIndex = -1;
        }
        // Check the history size
        if (historySize > 0) {
            while (undoStack.size() > historySize)
                undoStack.pollLast();
        }
    }

    /**
     * Serialize an object to an array of bytes.
     * Providing an estimate of the initial array size can help performance.
     *
     * @param object
     * @param initialSize
     * @return
     */
    private byte[] serialize(T object, int initialSize) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(initialSize)) {
            ObjectOutputStream out = new ObjectOutputStream(stream);
            out.writeObject(object);
            out.flush();
            return stream.toByteArray();
        } catch (IOException e) {
            log.error("Error serializing " + object, e);
            return null;
        }
    }

    /**
     * Deserialize an object from byte array with custom class loader and security checks.
     *
     * @param bytes serialized data
     * @return deserialized object or null if failed
     */
    @SuppressWarnings("unchecked")
    private T deserialize(byte[] bytes) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(stream) {
                 @Override
                 protected Class<?> resolveClass(ObjectStreamClass desc)
                         throws IOException, ClassNotFoundException {
                     // 使用当前类的类加载器加载类
                     return Class.forName(desc.getName(), false, UndoRedoEvent.class.getClassLoader());
                 }
             }) {
            return (T) in.readObject();
        } catch (ClassNotFoundException e) {
            log.error("Class not found during deserialization: {}", e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error during deserialization: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during deserialization: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Estimate the current available memory in bytes, based upon the JVM max and the memory currently used.
     * <p>
     * This may be used to help determine whether a memory-hungry operation should be attempted.
     *
     * @return the estimated unused memory in bytes
     */
    public static long estimateAvailableMemory() {
        System.gc();
        return Runtime.getRuntime().maxMemory() - estimateUsedMemory();
    }

    /**
     * Estimate the current used memory.
     *
     * @return the estimated allocated memory in bytes
     */
    public static long estimateUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

}
