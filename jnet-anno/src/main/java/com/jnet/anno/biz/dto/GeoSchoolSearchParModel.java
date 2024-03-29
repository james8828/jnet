package com.jnet.anno.biz.dto;

import com.jnet.anno.biz.entity.GeoSchool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoSchoolSearchParModel {
    private Integer size;
    private Integer page;
    private GeoSchool params;

}