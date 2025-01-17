package com.springboot.vitalorganize.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class FundStatisticsData {
    String fundName;
    Long fundId;
    Map<String, Object> statistics;
    List<String> charts;
}