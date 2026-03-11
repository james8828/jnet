package com.jnet.image.tile.statistics;

import lombok.Data;

/**
 * 图像数据类
 */
@Data
public class ImageInfo {
    private String name;
    private int width,height;
    private int tileSize; // 瓦片大小，默认512

    public ImageInfo(int width, int height) {
        this(width, height, 512);
    }

    public ImageInfo(String name,int width, int height) {
        this.width = width;
        this.height = height;
        this.tileSize = 512;
        this.name = name;
    }

    public ImageInfo(int width, int height, int tileSize) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
    }

    @Override
    public String toString() {
        return String.format("%dx%d", width, height);
    }
}

