package com.jnet.biz.algorithm.dataset;

import lombok.Data;

import java.util.List;

/**
 * 数据集划分结果
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class DatasetSplit {
    private List<ImageAnnotationData> trainSet;
    private List<ImageAnnotationData> valSet;
    private List<ImageAnnotationData> testSet;
}
