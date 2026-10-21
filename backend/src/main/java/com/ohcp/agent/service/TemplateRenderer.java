package com.ohcp.agent.service;

import com.ohcp.agent.model.AdviceDto;
import com.ohcp.agent.model.KnowledgeEntry;
import org.springframework.stereotype.Service;

/**
 * 纯知识库规则渲染器：不依赖大模型，直接根据检索命中的条目生成结构化输出。
 * 保证无 API Key / 断网环境下依然可完整演示。
 */
@Service
public class TemplateRenderer {

    public AdviceDto render(KnowledgeEntry e) {
        AdviceDto dto = new AdviceDto();
        dto.setElement(e.getElement());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setAgronomic(e.getAgronomic());
        dto.setOrganic(e.getOrganic());
        dto.setChemical(e.getChemical());
        dto.setSafety(e.getSafety());
        dto.setScienceCard(e.getScienceCard());
        dto.setEducation(e.getEducation());
        dto.setSimple(buildSimple(e));
        return dto;
    }

    /** 农户简易版：口语化要点，2-3 句 */
    private String buildSimple(KnowledgeEntry e) {
        String desc = firstSentence(e.getDescription());
        String chem = firstSentence(e.getChemical());
        String safety = firstSentence(e.getSafety());
        StringBuilder sb = new StringBuilder();
        sb.append("初步判断是").append(e.getName()).append("。");
        if (desc != null) {
            sb.append(desc).append("。");
        }
        sb.append("建议：").append(chem != null ? chem : e.getChemical()).append("。");
        if (safety != null) {
            sb.append("注意：").append(safety);
        }
        return sb.toString();
    }

    private String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        int idx = text.indexOf('。');
        return idx > 0 ? text.substring(0, idx + 1) : text;
    }
}
