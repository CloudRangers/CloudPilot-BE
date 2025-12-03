package com.cloudrangers.cloudpilot.dto.monitoring;

import java.util.List;
import java.util.Map;

/**
 * Prometheus /api/v1/query 응답을 최소한으로 매핑하는 DTO
 * {
 *   "status": "success",
 *   "data": {
 *     "resultType": "vector",
 *     "result": [
 *       {
 *         "metric": {...},
 *         "value": [ 1719999999.123, "1" ]
 *       }
 *     ]
 *   }
 * }
 */
public class PrometheusQueryResponse {

    private String status;
    private Data data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String resultType;
        private List<Result> result;

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        public List<Result> getResult() {
            return result;
        }

        public void setResult(List<Result> result) {
            this.result = result;
        }
    }

    public static class Result {
        private Map<String, String> metric;
        private List<Object> value;

        public Map<String, String> getMetric() {
            return metric;
        }

        public void setMetric(Map<String, String> metric) {
            this.metric = metric;
        }

        public List<Object> getValue() {
            return value;
        }

        public void setValue(List<Object> value) {
            this.value = value;
        }
    }
}
