/* 农作物缺素症识别与科学施肥建议智能体 - 前端交互 */
(function () {
    'use strict';

    const API = '/api';

    const examples = [
        '水稻，叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。',
        '小麦，下部老叶叶缘焦枯呈灼烧状，茎秆细弱容易倒伏。',
        '玉米，新生叶片小、簇生，节间缩短，叶脉间失绿，出现白苗。',
        '番茄，果实蒂部出现黑色凹陷病斑，新叶卷曲畸形。',
        '柑橘，新叶叶脉间失绿黄化，严重时整叶发白，叶脉保持绿色。'
    ];

    const $ = function (id) { return document.getElementById(id); };

    function initChips() {
        const wrap = $('chips');
        examples.forEach(function (text) {
            const chip = document.createElement('span');
            chip.className = 'chip';
            chip.textContent = text.length > 18 ? text.slice(0, 18) + '…' : text;
            chip.title = text;
            chip.addEventListener('click', function () {
                $('input-text').value = text;
            });
            wrap.appendChild(chip);
        });
    }

    function checkHealth() {
        fetch(API + '/health', { cache: 'no-store' })
            .then(function (r) { return r.json(); })
            .then(function (h) {
                const dot = document.querySelector('.dot');
                dot.classList.add('on');
                $('svc-status').textContent = '服务在线 · 知识库 ' + h.knowledgeEntries + ' 类缺素';
                const badge = $('llm-badge');
                badge.classList.remove('hidden');
                badge.textContent = h.llmEnabled ? '大模型增强已启用' : '纯本地知识库模式';
            })
            .catch(function () {
                $('svc-status').textContent = '服务未连接（请先启动后端）';
            });
    }

    function loadStats(target, bodyId) {
        fetch(API + '/stats', { cache: 'no-store' })
            .then(function (r) { return r.json(); })
            .then(function (s) { renderStats(target, bodyId, s); })
            .catch(function () { /* 静默 */ });
    }

    function renderStats(area, bodyId, s) {
        const body = $(bodyId);
        if (!body) return;
        body.innerHTML = '';
        const list = s.top || [];
        if (!list.length) {
            body.innerHTML = '<div class="empty-tip">暂无查询记录，快去识别一次缺素症吧。</div>';
            return;
        }
        let max = list[0][1];
        list.forEach(function (item) {
            const row = document.createElement('div');
            row.className = 'stat-row';
            const pct = Math.max(4, Math.round(item[1] / max * 100));
            row.innerHTML =
                '<span class="stat-name">缺' + item[0] + '</span>' +
                '<div class="stat-bar"><div style="width:' + pct + '%"></div></div>' +
                '<span class="stat-count">' + item[1] + ' 次</span>';
            body.appendChild(row);
        });
    }

    function renderCandidates(candidates) {
        const wrap = $('candidates');
        wrap.innerHTML = '';
        candidates.forEach(function (c, i) {
            const row = document.createElement('div');
            row.className = 'candidate-row' + (i === 0 ? ' top' : '');
            const pct = Math.round(c.confidence * 100);
            let reasons = '';
            (c.matchReasons || []).forEach(function (r) {
                reasons += '<span>' + r + '</span>';
            });
            row.innerHTML =
                '<div class="candidate-head">' +
                '  <span class="candidate-name"><span class="elem">缺' + c.element + '</span>' + c.name + '</span>' +
                '  <span class="conf">匹配置信度 <b>' + pct + '%</b></span>' +
                '</div>' +
                '<div class="conf-bar"><div style="width:' + pct + '%"></div></div>' +
                '<div class="match-reasons">' + (reasons || '<span>命中知识库症状特征</span>') + '</div>';
            wrap.appendChild(row);
        });
    }

    function renderAdvice(a, mode) {
        const body = $('advice-body');
        body.innerHTML = '';

        if (!a) {
            body.innerHTML = '<div class="empty-tip">未生成建议。</div>';
            return;
        }

        const title = document.createElement('div');
        title.className = 'advice-title';
        title.textContent = a.name + ' · 科学施肥方案';
        body.appendChild(title);

        if (mode === 'farmer') {
            const box = document.createElement('div');
            box.className = 'simple-box';
            box.textContent = a.simple || (a.name + '：请按下方建议操作。');
            body.appendChild(box);
            if (a.description) addBlock(body, '缺素简介', a.description);
            if (a.safety) addBlock(body, '施肥安全与减量提醒', a.safety);
            return;
        }

        if (a.description) addBlock(body, '缺素简介', a.description);
        if (a.agronomic) addBlock(body, '农艺调控手段', a.agronomic);
        if (a.organic) addBlock(body, '有机肥 / 生物肥施用方案', a.organic);
        if (a.chemical) addBlock(body, '化学肥料追施方案', a.chemical);
        if (a.safety) addBlock(body, '施肥安全与化肥减量增效提醒', a.safety);
    }

    function addBlock(parent, title, text) {
        const div = document.createElement('div');
        div.className = 'advice-block';
        div.innerHTML = '<h3>' + title + '</h3><p>' + text + '</p>';
        parent.appendChild(div);
    }

    function setRisk(level) {
        const el = $('risk-badge');
        el.textContent = level === '高' ? '高风险 · 建议结合测土结果补肥' :
                         level === '中' ? '中风险 · 建议尽快核实并追肥' :
                         '低风险 · 关注长势即可';
        el.className = 'risk ' + (level === '高' ? 'high' : level === '中' ? 'mid' : 'low');
    }

    function doAnalyze() {
        const input = $('input-text').value.trim();
        if (!input) {
            $('input-text').focus();
            return;
        }
        const mode = document.querySelector('input[name="mode"]:checked').value;
        const btn = $('submit-btn');
        btn.disabled = true;
        btn.textContent = '识别中…';

        fetch(API + '/analyze', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ input: input, mode: mode })
        })
            .then(function (r) { return r.json(); })
            .then(function (resp) {
                if (!resp.success) {
                    alert(resp.message || '识别失败，请重试。');
                    return;
                }
                $('result-area').classList.remove('hidden');
                renderCandidates(resp.candidates);
                renderAdvice(resp.advice, resp.mode);
                setRisk(resp.riskLevel);
                $('source-badge').textContent = resp.source || '本地知识库';
                const edu = $('education-box');
                if (resp.advice && resp.advice.education) {
                    edu.textContent = resp.advice.education;
                    edu.classList.remove('hidden');
                } else {
                    edu.classList.add('hidden');
                }
                const sc = $('science-card');
                if (resp.advice && resp.advice.scienceCard) {
                    sc.innerHTML = '<b>科普小卡片</b>　' + resp.advice.scienceCard;
                    sc.classList.remove('hidden');
                } else {
                    sc.classList.add('hidden');
                }
                renderStats($('stats-body'), 'stats-body', resp.stats);
                loadStats($('stats-body-standalone'), 'stats-body-standalone');
            })
            .catch(function () {
                alert('请求后端失败，请确认服务已启动（java -jar crop-nutrient-agent.jar）。');
            })
            .finally(function () {
                btn.disabled = false;
                btn.textContent = '识别缺素并生成施肥建议';
            });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initChips();
        checkHealth();
        loadStats($('stats-body-standalone'), 'stats-body-standalone');
        $('submit-btn').addEventListener('click', doAnalyze);
        $('input-text').addEventListener('keydown', function (e) {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                doAnalyze();
            }
        });
    });
})();
