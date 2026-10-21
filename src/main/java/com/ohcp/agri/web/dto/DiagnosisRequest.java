package com.ohcp.agri.web.dto;

/**
 * 前端提交的缺素症状描述。字段与需求文档「用户输入示例」对齐：
 * 水稻，叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。
 *
 * @param crop        作物类型，如「水稻」「小麦」
 * @param part        症状部位，如「下部老叶」「叶缘」
 * @param symptom     症状表现，如「均匀发黄」「焦枯」
 * @param environment 发生环境（可选），如「沙质土」「低温多雨」
 */
public record DiagnosisRequest(String crop, String part, String symptom, String environment) {

    /** 拼成一句自然语言，供检索与提示词使用。 */
    public String toQuery() {
        StringBuilder sb = new StringBuilder();
        if (crop != null && !crop.isBlank()) {
            sb.append(crop);
        }
        if (part != null && !part.isBlank()) {
            sb.append(sb.isEmpty() ? "" : "，").append(part);
        }
        if (symptom != null && !symptom.isBlank()) {
            sb.append(sb.isEmpty() ? "" : "，").append(symptom);
        }
        if (environment != null && !environment.isBlank()) {
            sb.append(sb.isEmpty() ? "" : "，").append(environment);
        }
        return sb.toString();
    }
}
